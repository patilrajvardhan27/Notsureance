package com.notsureance.claims.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByAgentIdOrderByFarmName(Long agentId);

    List<Policy> findAllByOrderByFarmName();
}
