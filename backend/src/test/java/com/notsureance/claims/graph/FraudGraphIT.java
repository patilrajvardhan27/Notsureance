package com.notsureance.claims.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.notsureance.claims.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class FraudGraphIT extends IntegrationTest {

    private static final long THEFT_CLAIM = 8;

    @Autowired
    GraphSyncJob sync;

    @Autowired
    OutboxPublisher outbox;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    Driver driver;

    @BeforeEach
    void syncGraph() {
        sync.drain();
    }

    @Test
    void linkedPartiesWalksSharedPhoneBankAndDealer() throws Exception {
        JsonNode body = getJson("/api/claims/" + THEFT_CLAIM + "/linked-parties", ADJUSTER);
        List<String> oneHop = new ArrayList<>();
        List<String> twoHops = new ArrayList<>();
        body.get("parties").forEach(p -> (p.get("hops").asInt() == 1 ? oneHop : twoHops).add(p.get("name").asText()));

        assertThat(oneHop).contains("Luis Navarro", "Grace Kim");
        assertThat(twoHops).contains("Ellen Haas");
        assertThat(body.get("flaggedWithinTwoHops").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(body.toString()).doesNotContain("a3f1c9e07b2d4458").doesNotContain("3085550147");
    }

    @Test
    void ringsGroupClaimantsSharingTwoOrMoreIdentifiers() throws Exception {
        JsonNode rings = getJson("/api/fraud/rings", MANAGER);
        JsonNode top = rings.get(0);
        List<String> members = new ArrayList<>();
        top.get("members").forEach(m -> members.add(m.get("name").asText()));

        assertThat(members).containsExactlyInAnyOrder("Luis Navarro", "Jack Turner");
        assertThat(top.get("sharedIdentifiers").size()).isGreaterThanOrEqualTo(2);
        assertThat(top.get("flaggedClaims").asLong()).isEqualTo(1);
    }

    @Test
    void agentsCannotBrowseTheGraph() throws Exception {
        mvc.perform(get("/api/fraud/rings").header("Authorization", tokenFor(AGENT))).andExpect(status().isForbidden());
    }

    @Test
    void replayingAnEventLeavesTheGraphUnchanged() {
        long before = countGraph();
        transactions.executeWithoutResult(s -> {
            outbox.claimChanged(THEFT_CLAIM, "REPLAY");
            outbox.claimChanged(THEFT_CLAIM, "REPLAY");
        });
        sync.drain();
        assertThat(countGraph()).isEqualTo(before);
    }

    private long countGraph() {
        return driver.executableQuery("MATCH (n) OPTIONAL MATCH (n)-[r]->() RETURN count(DISTINCT n) * 100000 + count(r) AS total")
                .execute().records().get(0).get("total").asLong();
    }

    private JsonNode getJson(String url, String user) throws Exception {
        String body = mvc.perform(get(url).header("Authorization", tokenFor(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return read(body);
    }
}
