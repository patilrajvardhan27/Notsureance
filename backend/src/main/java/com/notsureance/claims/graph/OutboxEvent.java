package com.notsureance.claims.graph;

import com.notsureance.claims.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity {

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
    }

    OutboxEvent(String aggregateType, Long aggregateId, String eventType) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
    }

    void markProcessed(Instant at) {
        this.processedAt = at;
        this.lastError = null;
    }

    void markFailed(String error) {
        this.attempts++;
        this.lastError = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 500));
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }
}
