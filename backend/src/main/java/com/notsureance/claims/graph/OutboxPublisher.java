package com.notsureance.claims.graph;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    public static final String CLAIM = "CLAIM";

    private final OutboxRepository outbox;

    public OutboxPublisher(OutboxRepository outbox) {
        this.outbox = outbox;
    }

    // Written in the caller's transaction, so a claim change and its sync event commit or roll back together.
    @Transactional(propagation = Propagation.MANDATORY)
    public void claimChanged(Long claimId, String eventType) {
        outbox.save(new OutboxEvent(CLAIM, claimId, eventType));
    }
}
