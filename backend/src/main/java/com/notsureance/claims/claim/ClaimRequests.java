package com.notsureance.claims.claim;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class ClaimRequests {

    private ClaimRequests() {
    }

    public record FileClaim(
            @NotNull Long policyId,
            @NotNull Long equipmentId,
            @NotNull LossCause cause,
            @NotNull @PastOrPresent LocalDate incidentDate,
            @NotBlank @Size(min = 20, max = 2000) String description,
            @NotNull @DecimalMin("100.00") @DecimalMax("5000000.00") @Digits(integer = 12, fraction = 2)
            BigDecimal estimatedLoss,
            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            boolean harvestCritical,
            @Size(max = 120) String repairDealer) {
    }

    public enum DecisionAction {
        START_REVIEW,
        APPROVE,
        DENY,
        REQUEST_INFO,
        MARK_PAID
    }

    public record Decision(
            @NotNull DecisionAction action,
            @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
            @Size(max = 1000) String note,
            boolean suspectedFraud) {
    }

    public record InfoResponse(@NotBlank @Size(max = 1000) String note) {
    }
}
