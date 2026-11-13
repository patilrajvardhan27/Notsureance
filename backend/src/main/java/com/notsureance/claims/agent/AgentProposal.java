package com.notsureance.claims.agent;

import com.notsureance.claims.common.BaseEntity;
import com.notsureance.claims.common.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "agent_proposals")
public class AgentProposal extends BaseEntity {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposedAction action;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decision_note")
    private String decisionNote;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected AgentProposal() {
    }

    AgentProposal(Long runId, Long claimId, ProposedAction action, String reason) {
        this.runId = runId;
        this.claimId = claimId;
        this.action = action;
        this.reason = reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }

    void decide(boolean approved, Long adjusterId, String note, Instant at) {
        if (status != Status.PENDING) {
            throw new BusinessRuleException("This proposal was already " + status.name().toLowerCase());
        }
        this.status = approved ? Status.APPROVED : Status.REJECTED;
        this.decidedBy = adjusterId;
        this.decisionNote = note;
        this.decidedAt = at;
    }

    public Long getRunId() {
        return runId;
    }

    public Long getClaimId() {
        return claimId;
    }

    public ProposedAction getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public Status getStatus() {
        return status;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
