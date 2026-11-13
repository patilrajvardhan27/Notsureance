package com.notsureance.claims.agent;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentTraceRepository extends JpaRepository<AgentTrace, Long> {

    List<AgentTrace> findByRunIdOrderByStepAsc(Long runId);
}
