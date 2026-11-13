package com.notsureance.claims.agent.eval;

import com.notsureance.claims.agent.AgentRunStatus;
import com.notsureance.claims.agent.ProposedAction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AgentEvalScorer {

    private AgentEvalScorer() {
    }

    public record CaseResult(String caseId, ProposedAction expected, ProposedAction proposed, boolean supported,
                             int toolCalls, AgentRunStatus status) {

        boolean matched() {
            return expected == proposed;
        }
    }

    public record Report(int cases, int actionMatches, double actionMatchRate, int citedAnswers,
                         double citationRate, double averageToolCalls, Map<ProposedAction, Integer> matchesByAction,
                         List<CaseResult> results) {
    }

    public static Report score(List<CaseResult> results) {
        int matches = (int) results.stream().filter(CaseResult::matched).count();
        int cited = (int) results.stream().filter(CaseResult::supported).count();
        double calls = results.stream().mapToInt(CaseResult::toolCalls).average().orElse(0);
        Map<ProposedAction, Integer> byAction = new EnumMap<>(ProposedAction.class);
        for (ProposedAction action : ProposedAction.values()) {
            byAction.put(action, (int) results.stream().filter(r -> r.expected() == action && r.matched()).count());
        }
        int total = results.size();
        return new Report(total, matches, ratio(matches, total), cited, ratio(cited, total),
                Math.round(calls * 100) / 100.0, byAction, List.copyOf(results));
    }

    private static double ratio(int part, int total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 1000.0;
    }
}
