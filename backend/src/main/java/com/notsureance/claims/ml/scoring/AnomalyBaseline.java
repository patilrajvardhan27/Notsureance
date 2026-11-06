package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.policy.EquipmentCategory;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record AnomalyBaseline(Map<EquipmentCategory, double[]> meanAndSpread) {

    public static AnomalyBaseline fit(List<ClaimSignals> honestClaims) {
        Map<EquipmentCategory, double[]> stats = new EnumMap<>(EquipmentCategory.class);
        for (EquipmentCategory category : EquipmentCategory.values()) {
            double[] values = honestClaims.stream().filter(s -> s.category() == category)
                    .mapToDouble(s -> Math.log(s.lossToValue())).toArray();
            if (values.length < 5) {
                continue;
            }
            double mean = Arrays.stream(values).average().orElse(0);
            double variance = Arrays.stream(values).map(v -> (v - mean) * (v - mean)).average().orElse(1);
            stats.put(category, new double[] {mean, Math.max(Math.sqrt(variance), 1e-6)});
        }
        return new AnomalyBaseline(stats);
    }

    public double zScore(ClaimSignals signals) {
        double[] stat = meanAndSpread.get(signals.category());
        if (stat == null) {
            return 0;
        }
        return Math.max(0, (Math.log(signals.lossToValue()) - stat[0]) / stat[1]);
    }
}
