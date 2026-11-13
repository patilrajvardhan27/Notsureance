package com.notsureance.claims.agent;

import com.notsureance.claims.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "agent_runs")
public class AgentRun extends BaseEntity {

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(nullable = false)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRunStatus status = AgentRunStatus.RUNNING;

    private String answer;

    @Column(nullable = false)
    private boolean supported;

    private String citations;

    @Column(name = "tool_calls", nullable = false)
    private int toolCalls;

    @Column(nullable = false)
    private String model;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AgentRun() {
    }

    AgentRun(Long claimId, Long requestedBy, String question, String model) {
        this.claimId = claimId;
        this.requestedBy = requestedBy;
        this.question = question;
        this.model = model;
    }

    void finish(AgentRunStatus outcome, String finalAnswer, boolean cited, String citedIds, int calls, Instant at) {
        this.status = outcome;
        this.answer = finalAnswer;
        this.supported = cited;
        this.citations = citedIds;
        this.toolCalls = calls;
        this.finishedAt = at;
    }

    public Long getClaimId() {
        return claimId;
    }

    public String getQuestion() {
        return question;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isSupported() {
        return supported;
    }

    public String getCitations() {
        return citations;
    }

    public int getToolCalls() {
        return toolCalls;
    }

    public String getModel() {
        return model;
    }
}
