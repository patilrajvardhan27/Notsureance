package com.notsureance.claims.agent;

import com.notsureance.claims.claim.ClaimRequests.Decision;
import com.notsureance.claims.claim.ClaimRequests.DecisionAction;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.claim.ClaimStatus;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.claim.ClaimWorkflowService;
import com.notsureance.claims.common.BusinessRuleException;
import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentProposalService {

    private final AgentProposalRepository proposals;
    private final ClaimService claims;
    private final ClaimWorkflowService workflow;
    private final Clock clock;

    public AgentProposalService(AgentProposalRepository proposals, ClaimService claims, ClaimWorkflowService workflow,
                                Clock clock) {
        this.proposals = proposals;
        this.claims = claims;
        this.workflow = workflow;
        this.clock = clock;
    }

    @Transactional
    public Long propose(AgentToolContext agent, long claimId, ProposedAction action, String reason) {
        if (agent.claimId() != claimId) {
            throw new BusinessRuleException("Proposals can only be made for the claim under review");
        }
        if (action == null || reason == null || reason.isBlank()) {
            throw new BusinessRuleException("A proposal needs an action and a reason");
        }
        if (proposals.existsByRunId(agent.runId())) {
            throw new BusinessRuleException("This run already made its proposal");
        }
        return proposals.save(new AgentProposal(agent.runId(), claimId, action, reason.trim())).getId();
    }

    @Transactional(readOnly = true)
    public List<AgentProposal> pending() {
        return proposals.findByStatusOrderByCreatedAtAsc(AgentProposal.Status.PENDING);
    }

    // Approving replays the proposal through the normal workflow, so every status rule still applies.
    @Transactional
    public ClaimDetail approve(Long proposalId, String note, AuthenticatedUser adjuster) {
        AgentProposal proposal = load(proposalId);
        proposal.decide(true, adjuster.id(), note, clock.instant());
        String why = "Agent proposal approved: " + proposal.getReason();
        Long claimId = proposal.getClaimId();
        ClaimDetail claim = claims.detail(claimId, adjuster);
        return switch (proposal.getAction()) {
            case FAST_TRACK -> workflow.decide(claimId, new Decision(DecisionAction.APPROVE, payout(claim), why, false),
                    adjuster);
            case REQUEST_INFO -> {
                startReviewIfNeeded(claim, adjuster);
                yield workflow.decide(claimId, new Decision(DecisionAction.REQUEST_INFO, null, why, false), adjuster);
            }
            case INVESTIGATE -> claim.summary().status() == ClaimStatus.SUBMITTED
                    ? workflow.decide(claimId, new Decision(DecisionAction.START_REVIEW, null,
                    "Sent to investigation. " + why, false), adjuster)
                    : claim;
        };
    }

    @Transactional
    public void reject(Long proposalId, String note, AuthenticatedUser adjuster) {
        load(proposalId).decide(false, adjuster.id(), note, clock.instant());
    }

    private void startReviewIfNeeded(ClaimDetail claim, AuthenticatedUser adjuster) {
        if (claim.summary().status() == ClaimStatus.SUBMITTED) {
            workflow.decide(claim.summary().id(), new Decision(DecisionAction.START_REVIEW, null, null, false), adjuster);
        }
    }

    private static BigDecimal payout(ClaimDetail claim) {
        BigDecimal estimate = BigDecimal.valueOf(claim.assessment().predictedPayout()).setScale(2, RoundingMode.HALF_UP);
        return estimate.min(claim.summary().estimatedLoss());
    }

    private AgentProposal load(Long proposalId) {
        return proposals.findById(proposalId).orElseThrow(() -> new NotFoundException("Proposal", proposalId));
    }
}
