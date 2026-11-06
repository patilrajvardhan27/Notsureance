package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.claim.LossCause;

public record LabeledClaim(ClaimSignals signals, LossCause textCause, boolean fraud, double payoutRatio) {
}
