package com.notsureance.claims.quote;

import com.notsureance.claims.quote.QuoteDtos.Adjustment;
import com.notsureance.claims.quote.QuoteDtos.QuoteRequest;
import com.notsureance.claims.quote.QuoteDtos.QuoteResult;
import com.notsureance.claims.quote.QuoteDtos.Suggestion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PremiumCalculator {

    private static final double MINIMUM_PREMIUM = 250;
    private static final Set<String> HAIL_BELT = Set.of("TX", "OK", "KS", "NE", "IA", "SD", "ND", "CO", "MN");
    private static final Set<String> FLOOD_PRONE = Set.of("LA", "MS", "AR", "MO", "IL");

    private final Clock clock;

    public PremiumCalculator(Clock clock) {
        this.clock = clock;
    }

    public QuoteResult quote(QuoteRequest request) {
        List<Adjustment> adjustments = new ArrayList<>();
        double base = request.equipmentValue().doubleValue() * request.category().baseRate();
        double premium = base;

        int age = Math.max(0, Year.now(clock).getValue() - request.modelYear());
        premium = apply(premium, Math.min(1.35, 1 + 0.015 * age), "Equipment age (" + age + " years)", adjustments);
        premium = apply(premium, regionFactor(request.stateCode()), "Regional weather exposure", adjustments);
        premium = apply(premium, deductibleFactor(request.deductible().doubleValue()), "Deductible credit", adjustments);
        if (request.gpsTracked()) {
            premium = apply(premium, 0.90, "GPS anti-theft tracking", adjustments);
        }
        if (request.safetyTraining()) {
            premium = apply(premium, 0.95, "Operator safety training", adjustments);
        }
        if (request.otherInsuredUnits() > 0) {
            premium = apply(premium, 1 - Math.min(0.12, 0.03 * request.otherInsuredUnits()), "Multi-unit fleet", adjustments);
        }
        if (request.claimFreeYears() > 0) {
            premium = apply(premium, 1 - Math.min(0.10, 0.02 * request.claimFreeYears()), "Claim-free history", adjustments);
        }
        if (request.bundleFarmPolicy()) {
            premium = apply(premium, 0.92, "Farm and ranch bundle", adjustments);
        }
        premium = Math.max(MINIMUM_PREMIUM, premium);
        return new QuoteResult(money(base), money(premium), money(premium / 12), adjustments, suggestions(request, premium));
    }

    private List<Suggestion> suggestions(QuoteRequest request, double current) {
        List<Suggestion> ideas = new ArrayList<>();
        if (!request.gpsTracked()) {
            ideas.add(new Suggestion("Add GPS anti-theft tracking", money(current * 0.10)));
        }
        if (!request.safetyTraining()) {
            ideas.add(new Suggestion("Complete an operator safety course", money(current * 0.05)));
        }
        if (!request.bundleFarmPolicy()) {
            ideas.add(new Suggestion("Bundle with a farm and ranch policy", money(current * 0.08)));
        }
        if (request.deductible().doubleValue() < 5000) {
            ideas.add(new Suggestion("Raise the deductible to $5,000", money(current * 0.08)));
        }
        return ideas;
    }

    private static double apply(double premium, double factor, String label, List<Adjustment> adjustments) {
        double adjusted = premium * factor;
        if (Math.abs(adjusted - premium) >= 0.005) {
            adjustments.add(new Adjustment(label, money(adjusted - premium)));
        }
        return adjusted;
    }

    private static double regionFactor(String state) {
        if (HAIL_BELT.contains(state)) {
            return 1.15;
        }
        return FLOOD_PRONE.contains(state) ? 1.10 : 1.0;
    }

    private static double deductibleFactor(double deductible) {
        if (deductible >= 10_000) {
            return 0.78;
        }
        if (deductible >= 5_000) {
            return 0.86;
        }
        return deductible >= 2_500 ? 0.93 : 1.0;
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
