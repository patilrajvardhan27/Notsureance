package com.notsureance.claims.agent.model;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "claude")
public class ClaudeModelConfig {

    // Reads ANTHROPIC_API_KEY from the environment, which App Service fills from Key Vault.
    @Bean(destroyMethod = "close")
    AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }

    @Bean
    ClaudeChatModel claudeChatModel(AnthropicClient client, ClaudeProperties properties, ObjectMapper json) {
        return new ClaudeChatModel(client, properties, json);
    }
}
