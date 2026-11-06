package com.notsureance.claims.quote;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteLeadRepository extends JpaRepository<QuoteLead, Long> {

    List<QuoteLead> findByAssignedAgentIdOrderByCreatedAtDesc(Long agentId);

    Optional<QuoteLead> findByIdAndAssignedAgentId(Long id, Long agentId);

    long countByAssignedAgentIdAndStatus(Long agentId, LeadStatus status);

    long countByStatus(LeadStatus status);
}
