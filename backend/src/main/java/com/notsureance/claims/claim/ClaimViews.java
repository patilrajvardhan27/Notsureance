package com.notsureance.claims.claim;

import com.notsureance.claims.ml.scoring.ClaimAssessment;
import com.notsureance.claims.policy.PolicyViews.EquipmentView;
import com.notsureance.claims.policy.PolicyViews.PolicySummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class ClaimViews {

    private ClaimViews() {
    }

    public record ClaimSummary(Long id, String claimNumber, String farmName, String equipmentName, LossCause cause,
                               ClaimStatus status, TriageLane lane, BigDecimal fraudScore, BigDecimal estimatedLoss,
                               BigDecimal predictedPayout, int priority, boolean harvestCritical,
                               LocalDate incidentDate, Instant filedAt) {

        public static ClaimSummary from(Claim c) {
            ClaimScore s = c.getScore();
            return new ClaimSummary(c.getId(), c.getClaimNumber(), c.getPolicy().getFarmName(),
                    c.getEquipment().displayName(), c.getCause(), c.getStatus(), s.getLane(), s.getFraudScore(),
                    c.getEstimatedLoss(), s.getPredictedPayout(), s.getPriority(), c.isHarvestCritical(),
                    c.getIncidentDate(), c.getCreatedAt());
        }
    }

    public record TimelineEntry(ClaimEventType type, ClaimStatus fromStatus, ClaimStatus toStatus, String note,
                                String actorName, Instant at) {

        public static TimelineEntry from(ClaimEvent e) {
            return new TimelineEntry(e.getType(), e.getFromStatus(), e.getToStatus(), e.getNote(),
                    e.getActor() == null ? "System" : e.getActor().getFullName(), e.getCreatedAt());
        }
    }

    public record ClaimDetail(ClaimSummary summary, PolicySummary policy, EquipmentView equipment, String description,
                              Instant reportedAt, BigDecimal approvedAmount, BigDecimal latitude, BigDecimal longitude,
                              String filedBy, String assignedAdjuster, String decisionReason, String repairDealer,
                              ClaimAssessment assessment, List<TimelineEntry> timeline, Set<ClaimStatus> nextStatuses) {
    }
}
