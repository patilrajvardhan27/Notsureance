package com.notsureance.claims.agent;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent")
public record AgentProperties(int maxToolCalls, Duration timeout, String modelLabel) {

    public AgentProperties {
        maxToolCalls = maxToolCalls > 0 ? maxToolCalls : 6;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        modelLabel = modelLabel == null || modelLabel.isBlank() ? "not configured" : modelLabel;
    }
}
