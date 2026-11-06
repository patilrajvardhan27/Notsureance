package com.notsureance.claims.quote;

import com.notsureance.claims.quote.QuoteDtos.LeadReceipt;
import com.notsureance.claims.quote.QuoteDtos.LeadRequest;
import com.notsureance.claims.quote.QuoteDtos.QuoteRequest;
import com.notsureance.claims.quote.QuoteDtos.QuoteResult;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.security.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuoteController {

    private final PremiumCalculator calculator;
    private final LeadService leadService;
    private final RequestRateLimiter rateLimiter;

    public QuoteController(PremiumCalculator calculator, LeadService leadService, RequestRateLimiter rateLimiter) {
        this.calculator = calculator;
        this.leadService = leadService;
        this.rateLimiter = rateLimiter;
    }

    public record LeadView(Long id, String fullName, String email, String phone, String stateCode, String category,
                           BigDecimal equipmentValue, BigDecimal annualPremium, LeadStatus status, Instant createdAt) {

        static LeadView from(QuoteLead l) {
            return new LeadView(l.getId(), l.getFullName(), l.getEmail(), l.getPhone(), l.getStateCode(),
                    l.getCategory().label(), l.getEquipmentValue(), l.getAnnualPremium(), l.getStatus(), l.getCreatedAt());
        }
    }

    public record LeadStatusChange(@NotNull LeadStatus status) {
    }

    @PostMapping("/quotes")
    public QuoteResult quote(@Valid @RequestBody QuoteRequest request, HttpServletRequest http) {
        rateLimiter.checkAllowed("quote:" + http.getRemoteAddr());
        return calculator.quote(request);
    }

    @PostMapping("/quotes/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public LeadReceipt captureLead(@Valid @RequestBody LeadRequest request, HttpServletRequest http) {
        rateLimiter.checkAllowed("lead:" + http.getRemoteAddr());
        return leadService.capture(request);
    }

    @GetMapping("/leads")
    @PreAuthorize("hasRole('AGENT')")
    public List<LeadView> myLeads(@AuthenticationPrincipal AuthenticatedUser user) {
        return leadService.assignedTo(user.id()).stream().map(LeadView::from).toList();
    }

    @PatchMapping("/leads/{leadId}")
    @PreAuthorize("hasRole('AGENT')")
    public LeadView changeStatus(@PathVariable Long leadId, @Valid @RequestBody LeadStatusChange change,
                                 @AuthenticationPrincipal AuthenticatedUser user) {
        return LeadView.from(leadService.updateStatus(leadId, change.status(), user.id()));
    }
}
