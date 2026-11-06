package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.TriageLane;
import org.springframework.stereotype.Component;

@Component
public class TriagePolicy {

    private final TriageSettings settings;

    public TriagePolicy(TriageSettings settings) {
        this.settings = settings;
    }

    public TriageLane laneFor(double fraudProbability, double anomalyScore, ClaimSignals signals) {
        if (fraudProbability >= settings.investigationMinFraud() || anomalyScore >= settings.anomalyThreshold() + 1) {
            return TriageLane.SPECIAL_INVESTIGATION;
        }
        boolean smallAndClean = signals.estimatedLoss() <= settings.fastTrackMaxLoss()
                && fraudProbability < settings.fastTrackMaxFraud()
                && anomalyScore < settings.anomalyThreshold()
                && signals.priorClaims() <= 1;
        return smallAndClean ? TriageLane.FAST_TRACK : TriageLane.STANDARD;
    }

    public int priorityFor(double fraudProbability, ClaimSignals signals) {
        double lossWeight = Math.min(1.0, Math.log10(Math.max(signals.estimatedLoss(), 10)) / 6.0);
        double score = 35 * lossWeight
                + (signals.harvestCritical() ? 25 : 0)
                + (signals.inPlantingOrHarvestSeason() ? 15 : 0)
                + 25 * fraudProbability;
        return (int) Math.round(Math.min(100, score));
    }
}
