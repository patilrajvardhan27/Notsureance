package com.notsureance.claims.policy;

import com.notsureance.claims.policy.PolicyViews.EquipmentView;
import com.notsureance.claims.policy.PolicyViews.PolicySummary;
import com.notsureance.claims.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<PolicySummary> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.visibleTo(user).stream().map(PolicySummary::from).toList();
    }

    @GetMapping("/{policyId}/equipment")
    public List<EquipmentView> equipment(@PathVariable Long policyId, @AuthenticationPrincipal AuthenticatedUser user) {
        return policyService.equipmentOn(policyId, user).stream().map(EquipmentView::from).toList();
    }
}
