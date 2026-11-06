package com.notsureance.claims.ml.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.triage")
public record TriageSettings(double fastTrackMaxLoss, double fastTrackMaxFraud, double investigationMinFraud,
                             double anomalyThreshold) {

    public TriageSettings {
        fastTrackMaxLoss = fastTrackMaxLoss > 0 ? fastTrackMaxLoss : 25_000;
        fastTrackMaxFraud = fastTrackMaxFraud > 0 ? fastTrackMaxFraud : 0.2;
        investigationMinFraud = investigationMinFraud > 0 ? investigationMinFraud : 0.4;
        anomalyThreshold = anomalyThreshold > 0 ? anomalyThreshold : 2.5;
    }
}
