package com.notsureance.claims.agent;

public class AgentUnavailableException extends RuntimeException {

    public AgentUnavailableException() {
        super("The claims agent is not configured on this server. Set AGENT_MODEL to claude or azure-openai.");
    }
}
