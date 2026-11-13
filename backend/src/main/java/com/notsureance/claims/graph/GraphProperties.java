package com.notsureance.claims.graph;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.graph")
public record GraphProperties(boolean enabled, int batchSize, int maxAttempts) {

    public GraphProperties {
        batchSize = batchSize > 0 ? batchSize : 50;
        maxAttempts = maxAttempts > 0 ? maxAttempts : 5;
    }
}
