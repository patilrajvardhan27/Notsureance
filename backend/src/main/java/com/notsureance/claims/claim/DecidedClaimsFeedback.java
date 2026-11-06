package com.notsureance.claims.claim;

import com.notsureance.claims.ml.scoring.LabeledClaim;
import com.notsureance.claims.ml.scoring.TrainingFeedbackSource;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DecidedClaimsFeedback implements TrainingFeedbackSource {

    private final ClaimRepository claims;
    private final ClaimSignalsFactory signalsFactory;

    public DecidedClaimsFeedback(ClaimRepository claims, ClaimSignalsFactory signalsFactory) {
        this.claims = claims;
        this.signalsFactory = signalsFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabeledClaim> decidedClaims() {
        return claims.findByStatusIn(EnumSet.of(ClaimStatus.APPROVED, ClaimStatus.PAID, ClaimStatus.DENIED)).stream()
                .filter(claim -> claim.getStatus() != ClaimStatus.DENIED || claim.isConfirmedFraud())
                .map(this::toLabeled)
                .toList();
    }

    private LabeledClaim toLabeled(Claim claim) {
        boolean fraud = claim.isConfirmedFraud();
        double coveredLoss = Math.min(claim.getEstimatedLoss().doubleValue(),
                claim.getEquipment().getInsuredValue().doubleValue());
        double ratio = fraud || claim.getApprovedAmount() == null ? 0
                : (claim.getApprovedAmount().doubleValue() + claim.getPolicy().getDeductible().doubleValue()) / coveredLoss;
        return new LabeledClaim(signalsFactory.from(claim), claim.getCause(), fraud, Math.min(ratio, 1.0));
    }
}
