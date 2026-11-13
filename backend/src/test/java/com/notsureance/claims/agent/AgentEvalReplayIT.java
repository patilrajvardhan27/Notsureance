package com.notsureance.claims.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.notsureance.claims.agent.eval.AgentEvalScorer;
import com.notsureance.claims.agent.eval.AgentEvalScorer.CaseResult;
import com.notsureance.claims.agent.eval.EvalCase;
import com.notsureance.claims.agent.eval.EvalCaseFiler;
import com.notsureance.claims.agent.eval.RecordedTurn;
import com.notsureance.claims.graph.GraphSyncJob;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.support.IntegrationTest;
import com.notsureance.claims.support.ScriptedChatModel;
import com.notsureance.claims.user.Role;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;

/** Replays a recorded live evaluation. Skipped until someone runs the agent-eval profile with a real model key. */
class AgentEvalReplayIT extends IntegrationTest {

    @Autowired
    EvalCaseFiler filer;

    @Autowired
    AgentService agent;

    @Autowired
    ScriptedChatModel model;

    @Autowired
    GraphSyncJob graph;

    @Test
    void recordedEvaluationReplaysToTheSameScores() throws Exception {
        InputStream recordingFile = getClass().getResourceAsStream("/agent-eval/recording.json");
        InputStream reportFile = getClass().getResourceAsStream("/agent-eval/report.json");
        assumeTrue(recordingFile != null && reportFile != null, "No recorded evaluation committed yet");
        Map<String, List<RecordedTurn>> recording = json.readValue(recordingFile, new TypeReference<>() {
        });
        AgentEvalScorer.Report expected = json.readValue(reportFile, AgentEvalScorer.Report.class);
        graph.drain();

        var adjuster = new AuthenticatedUser(3L, ADJUSTER, "Priya Raman", Role.ADJUSTER);
        List<CaseResult> results = new ArrayList<>();
        for (EvalCase evalCase : filer.cases()) {
            model.script(recording.get(evalCase.id()).stream().map(RecordedTurn::toMessage)
                    .toArray(AssistantMessage[]::new));
            var run = agent.run(filer.file(evalCase), null, adjuster);
            results.add(new CaseResult(evalCase.id(), evalCase.expected(),
                    run.proposal() == null ? null : run.proposal().action(), run.supported(), run.toolCalls(), run.status()));
        }
        AgentEvalScorer.Report replayed = AgentEvalScorer.score(results);
        assertThat(replayed.actionMatches()).isEqualTo(expected.actionMatches());
        assertThat(replayed.citedAnswers()).isEqualTo(expected.citedAnswers());
        assertThat(replayed.averageToolCalls()).isEqualTo(expected.averageToolCalls());
    }

    @Test
    void theEvaluationSetHoldsThirtyLabelledCases() {
        List<EvalCase> cases = filer.cases();
        assertThat(cases).hasSize(30);
        assertThat(cases).extracting(EvalCase::expected).containsOnly(ProposedAction.values());
        cases.forEach(evalCase -> assertThat(filer.file(evalCase)).isPositive());
    }
}
