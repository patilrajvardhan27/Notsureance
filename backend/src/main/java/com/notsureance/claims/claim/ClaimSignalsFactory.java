package com.notsureance.claims.claim;

import com.notsureance.claims.ml.scoring.ClaimSignals;
import com.notsureance.claims.policy.Equipment;
import com.notsureance.claims.policy.Policy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ClaimSignalsFactory {

    private final ClaimRepository claims;
    private final Clock clock;

    public ClaimSignalsFactory(ClaimRepository claims, Clock clock) {
        this.claims = claims;
        this.clock = clock;
    }

    public ClaimSignals from(Claim claim) {
        Long excludeId = claim.getId() == null ? -1L : claim.getId();
        int prior = claims.countPriorClaims(claim.getPolicy().getId(), claim.getIncidentDate().minusDays(365), excludeId);
        return from(claim, prior);
    }

    public ClaimSignals from(Claim claim, int priorClaims) {
        Policy policy = claim.getPolicy();
        Equipment equipment = claim.getEquipment();
        LocalDate reportedOn = claim.getReportedAt().atZone(ZoneOffset.UTC).toLocalDate();
        int age = Math.max(0, LocalDate.now(clock).getYear() - equipment.getModelYear());
        return new ClaimSignals(
                equipment.getCategory(),
                claim.getCause(),
                claim.getEstimatedLoss().doubleValue(),
                equipment.getInsuredValue().doubleValue(),
                policy.getDeductible().doubleValue(),
                policy.getCoverageLimit().doubleValue(),
                age,
                equipment.getEngineHours(),
                equipment.isGpsTracked(),
                Math.max(0, ChronoUnit.DAYS.between(policy.getEffectiveDate(), claim.getIncidentDate())),
                Math.max(0, ChronoUnit.DAYS.between(claim.getIncidentDate(), reportedOn)),
                priorClaims,
                claim.getIncidentDate(),
                claim.isHarvestCritical(),
                claim.getDescription());
    }
}
