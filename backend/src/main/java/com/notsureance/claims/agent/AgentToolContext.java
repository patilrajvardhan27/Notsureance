package com.notsureance.claims.agent;

import com.notsureance.claims.security.AuthenticatedUser;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.model.ToolContext;

/** Carries the signed-in adjuster into every tool, so tools see exactly what that person can see. */
public final class AgentToolContext {

    private static final String KEY = "notsureance.agent";

    private final AuthenticatedUser user;
    private final Long claimId;
    private final Long runId;
    private final Set<String> citable = ConcurrentHashMap.newKeySet();

    public AgentToolContext(AuthenticatedUser user, Long claimId, Long runId) {
        this.user = user;
        this.claimId = claimId;
        this.runId = runId;
    }

    public static AgentToolContext from(ToolContext context) {
        Object value = context == null ? null : context.getContext().get(KEY);
        if (value instanceof AgentToolContext agent) {
            return agent;
        }
        throw new IllegalStateException("Agent tools can only run inside an agent run");
    }

    ToolContext asToolContext() {
        return new ToolContext(Map.of(KEY, this));
    }

    public void allowCitation(String id) {
        if (id != null && !id.isBlank()) {
            citable.add(id);
        }
    }

    public void allowCitations(Collection<String> ids) {
        ids.forEach(this::allowCitation);
    }

    Set<String> citable() {
        return Set.copyOf(citable);
    }

    public AuthenticatedUser user() {
        return user;
    }

    public Long claimId() {
        return claimId;
    }

    public Long runId() {
        return runId;
    }
}
