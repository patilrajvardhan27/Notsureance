package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.ml.core.NaiveBayesTextClassifier;
import java.util.List;

public final class ClaimFeatures {

    public static final List<String> FRAUD_FEATURE_LABELS = List.of(
            "Loss is high relative to equipment value",
            "Claim filed soon after the policy started",
            "Incident was reported late",
            "Older equipment",
            "Prior claims in the last 12 months",
            "Theft of equipment without GPS tracking",
            "Description does not match the stated cause",
            "Incident happened on a weekend",
            "Very short description",
            "Cause is commonly linked to staged losses");

    private ClaimFeatures() {
    }

    public static double[] fraudFeatures(ClaimSignals s, double statedCauseTextProbability) {
        int words = NaiveBayesTextClassifier.tokenize(s.description()).size();
        return new double[] {
                Math.min(s.lossToValue(), 3.0),
                Math.exp(-s.policyTenureDays() / 90.0),
                Math.log1p(Math.max(0, s.reportingDelayDays())),
                s.equipmentAgeYears() / 10.0,
                s.priorClaims(),
                s.cause() == LossCause.THEFT && !s.gpsTracked() ? 1 : 0,
                1.0 - statedCauseTextProbability,
                s.weekendIncident() ? 1 : 0,
                words < 8 ? 1 : 0,
                s.cause().isHighRisk() ? 1 : 0
        };
    }

    public static double[] payoutFeatures(ClaimSignals s) {
        LossCause[] causes = LossCause.values();
        double[] features = new double[4 + causes.length];
        features[0] = Math.log(Math.max(s.insuredValue(), 1.0));
        features[1] = Math.min(s.lossToValue(), 3.0);
        features[2] = s.equipmentAgeYears();
        features[3] = Math.log1p(s.engineHours() / 1000.0);
        features[4 + s.cause().ordinal()] = 1;
        return features;
    }
}
