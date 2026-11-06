package com.notsureance.claims.ml.insight;

import com.notsureance.claims.claim.ClaimRepository;
import com.notsureance.claims.claim.ClaimRepository.PolicyClaimStats;
import com.notsureance.claims.ml.core.KMeans;
import com.notsureance.claims.ml.core.StandardScaler;
import com.notsureance.claims.policy.Equipment;
import com.notsureance.claims.policy.EquipmentRepository;
import com.notsureance.claims.policy.Policy;
import com.notsureance.claims.policy.PolicyService;
import com.notsureance.claims.security.AuthenticatedUser;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RiskSegmentationService {

    private static final int SEGMENTS = 3;
    private static final String[] NAMES = {"Steady", "Watch", "Elevated"};
    private static final String[] PLAYS = {
            "Offer claim-free renewal credit and a farm bundle",
            "Recommend GPS tracking and an operator safety course",
            "Schedule a loss-control visit before renewal"};

    private final PolicyService policyService;
    private final EquipmentRepository equipmentRepository;
    private final ClaimRepository claimRepository;

    public RiskSegmentationService(PolicyService policyService, EquipmentRepository equipmentRepository,
                                   ClaimRepository claimRepository) {
        this.policyService = policyService;
        this.equipmentRepository = equipmentRepository;
        this.claimRepository = claimRepository;
    }

    public record PolicyRef(Long id, String policyNumber, String farmName) {
    }

    public record Segment(String name, String recommendedPlay, int policyCount, double averageClaims,
                          double averageFraudScore, double gpsShare, List<PolicyRef> policies) {
    }

    public List<Segment> segment(AuthenticatedUser user) {
        List<Policy> policies = policyService.visibleTo(user);
        if (policies.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Equipment>> fleet = equipmentRepository.findByPolicyIdIn(policies.stream().map(Policy::getId).toList())
                .stream().collect(Collectors.groupingBy(e -> e.getPolicy().getId()));
        Map<Long, PolicyClaimStats> stats = claimRepository.claimStatsByPolicy().stream()
                .collect(Collectors.toMap(PolicyClaimStats::getPolicyId, Function.identity()));

        double[][] raw = policies.stream().map(p -> features(p, fleet.getOrDefault(p.getId(), List.of()), stats.get(p.getId())))
                .toArray(double[][]::new);
        KMeans.Result result = KMeans.fit(StandardScaler.fit(raw).transformAll(raw), SEGMENTS, 100, 7L);

        List<Integer> clusterOrder = IntStream.range(0, result.centroids().length).boxed()
                .sorted(Comparator.comparingDouble(c -> riskIndex(result.centroids()[c]))).toList();
        List<Segment> segments = new ArrayList<>();
        for (int rank = 0; rank < clusterOrder.size(); rank++) {
            int cluster = clusterOrder.get(rank);
            List<Integer> members = IntStream.range(0, policies.size()).filter(i -> result.assignments()[i] == cluster)
                    .boxed().toList();
            if (members.isEmpty()) {
                continue;
            }
            int label = clusterOrder.size() < SEGMENTS ? Math.min(rank + 1, SEGMENTS - 1) : rank;
            segments.add(new Segment(NAMES[label], PLAYS[label], members.size(), average(raw, members, 3),
                    average(raw, members, 4), average(raw, members, 5),
                    members.stream().map(policies::get)
                            .map(p -> new PolicyRef(p.getId(), p.getPolicyNumber(), p.getFarmName())).toList()));
        }
        return segments;
    }

    private static double[] features(Policy policy, List<Equipment> equipment, PolicyClaimStats stats) {
        int year = Year.now().getValue();
        double insured = equipment.stream().mapToDouble(e -> e.getInsuredValue().doubleValue()).sum();
        double age = equipment.stream().mapToInt(e -> year - e.getModelYear()).average().orElse(0);
        double gps = equipment.stream().filter(Equipment::isGpsTracked).count() / (double) Math.max(1, equipment.size());
        return new double[] {Math.log(policy.getAcreage()), Math.log1p(insured), age,
                stats == null ? 0 : stats.getClaimCount(), stats == null ? 0 : stats.getAverageFraud(), gps};
    }

    private static double riskIndex(double[] centroid) {
        return centroid[3] + 1.5 * centroid[4] + 0.3 * centroid[2] - 0.5 * centroid[5];
    }

    private static double average(double[][] raw, List<Integer> members, int column) {
        double value = members.stream().mapToDouble(i -> raw[i][column]).average().orElse(0);
        return Math.round(value * 1000) / 1000.0;
    }
}
