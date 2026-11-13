package com.notsureance.claims.graph;

import java.util.List;

public final class GraphViews {

    private GraphViews() {
    }

    public record LinkedParty(String claimantKey, String name, int hops, List<String> via, long claims,
                              long flaggedClaims, double claimedAmount) {
    }

    public record LinkedParties(String policyNumber, List<LinkedParty> parties, long flaggedWithinTwoHops) {
    }

    public record RingMember(String claimantKey, String name, long claims, long flaggedClaims, double claimedAmount) {
    }

    public record FraudRing(List<RingMember> members, List<String> sharedIdentifiers, double totalClaimed,
                            long flaggedClaims) {
    }

    record SharedPair(String left, String right, List<String> shared) {
    }

    static long flaggedWithinTwoHops(List<LinkedParty> parties) {
        return parties.stream().filter(p -> p.hops() <= 2).mapToLong(LinkedParty::flaggedClaims).sum();
    }
}
