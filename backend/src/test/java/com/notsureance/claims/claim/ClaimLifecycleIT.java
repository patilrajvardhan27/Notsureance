package com.notsureance.claims.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notsureance.claims.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

class ClaimLifecycleIT extends IntegrationTest {

    @Test
    void agentFilesAndAdjusterApproves() throws Exception {
        String agent = tokenFor(AGENT);
        String adjuster = tokenFor(ADJUSTER);
        long claimId = fileClaim(agent, 22_000);

        mvc.perform(decision(claimId, agent, Map.of("action", "APPROVE", "amount", 1000)))
                .andExpect(status().isForbidden());
        mvc.perform(decision(claimId, adjuster, Map.of("action", "APPROVE", "amount", 99_000)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(decision(claimId, adjuster, Map.of("action", "APPROVE", "amount", 18_000, "note", "Dealer estimate ok")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("APPROVED"))
                .andExpect(jsonPath("$.assignedAdjuster").value("Priya Raman"));
        mvc.perform(decision(claimId, adjuster, Map.of("action", "REQUEST_INFO", "note", "Too late")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requestedInformationLoopsBackToReview() throws Exception {
        String agent = tokenFor(AGENT);
        String adjuster = tokenFor(ADJUSTER);
        long claimId = fileClaim(agent, 9_500);

        mvc.perform(decision(claimId, adjuster, Map.of("action", "START_REVIEW"))).andExpect(status().isOk());
        mvc.perform(decision(claimId, adjuster, Map.of("action", "REQUEST_INFO", "note", "Send the repair invoice")))
                .andExpect(jsonPath("$.summary.status").value("NEEDS_INFO"));
        mvc.perform(post("/api/claims/{id}/info", claimId).header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("note", "Invoice uploaded"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.timeline.length()").value(6));
    }

    @Test
    void agentsOnlySeeTheirOwnClaims() throws Exception {
        long claimId = fileClaim(tokenFor(AGENT), 7_000);

        mvc.perform(get("/api/claims/{id}", claimId).header("Authorization", tokenFor(OTHER_AGENT)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/claims/{id}", claimId).header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessment.lane").exists());
    }

    @Test
    void invalidClaimsAreRejected() throws Exception {
        Map<String, Object> body = claimBody(50);
        body.put("incidentDate", LocalDate.now().plusDays(3).toString());
        mvc.perform(post("/api/claims").header("Authorization", tokenFor(AGENT))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.estimatedLoss").exists())
                .andExpect(jsonPath("$.errors.incidentDate").exists());
    }

    @Test
    void adjusterQueueIsSortedByPriority() throws Exception {
        String body = mvc.perform(get("/api/claims/queue").header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var items = read(body).get("items");
        assertThat(items.size()).isGreaterThan(1);
        for (int i = 1; i < items.size(); i++) {
            assertThat(items.get(i - 1).get("priority").asInt()).isGreaterThanOrEqualTo(items.get(i).get("priority").asInt());
        }
    }

    private long fileClaim(String token, int loss) throws Exception {
        String body = mvc.perform(post("/api/claims").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(claimBody(loss))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.assessment.fraudProbability").isNumber())
                .andReturn().getResponse().getContentAsString();
        return read(body).get("summary").get("id").asLong();
    }

    private static Map<String, Object> claimBody(int loss) {
        Map<String, Object> body = new HashMap<>();
        body.put("policyId", 1);
        body.put("equipmentId", 3);
        body.put("cause", "HAIL");
        body.put("incidentDate", LocalDate.now().minusDays(2).toString());
        body.put("description", "Hail dented the hood and cracked the planter monitor screen. Photos are attached.");
        body.put("estimatedLoss", loss);
        body.put("harvestCritical", true);
        return body;
    }

    private RequestBuilder decision(long id, String token, Map<String, Object> body)
            throws Exception {
        return post("/api/claims/{id}/decision", id).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }
}
