package com.notsureance.claims.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notsureance.claims.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class ReportingViewsIT extends IntegrationTest {

    private static final List<String> VIEWS = List.of("rpt_claims_daily", "rpt_loss_ratio_by_segment",
            "rpt_adjuster_cycle_time", "rpt_fraud_funnel", "rpt_fraud_by_segment", "rpt_agent_proposals");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void everyReportViewAnswersOnTheDemoData() {
        for (String view : List.of("rpt_claims_daily", "rpt_loss_ratio_by_segment", "rpt_adjuster_cycle_time",
                "rpt_fraud_funnel", "rpt_fraud_by_segment")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + view, Long.class)).as(view).isPositive();
        }
        Double lossRatio = jdbc.queryForObject(
                "SELECT max(loss_ratio) FROM rpt_loss_ratio_by_segment WHERE incurred_losses > 0", Double.class);
        assertThat(lossRatio).isPositive();
        assertThat(jdbc.queryForObject("SELECT claims FROM rpt_fraud_funnel WHERE stage = 'Confirmed fraud'", Long.class))
                .isEqualTo(1);
    }

    @Test
    void theReportRoleCanReadViewsButNotTables() {
        transactions.executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL ROLE report_reader");
            VIEWS.forEach(view -> jdbc.queryForObject("SELECT count(*) FROM " + view, Long.class));
            assertThatThrownBy(() -> jdbc.queryForObject("SELECT count(*) FROM claims", Long.class))
                    .satisfies(ex -> assertThat(NestedExceptionUtils.getMostSpecificCause(ex).getMessage())
                            .contains("permission denied"));
            status.setRollbackOnly();
        });
    }
}
