package com.notsureance.claims.claim;

public enum LossCause {
    COLLISION("Collision", 0.82, false),
    ROLLOVER("Rollover", 0.85, false),
    FIRE("Fire", 0.90, true),
    THEFT("Theft", 0.93, true),
    HAIL("Hail", 0.80, false),
    WIND("Windstorm", 0.78, false),
    FLOOD("Flood", 0.74, false),
    LIGHTNING("Lightning strike", 0.86, false),
    VANDALISM("Vandalism", 0.76, true),
    MECHANICAL_BREAKDOWN("Mechanical breakdown", 0.58, false);

    private final String label;
    private final double typicalPayoutRatio;
    private final boolean highRisk;

    LossCause(String label, double typicalPayoutRatio, boolean highRisk) {
        this.label = label;
        this.typicalPayoutRatio = typicalPayoutRatio;
        this.highRisk = highRisk;
    }

    public String label() {
        return label;
    }

    public double typicalPayoutRatio() {
        return typicalPayoutRatio;
    }

    public boolean isHighRisk() {
        return highRisk;
    }
}
