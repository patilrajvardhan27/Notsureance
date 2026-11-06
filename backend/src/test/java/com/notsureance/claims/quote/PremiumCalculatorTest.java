package com.notsureance.claims.quote;

import static org.assertj.core.api.Assertions.assertThat;

import com.notsureance.claims.policy.EquipmentCategory;
import com.notsureance.claims.quote.QuoteDtos.QuoteRequest;
import com.notsureance.claims.quote.QuoteDtos.QuoteResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PremiumCalculatorTest {

    private final PremiumCalculator calculator =
            new PremiumCalculator(Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void safetyMeasuresLowerThePremium() {
        QuoteResult plain = calculator.quote(request(false, false, false));
        QuoteResult protectedFleet = calculator.quote(request(true, true, true));

        assertThat(protectedFleet.annualPremium()).isLessThan(plain.annualPremium());
        assertThat(plain.suggestions()).extracting(QuoteDtos.Suggestion::action)
                .contains("Add GPS anti-theft tracking", "Bundle with a farm and ranch policy");
    }

    @Test
    void monthlyIsATwelfthOfAnnual() {
        QuoteResult result = calculator.quote(request(false, false, false));
        assertThat(result.monthlyPremium().multiply(BigDecimal.valueOf(12)).subtract(result.annualPremium()).abs())
                .isLessThan(new BigDecimal("0.10"));
    }

    @Test
    void neverQuotesBelowTheMinimum() {
        QuoteRequest tiny = new QuoteRequest(EquipmentCategory.UTILITY_VEHICLE, new BigDecimal("5000"), 2026, "OH",
                new BigDecimal("10000"), true, true, 5, 10, true);
        assertThat(calculator.quote(tiny).annualPremium()).isEqualByComparingTo("250.00");
    }

    private static QuoteRequest request(boolean gps, boolean training, boolean bundle) {
        return new QuoteRequest(EquipmentCategory.COMBINE, new BigDecimal("500000"), 2020, "IA",
                new BigDecimal("2500"), gps, training, 0, 0, bundle);
    }
}
