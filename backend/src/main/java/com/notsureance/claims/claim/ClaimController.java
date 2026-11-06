package com.notsureance.claims.claim;

import com.notsureance.claims.claim.ClaimRequests.Decision;
import com.notsureance.claims.claim.ClaimRequests.FileClaim;
import com.notsureance.claims.claim.ClaimRequests.InfoResponse;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.claim.ClaimViews.ClaimSummary;
import com.notsureance.claims.common.PageView;
import com.notsureance.claims.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimWorkflowService workflow;

    public ClaimController(ClaimService claimService, ClaimWorkflowService workflow) {
        this.claimService = claimService;
        this.workflow = workflow;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AGENT')")
    public ClaimDetail file(@Valid @RequestBody FileClaim request, @AuthenticationPrincipal AuthenticatedUser user) {
        return claimService.file(request, user);
    }

    @GetMapping
    public PageView<ClaimSummary> search(@RequestParam(required = false) ClaimStatus status,
                                         @RequestParam(required = false) TriageLane lane,
                                         @RequestParam(required = false) @Size(max = 80) String q,
                                         @RequestParam(defaultValue = "false") boolean openOnly,
                                         @RequestParam(defaultValue = "NEWEST") ClaimSort sort,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return claimService.search(status, lane, q, openOnly, PageRequest.of(page, size, sort.sort()), user);
    }

    @GetMapping("/queue")
    @PreAuthorize("hasRole('ADJUSTER')")
    public PageView<ClaimSummary> queue(@RequestParam(required = false) TriageLane lane,
                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return claimService.search(null, lane, null, true, PageRequest.of(page, 50, ClaimSort.PRIORITY.sort()), user);
    }

    @GetMapping("/{claimId}")
    public ClaimDetail detail(@PathVariable Long claimId, @AuthenticationPrincipal AuthenticatedUser user) {
        return claimService.detail(claimId, user);
    }

    @PostMapping("/{claimId}/decision")
    @PreAuthorize("hasRole('ADJUSTER')")
    public ClaimDetail decide(@PathVariable Long claimId, @Valid @RequestBody Decision decision,
                              @AuthenticationPrincipal AuthenticatedUser user) {
        return workflow.decide(claimId, decision, user);
    }

    @PostMapping("/{claimId}/info")
    @PreAuthorize("hasRole('AGENT')")
    public ClaimDetail respond(@PathVariable Long claimId, @Valid @RequestBody InfoResponse response,
                               @AuthenticationPrincipal AuthenticatedUser user) {
        return workflow.respondWithInfo(claimId, response, user);
    }
}
