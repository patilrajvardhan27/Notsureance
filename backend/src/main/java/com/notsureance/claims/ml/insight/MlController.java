package com.notsureance.claims.ml.insight;

import com.notsureance.claims.ml.insight.RiskSegmentationService.Segment;
import com.notsureance.claims.ml.insight.SimilarClaimsService.Neighborhood;
import com.notsureance.claims.ml.scoring.ClaimScoringService;
import com.notsureance.claims.ml.scoring.ModelReport;
import com.notsureance.claims.ml.scoring.ModelTrainingService;
import com.notsureance.claims.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ml")
public class MlController {

    private final ClaimScoringService scoring;
    private final ModelTrainingService trainer;
    private final RiskSegmentationService segmentation;
    private final SimilarClaimsService similarClaims;

    public MlController(ClaimScoringService scoring, ModelTrainingService trainer,
                        RiskSegmentationService segmentation, SimilarClaimsService similarClaims) {
        this.scoring = scoring;
        this.trainer = trainer;
        this.segmentation = segmentation;
        this.similarClaims = similarClaims;
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('ADJUSTER')")
    public ModelReport report() {
        return scoring.currentReport();
    }

    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADJUSTER')")
    public ModelReport retrain() {
        return trainer.retrain();
    }

    @GetMapping("/segments")
    public List<Segment> segments(@AuthenticationPrincipal AuthenticatedUser user) {
        return segmentation.segment(user);
    }

    @GetMapping("/claims/{claimId}/similar")
    @PreAuthorize("hasRole('ADJUSTER')")
    public Neighborhood similar(@PathVariable Long claimId, @AuthenticationPrincipal AuthenticatedUser user) {
        return similarClaims.nearest(claimId, user);
    }
}
