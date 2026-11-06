package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.policy.EquipmentCategory;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record ClaimSignals(
        EquipmentCategory category,
        LossCause cause,
        double estimatedLoss,
        double insuredValue,
        double deductible,
        double coverageLimit,
        int equipmentAgeYears,
        int engineHours,
        boolean gpsTracked,
        long policyTenureDays,
        long reportingDelayDays,
        int priorClaims,
        LocalDate incidentDate,
        boolean harvestCritical,
        String description) {

    public double lossToValue() {
        return estimatedLoss / Math.max(insuredValue, 1.0);
    }

    public boolean weekendIncident() {
        DayOfWeek day = incidentDate.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public boolean inPlantingOrHarvestSeason() {
        int month = incidentDate.getMonthValue();
        return month == 4 || month == 5 || (month >= 9 && month <= 11);
    }
}
