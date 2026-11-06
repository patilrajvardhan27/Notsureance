package com.notsureance.claims.claim;

import com.notsureance.claims.ml.scoring.ClaimAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public class ClaimScore {

    @Column(name = "fraud_score")
    private BigDecimal fraudScore;

    @Column(name = "predicted_payout")
    private BigDecimal predictedPayout;

    @Column(name = "anomaly_score")
    private BigDecimal anomalyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "predicted_cause")
    private LossCause predictedCause;

    @Enumerated(EnumType.STRING)
    @Column(name = "triage_lane")
    private TriageLane lane;

    @Column(name = "priority_score", nullable = false)
    private int priority;

    protected ClaimScore() {
    }

    static ClaimScore from(ClaimAssessment assessment) {
        ClaimScore score = new ClaimScore();
        score.fraudScore = BigDecimal.valueOf(assessment.fraudProbability()).setScale(4, RoundingMode.HALF_UP);
        score.predictedPayout = BigDecimal.valueOf(assessment.predictedPayout()).setScale(2, RoundingMode.HALF_UP);
        score.anomalyScore = BigDecimal.valueOf(Math.min(assessment.anomalyScore(), 999)).setScale(3, RoundingMode.HALF_UP);
        score.predictedCause = assessment.predictedCause();
        score.lane = assessment.lane();
        score.priority = assessment.priority();
        return score;
    }

    public BigDecimal getFraudScore() {
        return fraudScore;
    }

    public BigDecimal getPredictedPayout() {
        return predictedPayout;
    }

    public BigDecimal getAnomalyScore() {
        return anomalyScore;
    }

    public LossCause getPredictedCause() {
        return predictedCause;
    }

    public TriageLane getLane() {
        return lane;
    }

    public int getPriority() {
        return priority;
    }
}
