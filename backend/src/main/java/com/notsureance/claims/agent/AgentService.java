package com.notsureance.claims.agent;

import com.notsureance.claims.agent.AgentViews.ProposalView;
import com.notsureance.claims.agent.AgentViews.RunView;
import com.notsureance.claims.claim.Claim;
import com.notsureance.claims.claim.ClaimEvent;
import com.notsureance.claims.claim.ClaimEventRepository;
import com.notsureance.claims.claim.ClaimEventType;
import com.notsureance.claims.claim.ClaimRepository;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.claim.ClaimViews.ClaimSummary;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.UserRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ObjectProvider<ChatModel> models;
    private final AgentRunner runner;
    private final AgentRunRepository runs;
    private final AgentTraceRepository traces;
    private final AgentProposalRepository proposals;
    private final ClaimService claimService;
    private final ClaimRepository claims;
    private final ClaimEventRepository events;
    private final UserRepository users;
    private final AgentProperties properties;
    private final Clock clock;

    public AgentService(ObjectProvider<ChatModel> models, AgentRunner runner, AgentRunRepository runs,
                        AgentTraceRepository traces, AgentProposalRepository proposals, ClaimService claimService,
                        ClaimRepository claims, ClaimEventRepository events, UserRepository users,
                        AgentProperties properties, Clock clock) {
        this.models = models;
        this.runner = runner;
        this.runs = runs;
        this.traces = traces;
        this.proposals = proposals;
        this.claimService = claimService;
        this.claims = claims;
        this.events = events;
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    // No transaction around the run: model calls can take seconds and must not hold a database connection.
    public RunView run(Long claimId, String question, AuthenticatedUser user) {
        ChatModel model = models.getIfAvailable();
        if (model == null) {
            throw new AgentUnavailableException();
        }
        return runWith(model, claimId, question, user);
    }

    public RunView runWith(ChatModel model, Long claimId, String question, AuthenticatedUser user) {
        ClaimSummary claim = claimService.detail(claimId, user).summary();
        String asked = question == null || question.isBlank() ? "What should I do with this claim?" : question.trim();
        AgentRun run = runs.save(new AgentRun(claimId, user.id(), asked, properties.modelLabel()));
        AgentRunner.Outcome outcome;
        try {
            outcome = runner.run(model, new AgentToolContext(user, claimId, run.getId()), asked);
        } catch (RuntimeException ex) {
            log.warn("Agent run {} failed: {}", run.getId(), ex.getMessage());
            outcome = new AgentRunner.Outcome(AgentRunStatus.FAILED, null,
                    traces.findByRunIdOrderByStepAsc(run.getId()).size(), Citations.check(null, Set.of()));
        }
        run.finish(outcome.status(), outcome.answer(), outcome.citations().supported(),
                String.join(",", outcome.citations().cited()), outcome.toolCalls(), clock.instant());
        runs.save(run);
        events.save(new ClaimEvent(claims.getReferenceById(claimId), users.getReferenceById(user.id()),
                ClaimEventType.AGENT_RECOMMENDED, null, null, summary(outcome)));
        return view(run, claim.claimNumber(), claim.farmName());
    }

    @Transactional(readOnly = true)
    public List<RunView> recentRuns(Long claimId, AuthenticatedUser user) {
        Claim claim = claimService.visibleClaim(claimId, user);
        return runs.findTop5ByClaimIdOrderByCreatedAtDesc(claimId).stream()
                .map(run -> view(run, claim.getClaimNumber(), claim.getPolicy().getFarmName())).toList();
    }

    @Transactional(readOnly = true)
    public List<ProposalView> pendingProposals() {
        List<AgentProposal> pending = proposals.findByStatusOrderByCreatedAtAsc(AgentProposal.Status.PENDING);
        Map<Long, Claim> byId = claims.findAllById(pending.stream().map(AgentProposal::getClaimId).toList()).stream()
                .collect(Collectors.toMap(Claim::getId, Function.identity()));
        return pending.stream().map(p -> {
            Claim claim = byId.get(p.getClaimId());
            return ProposalView.from(p, claim.getClaimNumber(), claim.getPolicy().getFarmName());
        }).toList();
    }

    private RunView view(AgentRun run, String claimNumber, String farmName) {
        ProposalView proposal = proposals.findFirstByRunId(run.getId())
                .map(p -> ProposalView.from(p, claimNumber, farmName)).orElse(null);
        return RunView.from(run, traces.findByRunIdOrderByStepAsc(run.getId()), proposal);
    }

    private static String summary(AgentRunner.Outcome outcome) {
        return switch (outcome.status()) {
            case COMPLETED -> "Agent finished after " + outcome.toolCalls() + " tool call(s)"
                    + (outcome.citations().supported() ? "" : "; answer marked unsupported");
            case STEP_LIMIT -> "Agent stopped at the tool-call limit";
            case TIMED_OUT -> "Agent ran out of time";
            default -> "Agent run failed";
        };
    }
}
