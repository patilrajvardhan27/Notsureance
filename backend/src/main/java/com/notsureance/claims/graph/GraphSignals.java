package com.notsureance.claims.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Graph input for the fraud model. Scoring must never fail because the graph is down, so it falls back to zero. */
@Component
public class GraphSignals {

    private static final Logger log = LoggerFactory.getLogger(GraphSignals.class);

    private final FraudGraphRepository graph;
    private final GraphProperties properties;

    public GraphSignals(FraudGraphRepository graph, GraphProperties properties) {
        this.graph = graph;
        this.properties = properties;
    }

    public int confirmedFraudNear(String policyNumber) {
        if (!properties.enabled()) {
            return 0;
        }
        try {
            return (int) GraphViews.flaggedWithinTwoHops(graph.linkedParties(policyNumber));
        } catch (RuntimeException ex) {
            log.debug("Graph lookup skipped for {}: {}", policyNumber, ex.getMessage());
            return 0;
        }
    }
}
