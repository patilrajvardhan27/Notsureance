package com.notsureance.claims.ml.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class RidgeRegressionTest {

    @Test
    void recoversALinearRelationship() {
        double[][] x = new double[50][2];
        double[] y = new double[50];
        for (int i = 0; i < 50; i++) {
            x[i] = new double[] {i - 25, (i % 7) - 3};
            y[i] = 3 + 2 * x[i][0] - 0.5 * x[i][1];
        }
        RidgeRegression model = RidgeRegression.fit(x, y, 1e-6);

        assertThat(model.predict(new double[] {10, 1})).isCloseTo(22.5, within(0.01));
    }

    @Test
    void solvesASmallSystem() {
        double[] solution = RidgeRegression.solve(new double[][] {{2, 1}, {1, 3}}, new double[] {5, 10});
        assertThat(solution[0]).isCloseTo(1.0, within(1e-9));
        assertThat(solution[1]).isCloseTo(3.0, within(1e-9));
    }
}
