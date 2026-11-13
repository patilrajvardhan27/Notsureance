package com.notsureance.claims.graph;

import com.notsureance.claims.claim.Claim;
import com.notsureance.claims.claim.ClaimRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class GraphSyncJob {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncJob.class);

    private final OutboxRepository outbox;
    private final OutboxPublisher publisher;
    private final ClaimRepository claims;
    private final ClaimGraphWriter writer;
    private final TransactionTemplate transactions;
    private final GraphProperties properties;
    private final Clock clock;

    public GraphSyncJob(OutboxRepository outbox, OutboxPublisher publisher, ClaimRepository claims,
                        ClaimGraphWriter writer, TransactionTemplate transactions, GraphProperties properties,
                        Clock clock) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.claims = claims;
        this.writer = writer;
        this.transactions = transactions;
        this.properties = properties;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prepareGraph() {
        if (!properties.enabled()) {
            return;
        }
        try {
            writer.ensureSchema();
            if (writer.claimCount() < claims.count()) {
                transactions.executeWithoutResult(status -> claims.findAll()
                        .forEach(claim -> publisher.claimChanged(claim.getId(), "BACKFILL")));
            }
            drain();
        } catch (RuntimeException ex) {
            log.warn("Graph store is not reachable yet, claims will sync once it is: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.graph.sync-delay-ms:5000}", initialDelayString = "${app.graph.sync-delay-ms:5000}")
    public void syncPending() {
        if (properties.enabled()) {
            drain();
        }
    }

    public int drain() {
        int synced = 0;
        int batch;
        do {
            Integer done = transactions.execute(status -> syncBatch());
            batch = done == null ? 0 : done;
            synced += batch;
        } while (batch == properties.batchSize());
        return synced;
    }

    private int syncBatch() {
        List<OutboxEvent> events = outbox.lockPending(properties.batchSize(), properties.maxAttempts());
        int synced = 0;
        for (OutboxEvent event : events) {
            try {
                Claim claim = claims.findById(event.getAggregateId()).orElse(null);
                if (claim != null) {
                    writer.upsert(ClaimGraphRecord.from(claim));
                }
                event.markProcessed(clock.instant());
                synced++;
            } catch (RuntimeException ex) {
                event.markFailed(ex.getMessage());
            }
        }
        return synced;
    }
}
