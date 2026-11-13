package com.notsureance.claims.graph;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "SELECT * FROM outbox_events WHERE processed_at IS NULL AND attempts < :maxAttempts "
            + "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> lockPending(int limit, int maxAttempts);

    long countByProcessedAtIsNull();
}
