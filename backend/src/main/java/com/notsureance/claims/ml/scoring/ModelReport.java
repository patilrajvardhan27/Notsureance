package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.ml.core.ClassificationMetrics;
import com.notsureance.claims.ml.core.RegressionMetrics;
import java.time.Instant;
import java.util.List;

public record ModelReport(
        Instant trainedAt,
        int syntheticSamples,
        int feedbackSamples,
        ClassificationMetrics fraudHoldout,
        double fraudCrossValidatedAuc,
        RegressionMetrics payoutHoldout,
        double causeTextAccuracy,
        List<FeatureWeight> fraudFeatureWeights) {

    public record FeatureWeight(String feature, double weight) {
    }
}
