package com.notsureance.claims.agent;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    List<AgentRun> findTop5ByClaimIdOrderByCreatedAtDesc(Long claimId);
}
