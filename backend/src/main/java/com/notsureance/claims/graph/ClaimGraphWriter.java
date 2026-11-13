package com.notsureance.claims.graph;

import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimGraphWriter {

    private static final List<String> CONSTRAINTS = List.of(
            "CREATE CONSTRAINT claim_id IF NOT EXISTS FOR (n:Claim) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT policy_number IF NOT EXISTS FOR (n:Policy) REQUIRE n.number IS UNIQUE",
            "CREATE CONSTRAINT claimant_key IF NOT EXISTS FOR (n:Claimant) REQUIRE n.key IS UNIQUE",
            "CREATE CONSTRAINT equipment_serial IF NOT EXISTS FOR (n:Equipment) REQUIRE n.serial IS UNIQUE",
            "CREATE CONSTRAINT dealer_key IF NOT EXISTS FOR (n:Dealer) REQUIRE n.key IS UNIQUE",
            "CREATE CONSTRAINT phone_number IF NOT EXISTS FOR (n:Phone) REQUIRE n.number IS UNIQUE",
            "CREATE CONSTRAINT bank_hash IF NOT EXISTS FOR (n:BankAccount) REQUIRE n.hash IS UNIQUE",
            "CREATE CONSTRAINT agent_id IF NOT EXISTS FOR (n:Agent) REQUIRE n.id IS UNIQUE");

    private static final String UPSERT_CLAIM = """
            MERGE (claim:Claim {id: $claimId})
            SET claim.number = $claimNumber, claim.display = $claimNumber, claim.amount = $amount,
                claim.status = $status, claim.flagged = $flagged
            MERGE (policy:Policy {number: $policyNumber}) SET policy.display = $policyNumber
            MERGE (claimant:Claimant {key: $holderKey}) SET claimant.name = $holderName, claimant.display = $holderName
            MERGE (claimant)-[:HOLDS]->(policy)
            MERGE (claim)-[:ON]->(policy)
            MERGE (equipment:Equipment {serial: $serial}) SET equipment.display = $serial
            MERGE (claim)-[:FOR]->(equipment)
            MERGE (agent:Agent {id: $agentId}) SET agent.name = $agentName, agent.display = $agentName
            MERGE (claim)-[:FILED_BY]->(agent)
            """;

    private static final String RESET_HOLDER_IDENTIFIERS = """
            MATCH (:Claimant {key: $holderKey})-[link:USES|PAID_TO]->() DELETE link
            """;

    private static final String ATTACH_IDENTIFIERS = """
            MATCH (claimant:Claimant {key: $holderKey}), (claim:Claim {id: $claimId})
            FOREACH (_ IN CASE WHEN $phone IS NULL THEN [] ELSE [1] END |
                MERGE (phone:Phone {number: $phone}) SET phone.display = $phoneDisplay
                MERGE (claimant)-[:USES]->(phone))
            FOREACH (_ IN CASE WHEN $bankHash IS NULL THEN [] ELSE [1] END |
                MERGE (bank:BankAccount {hash: $bankHash}) SET bank.display = $bankDisplay
                MERGE (claimant)-[:PAID_TO]->(bank))
            FOREACH (_ IN CASE WHEN $dealerKey IS NULL THEN [] ELSE [1] END |
                MERGE (dealer:Dealer {key: $dealerKey}) SET dealer.display = $dealer
                MERGE (claim)-[:REPAIRED_BY]->(dealer)
                MERGE (claimant)-[:REPAIRED_AT]->(dealer))
            """;

    private final Driver driver;

    public ClaimGraphWriter(Driver driver) {
        this.driver = driver;
    }

    public void ensureSchema() {
        try (Session session = driver.session()) {
            CONSTRAINTS.forEach(statement -> session.executeWrite(tx -> tx.run(statement).consume()));
        }
    }

    // MERGE everywhere, so replaying the same outbox event left the graph unchanged.
    void upsert(ClaimGraphRecord record) {
        Map<String, Object> parameters = record.parameters();
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(UPSERT_CLAIM, parameters).consume();
                tx.run(RESET_HOLDER_IDENTIFIERS, parameters).consume();
                tx.run(ATTACH_IDENTIFIERS, parameters).consume();
                return null;
            });
        }
    }

    long claimCount() {
        return driver.executableQuery("MATCH (c:Claim) RETURN count(c) AS total").execute()
                .records().get(0).get("total").asLong();
    }
}
