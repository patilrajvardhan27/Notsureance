package com.notsureance.claims.ml.insight;

import com.notsureance.claims.claim.Claim;
import com.notsureance.claims.claim.ClaimRepository;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.claim.ClaimStatus;
import com.notsureance.claims.claim.LossCause;
import com.notsureance.claims.ml.core.KMeans;
import com.notsureance.claims.ml.core.StandardScaler;
import com.notsureance.claims.policy.EquipmentCategory;
import com.notsureance.claims.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SimilarClaimsService {

    private static final int NEIGHBORS = 5;

    private final ClaimService claimService;
    private final ClaimRepository claimRepository;

    public SimilarClaimsService(ClaimService claimService, ClaimRepository claimRepository) {
        this.claimService = claimService;
        this.claimRepository = claimRepository;
    }

    public record SimilarClaim(Long id, String claimNumber, LossCause cause, ClaimStatus status,
                               BigDecimal estimatedLoss, BigDecimal approvedAmount, double similarity) {
    }

    public record Neighborhood(List<SimilarClaim> neighbors, double approvalRate, double medianPayoutRatio) {
    }

    public Neighborhood nearest(Long claimId, AuthenticatedUser user) {
        Claim target = claimService.visibleClaim(claimId, user);
        List<Claim> candidates = claimRepository.findByStatusIn(EnumSet.of(ClaimStatus.APPROVED, ClaimStatus.PAID,
                ClaimStatus.DENIED)).stream().filter(c -> !c.getId().equals(claimId)).toList();
        if (candidates.isEmpty()) {
            return new Neighborhood(List.of(), 0, 0);
        }
        List<double[]> rows = new ArrayList<>(candidates.stream().map(SimilarClaimsService::vector).toList());
        rows.add(vector(target));
        double[][] scaled = StandardScaler.fit(rows.toArray(double[][]::new)).transformAll(rows.toArray(double[][]::new));
        double[] query = scaled[scaled.length - 1];

        List<SimilarClaim> neighbors = IntStream.range(0, candidates.size()).boxed()
                .sorted(Comparator.comparingDouble(i -> KMeans.squaredDistance(scaled[i], query)))
                .limit(NEIGHBORS)
                .map(i -> toView(candidates.get(i), Math.sqrt(KMeans.squaredDistance(scaled[i], query))))
                .toList();
        double approved = neighbors.stream().filter(n -> n.status() != ClaimStatus.DENIED).count();
        double[] ratios = neighbors.stream().filter(n -> n.approvedAmount() != null)
                .mapToDouble(n -> n.approvedAmount().doubleValue() / n.estimatedLoss().doubleValue()).sorted().toArray();
        double median = ratios.length == 0 ? 0 : ratios[ratios.length / 2];
        return new Neighborhood(neighbors, round(approved / neighbors.size()), round(median));
    }

    private static double[] vector(Claim claim) {
        int causes = LossCause.values().length;
        double[] v = new double[3 + causes + EquipmentCategory.values().length];
        double insured = claim.getEquipment().getInsuredValue().doubleValue();
        v[0] = Math.log(claim.getEstimatedLoss().doubleValue());
        v[1] = claim.getEstimatedLoss().doubleValue() / insured;
        v[2] = Year.now().getValue() - claim.getEquipment().getModelYear();
        v[3 + claim.getCause().ordinal()] = 2;
        v[3 + causes + claim.getEquipment().getCategory().ordinal()] = 1;
        return v;
    }

    private static SimilarClaim toView(Claim claim, double distance) {
        return new SimilarClaim(claim.getId(), claim.getClaimNumber(), claim.getCause(), claim.getStatus(),
                claim.getEstimatedLoss(), claim.getApprovedAmount(), round(1 / (1 + distance)));
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
