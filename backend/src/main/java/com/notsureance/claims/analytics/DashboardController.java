package com.notsureance.claims.analytics;

import com.notsureance.claims.analytics.DashboardService.DashboardStats;
import com.notsureance.claims.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardStats stats(@AuthenticationPrincipal AuthenticatedUser user) {
        return dashboardService.statsFor(user);
    }
}
