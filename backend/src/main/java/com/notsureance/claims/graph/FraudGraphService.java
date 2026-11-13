package com.notsureance.claims.graph;

import com.notsureance.claims.claim.Claim;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.graph.GraphViews.FraudRing;
import com.notsureance.claims.graph.GraphViews.LinkedParties;
import com.notsureance.claims.graph.GraphViews.LinkedParty;
import com.notsureance.claims.graph.GraphViews.RingMember;
import com.notsureance.claims.graph.GraphViews.SharedPair;
import com.notsureance.claims.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudGraphService {

    private static final Logger log = LoggerFactory.getLogger(FraudGraphService.class);

    private final FraudGraphRepository graph;
    private final ClaimService claimService;
    private final GraphProperties properties;

    public FraudGraphService(FraudGraphRepository graph, ClaimService claimService, GraphProperties properties) {
        this.graph = graph;
        this.claimService = claimService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public LinkedParties linkedParties(Long claimId, AuthenticatedUser user) {
        Claim claim = claimService.visibleClaim(claimId, user);
        return linkedPartiesFor(claim.getPolicy().getPolicyNumber());
    }

    public LinkedParties linkedPartiesFor(String policyNumber) {
        List<LinkedParty> parties = guarded(() -> graph.linkedParties(policyNumber));
        long flagged = parties.stream().filter(p -> p.hops() <= 2).mapToLong(LinkedParty::flaggedClaims).sum();
        return new LinkedParties(policyNumber, parties, flagged);
    }

    // Scoring must never fail because the graph is down, so this one quietly falls back to zero.
    public int flaggedClaimsNear(String policyNumber) {
        if (!properties.enabled()) {
            return 0;
        }
        try {
            return (int) Math.min(Integer.MAX_VALUE, linkedPartiesFor(policyNumber).flaggedWithinTwoHops());
        } catch (GraphUnavailableException ex) {
            log.debug("Graph lookup skipped for {}: {}", policyNumber, ex.getMessage());
            return 0;
        }
    }

    public List<FraudRing> rings() {
        List<SharedPair> pairs = guarded(graph::sharedPairs);
        Map<String, String> parent = new HashMap<>();
        Map<String, Set<String>> sharedByRoot = new HashMap<>();
        for (SharedPair pair : pairs) {
            union(parent, pair.left(), pair.right());
        }
        Map<String, List<String>> membersByRoot = new HashMap<>();
        parent.keySet().forEach(key -> membersByRoot.computeIfAbsent(find(parent, key), k -> new ArrayList<>()).add(key));
        pairs.forEach(pair -> sharedByRoot.computeIfAbsent(find(parent, pair.left()), k -> new LinkedHashSet<>())
                .addAll(pair.shared()));

        Map<String, RingMember> totals = new HashMap<>();
        guarded(() -> graph.claimantTotals(parent.keySet())).forEach(member -> totals.put(member.claimantKey(), member));
        return membersByRoot.entrySet().stream()
                .map(entry -> {
                    List<RingMember> members = entry.getValue().stream().map(totals::get)
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparingDouble(RingMember::claimedAmount).reversed()).toList();
                    return new FraudRing(members, List.copyOf(sharedByRoot.get(entry.getKey())),
                            members.stream().mapToDouble(RingMember::claimedAmount).sum(),
                            members.stream().mapToLong(RingMember::flaggedClaims).sum());
                })
                .sorted(Comparator.comparingDouble(FraudRing::totalClaimed).reversed())
                .toList();
    }

    private static String find(Map<String, String> parent, String key) {
        parent.putIfAbsent(key, key);
        String root = key;
        while (!parent.get(root).equals(root)) {
            root = parent.get(root);
        }
        parent.put(key, root);
        return root;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String rootA = find(parent, a);
        String rootB = find(parent, b);
        if (!rootA.equals(rootB)) {
            parent.put(rootB, rootA);
        }
    }

    private <T> T guarded(Supplier<T> query) {
        try {
            return query.get();
        } catch (RuntimeException ex) {
            throw new GraphUnavailableException(ex);
        }
    }
}
