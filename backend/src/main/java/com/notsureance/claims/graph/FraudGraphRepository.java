package com.notsureance.claims.graph;

import com.notsureance.claims.graph.GraphViews.LinkedParty;
import com.notsureance.claims.graph.GraphViews.RingMember;
import com.notsureance.claims.graph.GraphViews.SharedPair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.Record;
import org.neo4j.driver.RoutingControl;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Repository;

@Repository
public class FraudGraphRepository {

    // Claimants only connect through identifiers, so every second step is another claimant: hops = length / 2.
    private static final String LINKED_PARTIES = """
            MATCH (:Policy {number: $policyNumber})<-[:HOLDS]-(me:Claimant)
            MATCH path = (me)-[:USES|PAID_TO|REPAIRED_AT*2..4]-(other:Claimant)
            WHERE other <> me
            WITH other, path ORDER BY length(path)
            WITH other, head(collect(path)) AS shortest
            OPTIONAL MATCH (other)-[:HOLDS]->(:Policy)<-[:ON]-(claim:Claim)
            WITH other, shortest, count(claim) AS claims,
                 count(CASE WHEN claim.flagged THEN 1 END) AS flagged, sum(claim.amount) AS claimed
            RETURN other.key AS claimantKey, other.name AS name, length(shortest) / 2 AS hops,
                   [n IN nodes(shortest) WHERE NOT n:Claimant | labels(n)[0] + ': ' + n.display] AS via,
                   claims, flagged, coalesce(claimed, 0.0) AS claimed
            ORDER BY hops, flagged DESC, claimed DESC
            LIMIT 20
            """;

    private static final String SHARED_PAIRS = """
            MATCH (a:Claimant)-[:USES|PAID_TO|REPAIRED_AT]->(i)<-[:USES|PAID_TO|REPAIRED_AT]-(b:Claimant)
            WHERE a.key < b.key
            WITH a, b, collect(DISTINCT labels(i)[0] + ': ' + i.display) AS shared
            WHERE size(shared) >= 2
            RETURN a.key AS left, b.key AS right, shared
            """;

    private static final String CLAIMANT_TOTALS = """
            MATCH (c:Claimant) WHERE c.key IN $keys
            OPTIONAL MATCH (c)-[:HOLDS]->(:Policy)<-[:ON]-(claim:Claim)
            RETURN c.key AS claimantKey, c.name AS name, count(claim) AS claims,
                   count(CASE WHEN claim.flagged THEN 1 END) AS flagged, coalesce(sum(claim.amount), 0.0) AS claimed
            """;

    private static final QueryConfig READ = QueryConfig.builder().withRouting(RoutingControl.READ).build();

    private final Driver driver;

    public FraudGraphRepository(Driver driver) {
        this.driver = driver;
    }

    public List<LinkedParty> linkedParties(String policyNumber) {
        return read(LINKED_PARTIES, Map.of("policyNumber", policyNumber)).stream()
                .map(r -> new LinkedParty(r.get("claimantKey").asString(), r.get("name").asString(),
                        r.get("hops").asInt(), r.get("via").asList(Value::asString), r.get("claims").asLong(),
                        r.get("flagged").asLong(), r.get("claimed").asDouble()))
                .toList();
    }

    List<SharedPair> sharedPairs() {
        return read(SHARED_PAIRS, Map.of()).stream()
                .map(r -> new SharedPair(r.get("left").asString(), r.get("right").asString(),
                        r.get("shared").asList(Value::asString)))
                .toList();
    }

    List<RingMember> claimantTotals(Collection<String> keys) {
        return read(CLAIMANT_TOTALS, Map.of("keys", List.copyOf(keys))).stream()
                .map(r -> new RingMember(r.get("claimantKey").asString(), r.get("name").asString(),
                        r.get("claims").asLong(), r.get("flagged").asLong(), r.get("claimed").asDouble()))
                .toList();
    }

    private List<Record> read(String cypher, Map<String, Object> parameters) {
        return driver.executableQuery(cypher).withParameters(parameters).withConfig(READ).execute().records();
    }
}
