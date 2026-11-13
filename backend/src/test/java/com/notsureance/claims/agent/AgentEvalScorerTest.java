package com.notsureance.claims.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.notsureance.claims.agent.eval.AgentEvalScorer;
import com.notsureance.claims.agent.eval.AgentEvalScorer.CaseResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentEvalScorerTest {

    @Test
    void scoresActionMatchesCitationsAndToolCalls() {
        var report = AgentEvalScorer.score(List.of(
                new CaseResult("a", ProposedAction.FAST_TRACK, ProposedAction.FAST_TRACK, true, 4, AgentRunStatus.COMPLETED),
                new CaseResult("b", ProposedAction.INVESTIGATE, ProposedAction.REQUEST_INFO, true, 6, AgentRunStatus.COMPLETED),
                new CaseResult("c", ProposedAction.REQUEST_INFO, null, false, 6, AgentRunStatus.STEP_LIMIT)));

        assertThat(report.actionMatches()).isEqualTo(1);
        assertThat(report.actionMatchRate()).isEqualTo(0.333);
        assertThat(report.citedAnswers()).isEqualTo(2);
        assertThat(report.averageToolCalls()).isEqualTo(5.33);
        assertThat(report.matchesByAction().get(ProposedAction.FAST_TRACK)).isEqualTo(1);
    }
}
