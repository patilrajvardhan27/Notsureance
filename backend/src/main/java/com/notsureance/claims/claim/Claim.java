package com.notsureance.claims.claim;

import com.notsureance.claims.common.BaseEntity;
import com.notsureance.claims.common.BusinessRuleException;
import com.notsureance.claims.policy.Equipment;
import com.notsureance.claims.policy.Policy;
import com.notsureance.claims.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "claims")
public class Claim extends BaseEntity {

    @Column(name = "claim_number", nullable = false, unique = true, updatable = false)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filed_by")
    private AppUser filedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_adjuster")
    private AppUser assignedAdjuster;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LossCause cause;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(nullable = false)
    private String description;

    @Column(name = "estimated_loss", nullable = false)
    private BigDecimal estimatedLoss;

    @Column(name = "approved_amount")
    private BigDecimal approvedAmount;

    @Column(name = "incident_latitude")
    private BigDecimal incidentLatitude;

    @Column(name = "incident_longitude")
    private BigDecimal incidentLongitude;

    @Column(name = "harvest_critical", nullable = false)
    private boolean harvestCritical;

    @Embedded
    private ClaimScore score = new ClaimScore();

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "confirmed_fraud", nullable = false)
    private boolean confirmedFraud;

    @Version
    private long version;

    protected Claim() {
    }

    public Claim(String claimNumber, Policy policy, Equipment equipment, AppUser filedBy, LossCause cause,
                 LocalDate incidentDate, Instant reportedAt, String description, BigDecimal estimatedLoss) {
        this.claimNumber = claimNumber;
        this.policy = policy;
        this.equipment = equipment;
        this.filedBy = filedBy;
        this.cause = cause;
        this.incidentDate = incidentDate;
        this.reportedAt = reportedAt;
        this.description = description;
        this.estimatedLoss = estimatedLoss;
    }

    public void locateAt(BigDecimal latitude, BigDecimal longitude, boolean harvestCritical) {
        this.incidentLatitude = latitude;
        this.incidentLongitude = longitude;
        this.harvestCritical = harvestCritical;
    }

    public ClaimStatus moveTo(ClaimStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("A " + status + " claim cannot move to " + target);
        }
        ClaimStatus previous = status;
        status = target;
        return previous;
    }

    public void assignTo(AppUser adjuster) {
        this.assignedAdjuster = adjuster;
    }

    public void recordDecision(String reason, BigDecimal amount, boolean fraud) {
        this.decisionReason = reason;
        this.approvedAmount = amount;
        this.confirmedFraud = fraud;
    }

    public void applyScore(ClaimScore newScore) {
        this.score = newScore;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public Policy getPolicy() {
        return policy;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public AppUser getFiledBy() {
        return filedBy;
    }

    public AppUser getAssignedAdjuster() {
        return assignedAdjuster;
    }

    public LossCause getCause() {
        return cause;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getEstimatedLoss() {
        return estimatedLoss;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public BigDecimal getIncidentLatitude() {
        return incidentLatitude;
    }

    public BigDecimal getIncidentLongitude() {
        return incidentLongitude;
    }

    public boolean isHarvestCritical() {
        return harvestCritical;
    }

    public ClaimScore getScore() {
        return score;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public boolean isConfirmedFraud() {
        return confirmedFraud;
    }
}
