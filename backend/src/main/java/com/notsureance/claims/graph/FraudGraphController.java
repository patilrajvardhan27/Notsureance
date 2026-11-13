package com.notsureance.claims.graph;

import com.notsureance.claims.graph.GraphViews.FraudRing;
import com.notsureance.claims.graph.GraphViews.LinkedParties;
import com.notsureance.claims.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADJUSTER', 'MANAGER')")
public class FraudGraphController {

    private final FraudGraphService graph;

    public FraudGraphController(FraudGraphService graph) {
        this.graph = graph;
    }

    @GetMapping("/api/claims/{claimId}/linked-parties")
    public LinkedParties linkedParties(@PathVariable Long claimId, @AuthenticationPrincipal AuthenticatedUser user) {
        return graph.linkedParties(claimId, user);
    }

    @GetMapping("/api/fraud/rings")
    public List<FraudRing> rings() {
        return graph.rings();
    }
}
