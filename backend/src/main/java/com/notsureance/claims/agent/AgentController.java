package com.notsureance.claims.agent;

import com.notsureance.claims.agent.AgentViews.DecisionNote;
import com.notsureance.claims.agent.AgentViews.ProposalView;
import com.notsureance.claims.agent.AgentViews.RunRequest;
import com.notsureance.claims.agent.AgentViews.RunView;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private final AgentService agent;
    private final AgentProposalService proposals;

    public AgentController(AgentService agent, AgentProposalService proposals) {
        this.agent = agent;
        this.proposals = proposals;
    }

    @PostMapping("/api/claims/{claimId}/agent-runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADJUSTER')")
    public RunView run(@PathVariable Long claimId, @Valid @RequestBody(required = false) RunRequest request,
                       @AuthenticationPrincipal AuthenticatedUser user) {
        return agent.run(claimId, request == null ? null : request.question(), user);
    }

    @GetMapping("/api/claims/{claimId}/agent-runs")
    @PreAuthorize("hasAnyRole('ADJUSTER', 'MANAGER')")
    public List<RunView> runs(@PathVariable Long claimId, @AuthenticationPrincipal AuthenticatedUser user) {
        return agent.recentRuns(claimId, user);
    }

    @GetMapping("/api/agent/proposals")
    @PreAuthorize("hasAnyRole('ADJUSTER', 'MANAGER')")
    public List<ProposalView> pending() {
        return agent.pendingProposals();
    }

    @PostMapping("/api/agent/proposals/{proposalId}/approve")
    @PreAuthorize("hasRole('ADJUSTER')")
    public ClaimDetail approve(@PathVariable Long proposalId, @Valid @RequestBody(required = false) DecisionNote body,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        return proposals.approve(proposalId, body == null ? null : body.note(), user);
    }

    @PostMapping("/api/agent/proposals/{proposalId}/reject")
    @PreAuthorize("hasRole('ADJUSTER')")
    public ResponseEntity<Void> reject(@PathVariable Long proposalId, @Valid @RequestBody(required = false) DecisionNote body,
                                       @AuthenticationPrincipal AuthenticatedUser user) {
        proposals.reject(proposalId, body == null ? null : body.note(), user);
        return ResponseEntity.noContent().build();
    }
}
