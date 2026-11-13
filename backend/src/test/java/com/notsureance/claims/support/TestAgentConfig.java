package com.notsureance.claims.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestAgentConfig {

    @Bean
    ScriptedChatModel scriptedChatModel() {
        return new ScriptedChatModel();
    }
}
