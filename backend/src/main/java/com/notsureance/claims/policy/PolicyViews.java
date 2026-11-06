package com.notsureance.claims.policy;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PolicyViews {

    private PolicyViews() {
    }

    public record PolicySummary(Long id, String policyNumber, String farmName, String holderName, String stateCode,
                                String county, int acreage, BigDecimal coverageLimit, BigDecimal deductible,
                                LocalDate effectiveDate, LocalDate expiryDate) {

        public static PolicySummary from(Policy policy) {
            return new PolicySummary(policy.getId(), policy.getPolicyNumber(), policy.getFarmName(),
                    policy.getHolderName(), policy.getStateCode(), policy.getCounty(), policy.getAcreage(),
                    policy.getCoverageLimit(), policy.getDeductible(), policy.getEffectiveDate(),
                    policy.getExpiryDate());
        }
    }

    public record EquipmentView(Long id, EquipmentCategory category, String categoryLabel, String displayName,
                                String serialNumber, BigDecimal insuredValue, int engineHours, boolean gpsTracked) {

        public static EquipmentView from(Equipment equipment) {
            return new EquipmentView(equipment.getId(), equipment.getCategory(), equipment.getCategory().label(),
                    equipment.displayName(), equipment.getSerialNumber(), equipment.getInsuredValue(),
                    equipment.getEngineHours(), equipment.isGpsTracked());
        }
    }
}
