package com.notsureance.claims.policy;

import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policies;
    private final EquipmentRepository equipment;

    public PolicyService(PolicyRepository policies, EquipmentRepository equipment) {
        this.policies = policies;
        this.equipment = equipment;
    }

    public List<Policy> visibleTo(AuthenticatedUser user) {
        return user.seesAllClaims() ? policies.findAllByOrderByFarmName() : policies.findByAgentIdOrderByFarmName(user.id());
    }

    public Policy accessiblePolicy(Long policyId, AuthenticatedUser user) {
        Policy policy = policies.findById(policyId).orElseThrow(() -> new NotFoundException("Policy", policyId));
        if (!user.seesAllClaims() && !policy.getAgent().getId().equals(user.id())) {
            throw new NotFoundException("Policy", policyId);
        }
        return policy;
    }

    public List<Equipment> equipmentOn(Long policyId, AuthenticatedUser user) {
        accessiblePolicy(policyId, user);
        return equipment.findByPolicyIdOrderByModelYearDesc(policyId);
    }

    public Equipment equipmentOnPolicy(Long policyId, Long equipmentId) {
        Equipment item = equipment.findById(equipmentId)
                .orElseThrow(() -> new NotFoundException("Equipment", equipmentId));
        if (!item.getPolicy().getId().equals(policyId)) {
            throw new NotFoundException("Equipment", equipmentId);
        }
        return item;
    }
}
