package com.notsureance.claims.ml.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class LogisticRegressionTest {

    @Test
    void separatesTwoGaussianClusters() {
        Random random = new Random(1);
        double[][] x = new double[400][2];
        int[] y = new int[400];
        for (int i = 0; i < 400; i++) {
            y[i] = i % 2;
            double shift = y[i] == 1 ? 1.5 : -1.5;
            x[i] = new double[] {shift + random.nextGaussian(), shift + random.nextGaussian()};
        }
        LogisticRegression model = LogisticRegression.fit(x, y, LogisticRegression.Options.defaults());

        double[] scores = new double[400];
        for (int i = 0; i < 400; i++) {
            scores[i] = model.probability(x[i]);
        }
        ClassificationMetrics metrics = ClassificationMetrics.evaluate(scores, y, 0.5);
        assertThat(metrics.rocAuc()).isGreaterThan(0.95);
        assertThat(metrics.accuracy()).isGreaterThan(0.9);
        assertThat(model.weights()[0]).isPositive();
    }

    @Test
    void contributionsSumToTheLinearScore() {
        LogisticRegression model = LogisticRegression.fit(new double[][] {{0}, {1}, {2}, {3}}, new int[] {0, 0, 1, 1},
                LogisticRegression.Options.defaults());
        double[] parts = model.contributions(new double[] {2});
        assertThat(parts[0]).isEqualTo(model.weights()[0] * 2);
    }
}
