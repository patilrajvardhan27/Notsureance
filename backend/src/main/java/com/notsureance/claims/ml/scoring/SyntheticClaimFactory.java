package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.policy.EquipmentCategory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SyntheticClaimFactory {

    private static final double FRAUD_RATE = 0.12;
    private static final LossCause[] CAUSES = LossCause.values();
    private static final double[] HONEST_CAUSE_WEIGHTS = {14, 5, 7, 5, 16, 12, 8, 6, 4, 23};
    private static final double[] FRAUD_CAUSE_WEIGHTS = {10, 4, 20, 26, 6, 5, 5, 4, 14, 6};

    private final Random random;
    private final LocalDate today;

    public SyntheticClaimFactory(long seed, LocalDate today) {
        this.random = new Random(seed);
        this.today = today;
    }

    public List<LabeledClaim> generate(int count) {
        List<LabeledClaim> claims = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            claims.add(next(random.nextDouble() < FRAUD_RATE));
        }
        return claims;
    }

    private LabeledClaim next(boolean fraud) {
        EquipmentCategory category = EquipmentCategory.values()[random.nextInt(EquipmentCategory.values().length)];
        double insuredValue = between(category.typicalMinValue(), category.typicalMaxValue());
        int age = (int) Math.min(25, Math.abs(random.nextGaussian() * 6));
        LossCause cause = pick(fraud ? FRAUD_CAUSE_WEIGHTS : HONEST_CAUSE_WEIGHTS);
        double lossToValue = fraud && random.nextDouble() < 0.55 ? between(0.5, 1.1) : between(0.03, 0.65);
        long tenure = fraud && random.nextDouble() < 0.45 ? (long) between(0, 60) : (long) between(30, 1800);
        long delay = (long) (-Math.log(1 - random.nextDouble()) * (fraud ? 10 : 3));
        int priorClaims = priorClaims(fraud);
        boolean gps = random.nextDouble() < (fraud ? 0.2 : 0.4);
        LocalDate incident = incidentDate(random.nextDouble() < (fraud ? 0.45 : 2 / 7.0));

        boolean mismatched = random.nextDouble() < (fraud ? 0.35 : 0.04);
        LossCause textCause = mismatched ? otherThan(cause) : cause;
        boolean brief = random.nextDouble() < (fraud ? 0.3 : 0.06);
        String description = CausePhrasebook.describe(textCause, random, brief);

        ClaimSignals signals = new ClaimSignals(category, cause, lossToValue * insuredValue, insuredValue,
                pickDeductible(), insuredValue * 1.1, age, age * (int) between(250, 650), gps, tenure, delay,
                priorClaims, incident, random.nextDouble() < 0.3, description, linkedFraud(fraud));
        double payoutRatio = fraud ? 0 : clamp(cause.typicalPayoutRatio() - 0.012 * age
                - 0.08 * Math.max(0, lossToValue - 0.5) + random.nextGaussian() * 0.05, 0.15, 1.0);
        return new LabeledClaim(signals, textCause, fraud, payoutRatio);
    }

    // Ring members tend to sit near other confirmed fraud; honest claimants rarely do. This is an assumption baked
    // into the synthetic data, so any lift it shows says as much about the generator as about the graph.
    private int linkedFraud(boolean fraud) {
        if (fraud) {
            return random.nextDouble() < 0.35 ? 1 + random.nextInt(3) : 0;
        }
        return random.nextDouble() < 0.04 ? 1 : 0;
    }

    private int priorClaims(boolean fraud) {
        double roll = random.nextDouble();
        if (fraud) {
            return roll < 0.55 ? 0 : roll < 0.8 ? 1 : roll < 0.95 ? 2 : 3;
        }
        return roll < 0.8 ? 0 : roll < 0.95 ? 1 : 2;
    }

    private LocalDate incidentDate(boolean weekend) {
        LocalDate date = today.minusDays(1 + random.nextInt(720));
        while (isWeekend(date) != weekend) {
            date = date.minusDays(1);
        }
        return date;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private LossCause pick(double[] weights) {
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll <= 0) {
                return CAUSES[i];
            }
        }
        return CAUSES[CAUSES.length - 1];
    }

    private LossCause otherThan(LossCause cause) {
        LossCause other = cause;
        while (other == cause) {
            other = CAUSES[random.nextInt(CAUSES.length)];
        }
        return other;
    }

    private double pickDeductible() {
        double[] options = {1000, 2500, 5000, 10000};
        return options[random.nextInt(options.length)];
    }

    private double between(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
