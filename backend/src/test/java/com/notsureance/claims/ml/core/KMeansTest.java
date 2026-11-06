package com.notsureance.claims.ml.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KMeansTest {

    @Test
    void findsWellSeparatedGroups() {
        double[][] points = {{0, 0}, {0.2, 0.1}, {0.1, 0.3}, {10, 10}, {10.2, 9.9}, {9.8, 10.1}};
        KMeans.Result result = KMeans.fit(points, 2, 50, 3L);

        int[] a = result.assignments();
        assertThat(a[0]).isEqualTo(a[1]).isEqualTo(a[2]);
        assertThat(a[3]).isEqualTo(a[4]).isEqualTo(a[5]);
        assertThat(a[0]).isNotEqualTo(a[3]);
        assertThat(result.inertia()).isLessThan(1.0);
    }
}
