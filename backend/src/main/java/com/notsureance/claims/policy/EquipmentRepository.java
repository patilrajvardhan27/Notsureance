package com.notsureance.claims.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByPolicyIdOrderByModelYearDesc(Long policyId);

    List<Equipment> findByPolicyIdIn(List<Long> policyIds);
}
