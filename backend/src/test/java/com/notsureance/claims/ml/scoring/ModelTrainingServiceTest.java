package com.notsureance.claims.ml.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.claim.TriageLane;
import com.notsureance.claims.policy.EquipmentCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModelTrainingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);
    private static ModelRegistry registry;
    private static ModelReport report;
    private static ClaimScoringService scoring;

    @BeforeAll
    static void train() {
        registry = new ModelRegistry();
        ModelTrainingService trainer = new ModelTrainingService(registry, List::of, event -> { }, CLOCK, 2000);
        report = trainer.retrain();
        scoring = new ClaimScoringService(registry, trainer, new TriagePolicy(new TriageSettings(0, 0, 0, 0)));
    }

    @Test
    void reportsUsefulHoldoutMetrics() {
        assertThat(report.fraudHoldout().rocAuc()).isGreaterThan(0.8);
        assertThat(report.fraudCrossValidatedAuc()).isGreaterThan(0.8);
        assertThat(report.causeTextAccuracy()).isGreaterThan(0.85);
        assertThat(report.payoutHoldout().rSquared()).isGreaterThan(0.5);
        assertThat(report.fraudFeatureWeights()).hasSize(ClaimFeatures.FRAUD_FEATURE_LABELS.size());
    }

    @Test
    void cleanSmallClaimIsFastTracked() {
        ClaimSignals clean = signals(LossCause.HAIL, 6_000, 400, 1, true,
                "Golf ball sized hail cracked the windshield and dented the cab roof. Photos are attached for review.");
        ClaimAssessment assessment = scoring.assess(clean);

        assertThat(assessment.lane()).isEqualTo(TriageLane.FAST_TRACK);
        assertThat(assessment.predictedCause()).isEqualTo(LossCause.HAIL);
        assertThat(assessment.predictedPayout()).isPositive();
    }

    @Test
    void suspiciousTheftGoesToInvestigation() {
        ClaimSignals suspicious = signals(LossCause.THEFT, 170_000, 9, 14, false,
                "Engine seized and the tractor will not start.");
        ClaimAssessment assessment = scoring.assess(suspicious);

        assertThat(assessment.fraudProbability()).isGreaterThan(0.6);
        assertThat(assessment.lane()).isEqualTo(TriageLane.SPECIAL_INVESTIGATION);
        assertThat(assessment.topFactors()).isNotEmpty();
    }

    private static ClaimSignals signals(LossCause cause, double loss, long tenure, long delay, boolean gps, String text) {
        return new ClaimSignals(EquipmentCategory.TRACTOR, cause, loss, 190_000, 2_500, 1_000_000, 6, 3_000, gps,
                tenure, delay, 0, LocalDate.of(2026, 9, 23), false, text);
    }
}
