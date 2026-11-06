package com.notsureance.claims.claim;

import com.notsureance.claims.claim.ClaimRequests.Decision;
import com.notsureance.claims.claim.ClaimRequests.InfoResponse;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.common.BusinessRuleException;
import com.notsureance.claims.ml.scoring.ClaimScoringService;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClaimWorkflowService {

    private final ClaimService claimService;
    private final ClaimEventRepository events;
    private final UserRepository users;
    private final ClaimScoringService scoring;
    private final ClaimSignalsFactory signalsFactory;

    public ClaimWorkflowService(ClaimService claimService, ClaimEventRepository events, UserRepository users,
                                ClaimScoringService scoring, ClaimSignalsFactory signalsFactory) {
        this.claimService = claimService;
        this.events = events;
        this.users = users;
        this.scoring = scoring;
        this.signalsFactory = signalsFactory;
    }

    public ClaimDetail decide(Long claimId, Decision decision, AuthenticatedUser user) {
        Claim claim = claimService.visibleClaim(claimId, user);
        AppUser adjuster = users.getReferenceById(user.id());
        if (claim.getAssignedAdjuster() == null) {
            claim.assignTo(adjuster);
            events.save(new ClaimEvent(claim, adjuster, ClaimEventType.ASSIGNED, null, null,
                    "Assigned to " + user.fullName()));
        }
        String note = decision.note() == null ? null : decision.note().trim();
        ClaimStatus target = switch (decision.action()) {
            case START_REVIEW -> ClaimStatus.UNDER_REVIEW;
            case APPROVE -> approve(claim, decision.amount(), note);
            case DENY -> {
                claim.recordDecision(requireNote(note), null, decision.suspectedFraud());
                yield ClaimStatus.DENIED;
            }
            case REQUEST_INFO -> {
                requireNote(note);
                yield ClaimStatus.NEEDS_INFO;
            }
            case MARK_PAID -> ClaimStatus.PAID;
        };
        ClaimStatus previous = claim.moveTo(target);
        events.save(new ClaimEvent(claim, adjuster, ClaimEventType.STATUS_CHANGED, previous, target, note));
        return claimService.toDetail(claim, scoring.assess(signalsFactory.from(claim)), user);
    }

    public ClaimDetail respondWithInfo(Long claimId, InfoResponse response, AuthenticatedUser user) {
        Claim claim = claimService.visibleClaim(claimId, user);
        if (claim.getStatus() != ClaimStatus.NEEDS_INFO) {
            throw new BusinessRuleException("This claim is not waiting on more information");
        }
        AppUser agent = users.getReferenceById(user.id());
        ClaimStatus previous = claim.moveTo(ClaimStatus.UNDER_REVIEW);
        events.save(new ClaimEvent(claim, agent, ClaimEventType.INFO_PROVIDED, previous, ClaimStatus.UNDER_REVIEW,
                response.note().trim()));
        return claimService.toDetail(claim, scoring.assess(signalsFactory.from(claim)), user);
    }

    private static ClaimStatus approve(Claim claim, BigDecimal amount, String note) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("An approved amount greater than zero is required");
        }
        if (amount.compareTo(claim.getPolicy().getCoverageLimit()) > 0) {
            throw new BusinessRuleException("The approved amount exceeds the policy coverage limit");
        }
        if (amount.compareTo(claim.getEstimatedLoss()) > 0) {
            throw new BusinessRuleException("The approved amount cannot exceed the estimated loss");
        }
        claim.recordDecision(note, amount, false);
        return ClaimStatus.APPROVED;
    }

    private static String requireNote(String note) {
        if (note == null || note.isBlank()) {
            throw new BusinessRuleException("Please add a note explaining this decision");
        }
        return note;
    }
}
