package com.notsureance.claims.quote;

import com.notsureance.claims.policy.EquipmentCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class QuoteDtos {

    private QuoteDtos() {
    }

    public record QuoteRequest(
            @NotNull EquipmentCategory category,
            @NotNull @DecimalMin("5000") @DecimalMax("2000000") BigDecimal equipmentValue,
            @Min(1980) @Max(2100) int modelYear,
            @NotNull @Pattern(regexp = "[A-Z]{2}") String stateCode,
            @NotNull @DecimalMin("1000") @DecimalMax("10000") BigDecimal deductible,
            boolean gpsTracked,
            boolean safetyTraining,
            @Min(0) @Max(50) int otherInsuredUnits,
            @Min(0) @Max(20) int claimFreeYears,
            boolean bundleFarmPolicy) {
    }

    public record Adjustment(String label, BigDecimal amount) {
    }

    public record Suggestion(String action, BigDecimal annualSavings) {
    }

    public record QuoteResult(BigDecimal basePremium, BigDecimal annualPremium, BigDecimal monthlyPremium,
                              List<Adjustment> adjustments, List<Suggestion> suggestions) {
    }

    public record LeadRequest(
            @NotNull @Valid QuoteRequest quote,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 160) String email,
            @Pattern(regexp = "^$|^[0-9+()\\- ]{7,20}$") String phone,
            @AssertTrue(message = "Consent is required so an agent can contact you") boolean consentToContact) {
    }

    public record LeadReceipt(Long leadId, String assignedAgent, QuoteResult quote) {
    }
}
