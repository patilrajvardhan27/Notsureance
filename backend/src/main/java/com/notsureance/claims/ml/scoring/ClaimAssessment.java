package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.claim.TriageLane;
import java.util.List;

public record ClaimAssessment(
        double fraudProbability,
        double predictedPayout,
        double anomalyScore,
        LossCause predictedCause,
        double causeConfidence,
        TriageLane lane,
        int priority,
        List<RiskFactor> topFactors) {

    public record RiskFactor(String label, double impact) {
    }
}
