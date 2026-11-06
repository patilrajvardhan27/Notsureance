package com.notsureance.claims.quote;

import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.quote.QuoteDtos.LeadReceipt;
import com.notsureance.claims.quote.QuoteDtos.LeadRequest;
import com.notsureance.claims.quote.QuoteDtos.QuoteResult;
import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.Role;
import com.notsureance.claims.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LeadService {

    private final QuoteLeadRepository leads;
    private final UserRepository users;
    private final PremiumCalculator calculator;

    public LeadService(QuoteLeadRepository leads, UserRepository users, PremiumCalculator calculator) {
        this.leads = leads;
        this.users = users;
        this.calculator = calculator;
    }

    public LeadReceipt capture(LeadRequest request) {
        QuoteResult quote = calculator.quote(request.quote());
        AppUser agent = leastBusyAgent();
        QuoteLead lead = leads.save(new QuoteLead(request, quote.annualPremium(), agent));
        return new LeadReceipt(lead.getId(), agent == null ? null : agent.getFullName(), quote);
    }

    @Transactional(readOnly = true)
    public List<QuoteLead> assignedTo(Long agentId) {
        return leads.findByAssignedAgentIdOrderByCreatedAtDesc(agentId);
    }

    public QuoteLead updateStatus(Long leadId, LeadStatus status, Long agentId) {
        QuoteLead lead = leads.findByIdAndAssignedAgentId(leadId, agentId)
                .orElseThrow(() -> new NotFoundException("Lead", leadId));
        lead.changeStatus(status);
        return lead;
    }

    private AppUser leastBusyAgent() {
        return users.findByRoleAndActiveTrue(Role.AGENT).stream()
                .min(Comparator.comparingLong((AppUser a) -> leads.countByAssignedAgentIdAndStatus(a.getId(), LeadStatus.NEW))
                        .thenComparing(AppUser::getId))
                .orElse(null);
    }
}
