package com.notsureance.claims.policy;

public enum EquipmentCategory {
    TRACTOR("Tractor", 0.0105, 60_000, 450_000),
    COMBINE("Combine harvester", 0.0130, 250_000, 900_000),
    SPRAYER("Self-propelled sprayer", 0.0120, 150_000, 600_000),
    PLANTER("Planter", 0.0090, 40_000, 300_000),
    BALER("Baler", 0.0110, 25_000, 120_000),
    IRRIGATION_PIVOT("Center pivot irrigation", 0.0080, 50_000, 180_000),
    GRAIN_DRYER("Grain dryer", 0.0095, 40_000, 250_000),
    UTILITY_VEHICLE("Utility vehicle", 0.0150, 10_000, 35_000);

    private final String label;
    private final double baseRate;
    private final double typicalMinValue;
    private final double typicalMaxValue;

    EquipmentCategory(String label, double baseRate, double typicalMinValue, double typicalMaxValue) {
        this.label = label;
        this.baseRate = baseRate;
        this.typicalMinValue = typicalMinValue;
        this.typicalMaxValue = typicalMaxValue;
    }

    public String label() {
        return label;
    }

    public double baseRate() {
        return baseRate;
    }

    public double typicalMinValue() {
        return typicalMinValue;
    }

    public double typicalMaxValue() {
        return typicalMaxValue;
    }
}
