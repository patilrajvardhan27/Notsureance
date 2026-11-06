package com.notsureance.claims.analytics;

import com.notsureance.claims.claim.ClaimRepository;
import com.notsureance.claims.claim.ClaimStatus;
import com.notsureance.claims.claim.TriageLane;
import com.notsureance.claims.quote.LeadStatus;
import com.notsureance.claims.quote.QuoteLeadRepository;
import com.notsureance.claims.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ClaimRepository claims;
    private final QuoteLeadRepository leads;

    public DashboardService(ClaimRepository claims, QuoteLeadRepository leads) {
        this.claims = claims;
        this.leads = leads;
    }

    public record DashboardStats(Map<ClaimStatus, Long> claimsByStatus, Map<TriageLane, Long> openByLane,
                                 long openClaims, long totalClaims, BigDecimal approvedAmount,
                                 double fastTrackShare, long newLeads) {
    }

    public DashboardStats statsFor(AuthenticatedUser user) {
        Long agentFilter = user.isAdjuster() ? null : user.id();
        Map<ClaimStatus, Long> byStatus = new EnumMap<>(ClaimStatus.class);
        for (ClaimStatus status : ClaimStatus.values()) {
            byStatus.put(status, 0L);
        }
        claims.countByStatus(agentFilter).forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));

        Map<TriageLane, Long> byLane = new EnumMap<>(TriageLane.class);
        for (TriageLane lane : TriageLane.values()) {
            byLane.put(lane, 0L);
        }
        var openStatuses = EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.NEEDS_INFO);
        claims.countOpenByLane(openStatuses, agentFilter).stream()
                .filter(row -> row.getLane() != null)
                .forEach(row -> byLane.put(row.getLane(), row.getTotal()));

        long open = openStatuses.stream().mapToLong(byStatus::get).sum();
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        double fastShare = open == 0 ? 0 : byLane.get(TriageLane.FAST_TRACK) / (double) open;
        BigDecimal approved = claims.sumApproved(EnumSet.of(ClaimStatus.APPROVED, ClaimStatus.PAID), agentFilter);
        long newLeads = user.isAdjuster() ? leads.countByStatus(LeadStatus.NEW)
                : leads.countByAssignedAgentIdAndStatus(user.id(), LeadStatus.NEW);
        return new DashboardStats(byStatus, byLane, open, total, approved, Math.round(fastShare * 1000) / 1000.0, newLeads);
    }
}
