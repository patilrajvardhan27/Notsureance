package com.notsureance.claims.agent.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent.claude")
public record ClaudeProperties(String model, long maxTokens, String effort, boolean refusalFallback) {

    public ClaudeProperties {
        model = model == null || model.isBlank() ? "claude-opus-5-5" : model;
        maxTokens = maxTokens > 0 ? maxTokens : 16_000;
        effort = effort == null || effort.isBlank() ? "medium" : effort;
    }
}
