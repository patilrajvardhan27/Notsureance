package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.claim.TriageLane;
import com.notsureance.claims.ml.core.NaiveBayesTextClassifier.Prediction;
import com.notsureance.claims.ml.scoring.ClaimAssessment.RiskFactor;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class ClaimScoringService {

    private static final int TOP_FACTORS = 3;
    private static final double MIN_FACTOR_IMPACT = 0.15;

    private final ModelRegistry registry;
    private final ModelTrainingService trainer;
    private final TriagePolicy triagePolicy;

    public ClaimScoringService(ModelRegistry registry, ModelTrainingService trainer, TriagePolicy triagePolicy) {
        this.registry = registry;
        this.trainer = trainer;
        this.triagePolicy = triagePolicy;
    }

    public ClaimAssessment assess(ClaimSignals signals) {
        TrainedModels models = models();
        Prediction text = models.causeModel().predict(signals.description());

        double[] fraudX = models.fraudScaler().transform(
                ClaimFeatures.fraudFeatures(signals, text.probabilityOf(signals.cause().name())));
        double fraudProbability = models.fraudModel().probability(fraudX);
        List<RiskFactor> factors = topFactors(models.fraudModel().contributions(fraudX));

        double ratio = models.payoutModel().predict(models.payoutScaler().transform(ClaimFeatures.payoutFeatures(signals)));
        double coveredLoss = Math.min(signals.estimatedLoss(), signals.insuredValue());
        double payout = clamp(clamp(ratio, 0.05, 1.0) * coveredLoss - signals.deductible(), 0, signals.coverageLimit());

        double anomaly = models.anomalyBaseline().zScore(signals);
        TriageLane lane = triagePolicy.laneFor(fraudProbability, anomaly, signals);
        int priority = triagePolicy.priorityFor(fraudProbability, signals);
        return new ClaimAssessment(round(fraudProbability, 4), Math.round(payout * 100) / 100.0, round(anomaly, 3),
                LossCause.valueOf(text.label()), round(text.confidence(), 3), lane, priority, factors);
    }

    public ModelReport currentReport() {
        return models().report();
    }

    private TrainedModels models() {
        return registry.current().orElseGet(() -> {
            trainer.retrain();
            return registry.current().orElseThrow();
        });
    }

    private static List<RiskFactor> topFactors(double[] contributions) {
        return IntStream.range(0, contributions.length)
                .filter(i -> contributions[i] > MIN_FACTOR_IMPACT)
                .boxed()
                .sorted((a, b) -> Double.compare(contributions[b], contributions[a]))
                .limit(TOP_FACTORS)
                .map(i -> new RiskFactor(ClaimFeatures.FRAUD_FEATURE_LABELS.get(i), round(contributions[i], 3)))
                .toList();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
