package com.notsureance.claims.ml.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class ClassificationMetricsTest {

    @Test
    void computesConfusionMatrixAndAuc() {
        double[] scores = {0.9, 0.8, 0.4, 0.3, 0.6, 0.1};
        int[] labels = {1, 1, 1, 0, 0, 0};
        ClassificationMetrics m = ClassificationMetrics.evaluate(scores, labels, 0.5);

        assertThat(m.truePositives()).isEqualTo(2);
        assertThat(m.falsePositives()).isEqualTo(1);
        assertThat(m.falseNegatives()).isEqualTo(1);
        assertThat(m.precision()).isCloseTo(2 / 3.0, within(1e-9));
        assertThat(m.rocAuc()).isCloseTo(8 / 9.0, within(1e-9));
    }

    @Test
    void perfectRankingGivesAucOfOne() {
        assertThat(ClassificationMetrics.rocAuc(new double[] {0.1, 0.2, 0.8, 0.9}, new int[] {0, 0, 1, 1}))
                .isEqualTo(1.0);
    }
}
