package com.notsureance.claims.claim;

import com.notsureance.claims.ml.scoring.ClaimScoringService;
import com.notsureance.claims.ml.scoring.ModelsTrainedEvent;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClaimRescoringListener {

    private static final Logger log = LoggerFactory.getLogger(ClaimRescoringListener.class);

    private final ClaimRepository claims;
    private final ClaimScoringService scoring;
    private final ClaimSignalsFactory signalsFactory;

    public ClaimRescoringListener(ClaimRepository claims, ClaimScoringService scoring,
                                  ClaimSignalsFactory signalsFactory) {
        this.claims = claims;
        this.scoring = scoring;
        this.signalsFactory = signalsFactory;
    }

    @EventListener
    @Transactional
    public void rescoreOpenClaims(ModelsTrainedEvent event) {
        var open = claims.findByStatusIn(EnumSet.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW,
                ClaimStatus.NEEDS_INFO));
        open.forEach(claim -> claim.applyScore(ClaimScore.from(scoring.assess(signalsFactory.from(claim)))));
        log.info("Rescored {} open claims with the latest models", open.size());
    }
}
