package com.notsureance.claims.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.support.ScriptedChatModel;
import com.notsureance.claims.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

class AgentRunnerTest {

    private final AgentToolContext context =
            new AgentToolContext(new AuthenticatedUser(1L, "a@x.dev", "A", Role.ADJUSTER), 8L, 1L);

    @Test
    void stopsWhenTheModelOverrunsItsTimeBudget() {
        AgentRunner runner = new AgentRunner(List.of(), mock(AgentTraceRepository.class),
                new AgentProperties(6, Duration.ofMillis(200), "test"), Clock.systemUTC());
        ChatModel slow = prompt -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return new ChatResponse(List.of(new Generation(ScriptedChatModel.answer("late"))));
        };
        assertThat(runner.run(slow, context, "?").status()).isEqualTo(AgentRunStatus.TIMED_OUT);
    }

    @Test
    void anUnknownToolIsReportedBackInsteadOfCrashing() {
        AgentRunner runner = new AgentRunner(List.of(), mock(AgentTraceRepository.class),
                new AgentProperties(6, Duration.ofSeconds(5), "test"), Clock.systemUTC());
        ScriptedChatModel model = new ScriptedChatModel();
        model.script(ScriptedChatModel.tool("dropTables", "{}"), ScriptedChatModel.answer("Nothing to cite."));

        AgentRunner.Outcome outcome = runner.run(model, context, "?");

        assertThat(outcome.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(outcome.toolCalls()).isEqualTo(1);
        assertThat(model.prompts().get(1).getInstructions().toString()).contains("Unknown tool dropTables");
    }
}
