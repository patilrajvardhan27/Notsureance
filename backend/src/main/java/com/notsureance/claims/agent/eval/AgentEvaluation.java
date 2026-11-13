package com.notsureance.claims.agent.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.notsureance.claims.agent.AgentService;
import com.notsureance.claims.agent.AgentUnavailableException;
import com.notsureance.claims.agent.AgentViews.RunView;
import com.notsureance.claims.agent.eval.AgentEvalScorer.CaseResult;
import com.notsureance.claims.agent.eval.AgentEvalScorer.Report;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.Role;
import com.notsureance.claims.user.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runs the 30 evaluation cases against the configured live model and writes the scores plus every model reply,
 * so CI can replay the same run later without a key. Start with SPRING_PROFILES_ACTIVE=dev,agent-eval.
 */
@Component
@Profile("agent-eval")
public class AgentEvaluation implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentEvaluation.class);

    private final EvalCaseFiler filer;
    private final AgentService agent;
    private final ObjectProvider<ChatModel> models;
    private final UserRepository users;
    private final ObjectMapper json;
    private final Path output;

    public AgentEvaluation(EvalCaseFiler filer, AgentService agent, ObjectProvider<ChatModel> models,
                           UserRepository users, ObjectMapper json,
                           @Value("${app.agent.eval.output:agent-eval}") String output) {
        this.filer = filer;
        this.agent = agent;
        this.models = models;
        this.users = users;
        this.json = json.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.output = Path.of(output);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ChatModel live = models.getIfAvailable();
        if (live == null) {
            throw new AgentUnavailableException();
        }
        AuthenticatedUser adjuster = principal(Role.ADJUSTER);
        Map<String, List<RecordedTurn>> recording = new LinkedHashMap<>();
        List<CaseResult> results = new ArrayList<>();
        for (EvalCase evalCase : filer.cases()) {
            List<RecordedTurn> turns = new ArrayList<>();
            ChatModel recorder = prompt -> {
                var response = live.call(prompt);
                AssistantMessage reply = response.getResult().getOutput();
                turns.add(RecordedTurn.of(reply));
                return response;
            };
            Long claimId = filer.file(evalCase);
            RunView run = agent.runWith(recorder, claimId, null, adjuster);
            recording.put(evalCase.id(), turns);
            results.add(new CaseResult(evalCase.id(), evalCase.expected(),
                    run.proposal() == null ? null : run.proposal().action(), run.supported(), run.toolCalls(),
                    run.status()));
            log.info("Eval {}: expected {}, proposed {}", evalCase.id(), evalCase.expected(),
                    run.proposal() == null ? "nothing" : run.proposal().action());
        }
        Report report = AgentEvalScorer.score(results);
        Files.createDirectories(output);
        json.writeValue(output.resolve("report.json").toFile(), report);
        json.writeValue(output.resolve("recording.json").toFile(), recording);
        log.info("Agent eval: {}/{} actions matched, {} cited, {} tool calls on average. Written to {}",
                report.actionMatches(), report.cases(), report.citedAnswers(), report.averageToolCalls(),
                output.toAbsolutePath());
    }

    private AuthenticatedUser principal(Role role) {
        AppUser user = users.findByRoleAndActiveTrue(role).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No demo user with role " + role));
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), role);
    }
}
