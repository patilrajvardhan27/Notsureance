package com.notsureance.claims.ml.insight;

import com.notsureance.claims.ml.insight.RiskSegmentationService.PolicyRef;
import com.notsureance.claims.ml.scoring.ModelsTrainedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Stored the latest k-means segment per policy, so the Power BI report could slice fraud rate by it. */
@Component
public class PolicySegmentWriter {

    private final RiskSegmentationService segmentation;
    private final JdbcTemplate jdbc;

    public PolicySegmentWriter(RiskSegmentationService segmentation, JdbcTemplate jdbc) {
        this.segmentation = segmentation;
        this.jdbc = jdbc;
    }

    @EventListener(ModelsTrainedEvent.class)
    @Transactional
    public void storeSegments() {
        segmentation.segmentAll().forEach(segment -> segment.policies().stream().map(PolicyRef::id)
                .forEach(policyId -> jdbc.update("""
                        INSERT INTO policy_segments (policy_id, segment, computed_at) VALUES (?, ?, now())
                        ON CONFLICT (policy_id) DO UPDATE SET segment = EXCLUDED.segment, computed_at = now()
                        """, policyId, segment.name())));
    }
}
