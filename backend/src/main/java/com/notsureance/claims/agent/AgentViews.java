package com.notsureance.claims.agent;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class AgentViews {

    private AgentViews() {
    }

    public record RunRequest(@Size(max = 500) String question) {
    }

    public record DecisionNote(@Size(max = 1000) String note) {
    }

    public record TraceView(int step, String tool, String input, String output, boolean failed, long durationMs) {

        static TraceView from(AgentTrace t) {
            return new TraceView(t.getStep(), t.getToolName(), t.getInput(), t.getOutput(), t.isFailed(), t.getDurationMs());
        }
    }

    public record ProposalView(Long id, Long claimId, String claimNumber, String farmName, ProposedAction action,
                               String reason, AgentProposal.Status status, String decisionNote, Instant createdAt) {

        static ProposalView from(AgentProposal p, String claimNumber, String farmName) {
            return new ProposalView(p.getId(), p.getClaimId(), claimNumber, farmName, p.getAction(), p.getReason(),
                    p.getStatus(), p.getDecisionNote(), p.getCreatedAt());
        }
    }

    public record RunView(Long id, AgentRunStatus status, String question, String answer, boolean supported,
                          List<String> citations, int toolCalls, String model, Instant startedAt,
                          List<TraceView> trace, ProposalView proposal) {

        static RunView from(AgentRun run, List<AgentTrace> traces, ProposalView proposal) {
            List<String> cited = run.getCitations() == null || run.getCitations().isBlank()
                    ? List.of() : Arrays.asList(run.getCitations().split(","));
            return new RunView(run.getId(), run.getStatus(), run.getQuestion(), run.getAnswer(), run.isSupported(),
                    cited, run.getToolCalls(), run.getModel(), run.getCreatedAt(),
                    traces.stream().map(TraceView::from).toList(), proposal);
        }
    }
}
