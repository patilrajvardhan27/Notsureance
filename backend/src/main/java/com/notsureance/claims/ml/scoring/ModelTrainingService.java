package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.ml.core.ClassificationMetrics;
import com.notsureance.claims.ml.core.LogisticRegression;
import com.notsureance.claims.ml.core.NaiveBayesTextClassifier;
import com.notsureance.claims.ml.core.RegressionMetrics;
import com.notsureance.claims.ml.core.RidgeRegression;
import com.notsureance.claims.ml.core.Shuffle;
import com.notsureance.claims.ml.core.StandardScaler;
import com.notsureance.claims.ml.scoring.ModelReport.FeatureWeight;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ModelTrainingService {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainingService.class);
    private static final long SEED = 42L;
    private static final double RIDGE_LAMBDA = 1.0;
    private static final int FOLDS = 5;

    private final ModelRegistry registry;
    private final TrainingFeedbackSource feedbackSource;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final int syntheticSamples;
    private final double threshold;

    public ModelTrainingService(ModelRegistry registry, TrainingFeedbackSource feedbackSource,
                                ApplicationEventPublisher events, Clock clock, TriageSettings triage,
                                @Value("${app.ml.synthetic-samples:3000}") int syntheticSamples) {
        this.registry = registry;
        this.feedbackSource = feedbackSource;
        this.events = events;
        this.clock = clock;
        this.syntheticSamples = syntheticSamples;
        this.threshold = triage.investigationMinFraud();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void trainOnStartup() {
        retrain();
    }

    @Scheduled(cron = "${app.ml.retrain-cron:0 0 3 * * *}")
    public void retrainNightly() {
        retrain();
    }

    public synchronized ModelReport retrain() {
        List<LabeledClaim> feedback = feedbackSource.decidedClaims();
        List<LabeledClaim> all = new ArrayList<>(new SyntheticClaimFactory(SEED, LocalDate.now(clock))
                .generate(syntheticSamples));
        all.addAll(feedback);
        int[] order = Shuffle.indices(all.size(), SEED);
        int cut = (int) (all.size() * 0.8);
        List<LabeledClaim> train = IntStream.range(0, cut).mapToObj(i -> all.get(order[i])).toList();
        List<LabeledClaim> test = IntStream.range(cut, all.size()).mapToObj(i -> all.get(order[i])).toList();

        NaiveBayesTextClassifier causeModel = fitCauseModel(train);
        double textAccuracy = test.stream()
                .filter(c -> causeModel.predict(c.signals().description()).label().equals(c.textCause().name()))
                .count() / (double) test.size();

        double[][] trainX = fraudMatrix(train, causeModel);
        int[] trainY = labels(train);
        StandardScaler fraudScaler = StandardScaler.fit(trainX);
        LogisticRegression fraudModel = LogisticRegression.fit(fraudScaler.transformAll(trainX), trainY,
                LogisticRegression.Options.defaults());
        double[][] testX = fraudScaler.transformAll(fraudMatrix(test, causeModel));
        double[] testScores = IntStream.range(0, testX.length).mapToDouble(i -> fraudModel.probability(testX[i])).toArray();
        ClassificationMetrics fraudMetrics = ClassificationMetrics.evaluate(testScores, labels(test), threshold);

        List<LabeledClaim> honestTrain = train.stream().filter(c -> !c.fraud()).toList();
        List<LabeledClaim> honestTest = test.stream().filter(c -> !c.fraud()).toList();
        double[][] payoutX = payoutMatrix(honestTrain);
        StandardScaler payoutScaler = StandardScaler.fit(payoutX);
        RidgeRegression payoutModel = RidgeRegression.fit(payoutScaler.transformAll(payoutX), ratios(honestTrain), RIDGE_LAMBDA);
        double[][] payoutTestX = payoutScaler.transformAll(payoutMatrix(honestTest));
        double[] payoutPredictions = IntStream.range(0, payoutTestX.length)
                .mapToDouble(i -> payoutModel.predict(payoutTestX[i])).toArray();
        RegressionMetrics payoutMetrics = RegressionMetrics.evaluate(payoutPredictions, ratios(honestTest));

        AnomalyBaseline baseline = AnomalyBaseline.fit(honestTrain.stream().map(LabeledClaim::signals).toList());
        ModelReport report = new ModelReport(clock.instant(), syntheticSamples, feedback.size(), fraudMetrics,
                crossValidatedAuc(trainX, trainY), payoutMetrics, textAccuracy, featureWeights(fraudModel));
        registry.publish(new TrainedModels(fraudScaler, fraudModel, payoutScaler, payoutModel, causeModel, baseline, report));
        log.info("Claim models trained: fraud AUC {}, payout R2 {}, text accuracy {}",
                round(fraudMetrics.rocAuc()), round(payoutMetrics.rSquared()), round(textAccuracy));
        events.publishEvent(new ModelsTrainedEvent(report));
        return report;
    }

    private double crossValidatedAuc(double[][] x, int[] y) {
        double total = 0;
        for (int fold = 0; fold < FOLDS; fold++) {
            final int current = fold;
            int[] trainIdx = IntStream.range(0, x.length).filter(i -> i % FOLDS != current).toArray();
            int[] testIdx = IntStream.range(0, x.length).filter(i -> i % FOLDS == current).toArray();
            double[][] foldTrain = IntStream.of(trainIdx).mapToObj(i -> x[i]).toArray(double[][]::new);
            StandardScaler scaler = StandardScaler.fit(foldTrain);
            LogisticRegression model = LogisticRegression.fit(scaler.transformAll(foldTrain),
                    IntStream.of(trainIdx).map(i -> y[i]).toArray(), LogisticRegression.Options.defaults());
            double[] scores = IntStream.of(testIdx).mapToDouble(i -> model.probability(scaler.transform(x[i]))).toArray();
            total += ClassificationMetrics.evaluate(scores, IntStream.of(testIdx).map(i -> y[i]).toArray(), threshold).rocAuc();
        }
        return total / FOLDS;
    }

    private static NaiveBayesTextClassifier fitCauseModel(List<LabeledClaim> claims) {
        return NaiveBayesTextClassifier.fit(claims.stream().map(c -> c.signals().description()).toList(),
                claims.stream().map(c -> c.textCause().name()).toList(), 1.0);
    }

    private static double[][] fraudMatrix(List<LabeledClaim> claims, NaiveBayesTextClassifier causeModel) {
        return claims.stream().map(c -> ClaimFeatures.fraudFeatures(c.signals(),
                causeModel.predict(c.signals().description()).probabilityOf(c.signals().cause().name())))
                .toArray(double[][]::new);
    }

    private static double[][] payoutMatrix(List<LabeledClaim> claims) {
        return claims.stream().map(c -> ClaimFeatures.payoutFeatures(c.signals())).toArray(double[][]::new);
    }

    private static int[] labels(List<LabeledClaim> claims) {
        return claims.stream().mapToInt(c -> c.fraud() ? 1 : 0).toArray();
    }

    private static double[] ratios(List<LabeledClaim> claims) {
        return claims.stream().mapToDouble(LabeledClaim::payoutRatio).toArray();
    }

    private static List<FeatureWeight> featureWeights(LogisticRegression model) {
        double[] weights = model.weights();
        return IntStream.range(0, weights.length)
                .mapToObj(i -> new FeatureWeight(ClaimFeatures.FRAUD_FEATURE_LABELS.get(i), round(weights[i])))
                .sorted((a, b) -> Double.compare(Math.abs(b.weight()), Math.abs(a.weight())))
                .toList();
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
