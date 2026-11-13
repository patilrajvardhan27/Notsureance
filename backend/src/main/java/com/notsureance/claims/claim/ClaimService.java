package com.notsureance.claims.claim;

import com.notsureance.claims.claim.ClaimRequests.FileClaim;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.claim.ClaimViews.ClaimSummary;
import com.notsureance.claims.claim.ClaimViews.TimelineEntry;
import com.notsureance.claims.common.BusinessRuleException;
import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.common.PageView;
import com.notsureance.claims.graph.OutboxPublisher;
import com.notsureance.claims.ml.scoring.ClaimAssessment;
import com.notsureance.claims.ml.scoring.ClaimScoringService;
import com.notsureance.claims.policy.Equipment;
import com.notsureance.claims.policy.Policy;
import com.notsureance.claims.policy.PolicyService;
import com.notsureance.claims.policy.PolicyViews.EquipmentView;
import com.notsureance.claims.policy.PolicyViews.PolicySummary;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.Role;
import com.notsureance.claims.user.UserRepository;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClaimService {

    private final ClaimRepository claims;
    private final ClaimEventRepository events;
    private final PolicyService policyService;
    private final UserRepository users;
    private final ClaimScoringService scoring;
    private final ClaimSignalsFactory signalsFactory;
    private final ClaimNumberGenerator numbers;
    private final OutboxPublisher outbox;
    private final Clock clock;

    public ClaimService(ClaimRepository claims, ClaimEventRepository events, PolicyService policyService,
                        UserRepository users, ClaimScoringService scoring, ClaimSignalsFactory signalsFactory,
                        ClaimNumberGenerator numbers, OutboxPublisher outbox, Clock clock) {
        this.claims = claims;
        this.events = events;
        this.policyService = policyService;
        this.users = users;
        this.scoring = scoring;
        this.signalsFactory = signalsFactory;
        this.numbers = numbers;
        this.outbox = outbox;
        this.clock = clock;
    }

    public ClaimDetail file(FileClaim request, AuthenticatedUser user) {
        Policy policy = policyService.accessiblePolicy(request.policyId(), user);
        if (!policy.isActiveOn(request.incidentDate())) {
            throw new BusinessRuleException("The incident date falls outside the policy period");
        }
        Equipment equipment = policyService.equipmentOnPolicy(policy.getId(), request.equipmentId());
        AppUser agent = users.getReferenceById(user.id());
        Claim claim = new Claim(numbers.next(), policy, equipment, agent, request.cause(), request.incidentDate(),
                clock.instant(), request.description().trim(), request.estimatedLoss());
        claim.locateAt(request.latitude(), request.longitude(), request.harvestCritical(), request.repairDealer());

        ClaimAssessment assessment = scoring.assess(signalsFactory.from(claim));
        claim.applyScore(ClaimScore.from(assessment));
        claims.save(claim);
        events.save(new ClaimEvent(claim, agent, ClaimEventType.FILED, null, ClaimStatus.SUBMITTED, null));
        events.save(new ClaimEvent(claim, null, ClaimEventType.SCORED, null, null, describe(assessment)));
        outbox.claimChanged(claim.getId(), "FILED");
        return toDetail(claim, assessment, user);
    }

    @Transactional(readOnly = true)
    public PageView<ClaimSummary> search(ClaimStatus status, TriageLane lane, String text, boolean openOnly,
                                         Pageable pageable, AuthenticatedUser user) {
        Specification<Claim> spec = Specification.allOf(ClaimSpecifications.visibleTo(user),
                ClaimSpecifications.hasStatus(status), ClaimSpecifications.inLane(lane),
                ClaimSpecifications.openOnly(openOnly), ClaimSpecifications.matches(text));
        return PageView.of(claims.findAll(spec, pageable), ClaimSummary::from);
    }

    @Transactional(readOnly = true)
    public ClaimDetail detail(Long claimId, AuthenticatedUser user) {
        Claim claim = visibleClaim(claimId, user);
        return toDetail(claim, scoring.assess(signalsFactory.from(claim)), user);
    }

    @Transactional(readOnly = true)
    public Claim visibleClaim(Long claimId, AuthenticatedUser user) {
        Claim claim = claims.findById(claimId).orElseThrow(() -> new NotFoundException("Claim", claimId));
        if (!user.seesAllClaims() && !claim.getFiledBy().getId().equals(user.id())) {
            throw new NotFoundException("Claim", claimId);
        }
        return claim;
    }

    ClaimDetail toDetail(Claim claim, ClaimAssessment assessment, AuthenticatedUser user) {
        var timeline = events.findByClaimIdOrderByCreatedAtAscIdAsc(claim.getId()).stream()
                .map(TimelineEntry::from).toList();
        AppUser adjuster = claim.getAssignedAdjuster();
        return new ClaimDetail(ClaimSummary.from(claim), PolicySummary.from(claim.getPolicy()),
                EquipmentView.from(claim.getEquipment()), claim.getDescription(), claim.getReportedAt(),
                claim.getApprovedAmount(), claim.getIncidentLatitude(), claim.getIncidentLongitude(),
                claim.getFiledBy().getFullName(), adjuster == null ? null : adjuster.getFullName(),
                claim.getDecisionReason(), claim.getRepairDealer(), assessment, timeline, nextStatusesFor(claim, user));
    }

    private static Set<ClaimStatus> nextStatusesFor(Claim claim, AuthenticatedUser user) {
        if (user.isAdjuster()) {
            return claim.getStatus().nextAllowed();
        }
        boolean agentOwesInfo = user.role() == Role.AGENT && claim.getStatus() == ClaimStatus.NEEDS_INFO;
        return agentOwesInfo ? EnumSet.of(ClaimStatus.UNDER_REVIEW) : EnumSet.noneOf(ClaimStatus.class);
    }

    static String describe(ClaimAssessment assessment) {
        return "Routed to %s with fraud risk %.0f%% and priority %d".formatted(
                assessment.lane().name().replace('_', ' ').toLowerCase(), assessment.fraudProbability() * 100,
                assessment.priority());
    }
}
