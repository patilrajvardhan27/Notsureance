package com.notsureance.claims.agent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentProposalRepository extends JpaRepository<AgentProposal, Long> {

    List<AgentProposal> findByStatusOrderByCreatedAtAsc(AgentProposal.Status status);

    Optional<AgentProposal> findFirstByRunId(Long runId);

    boolean existsByRunId(Long runId);
}
