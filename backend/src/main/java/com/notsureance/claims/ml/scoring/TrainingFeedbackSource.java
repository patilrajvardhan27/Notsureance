package com.notsureance.claims.ml.scoring;

import java.util.List;

public interface TrainingFeedbackSource {

    List<LabeledClaim> decidedClaims();
}
