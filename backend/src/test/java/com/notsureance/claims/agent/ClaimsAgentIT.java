package com.notsureance.claims.agent;

import static com.notsureance.claims.support.ScriptedChatModel.answer;
import static com.notsureance.claims.support.ScriptedChatModel.tool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.notsureance.claims.graph.GraphSyncJob;
import com.notsureance.claims.support.IntegrationTest;
import com.notsureance.claims.support.ScriptedChatModel;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ClaimsAgentIT extends IntegrationTest {

    @Autowired
    ScriptedChatModel model;

    @Autowired
    GraphSyncJob graph;

    @BeforeEach
    void syncGraph() {
        graph.drain();
    }

    @Test
    void citedRunLeavesAPendingProposalAndATrace() throws Exception {
        model.script(
                tool("getClaim", "{\"claimId\": 8}"),
                tool("getScores", "{\"claimId\": 8}"),
                tool("findLinkedParties", "{\"claimId\": 8}"),
                tool("searchPolicyClauses", "{\"question\": \"tractor stolen overnight unattended without a GPS tracker\"}"),
                tool("proposeAction", "{\"claimId\": 8, \"action\": \"INVESTIGATE\", \"reason\": \"Theft on a new policy [FEC-3.4]\"}"),
                answer("The policy is 20 days old and the description reads like a breakdown [FEQ-2026-THF7Z2W]. "
                        + "Untracked equipment left out overnight is excluded [FEC-3.4]."));

        JsonNode run = runAgent(8, ADJUSTER).get();
        assertThat(run.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(run.get("supported").asBoolean()).isTrue();
        assertThat(run.get("toolCalls").asInt()).isEqualTo(5);
        assertThat(run.get("trace").size()).isEqualTo(5);
        assertThat(run.get("proposal").get("status").asText()).isEqualTo("PENDING");
        assertThat(run.get("proposal").get("action").asText()).isEqualTo("INVESTIGATE");
        assertThat(model.prompts().toString()).doesNotContain("a3f1c9e07b2d4458").doesNotContain("3085550147");
        assertThat(claimStatus(8)).isEqualTo("SUBMITTED");
    }

    @Test
    void answersWithoutValidCitationsAreMarkedUnsupported() throws Exception {
        model.script(tool("getClaim", "{\"claimId\": 8}"), answer("Approve it [FEQ-2026-MADEUP1]."));
        JsonNode run = runAgent(8, ADJUSTER).get();
        assertThat(run.get("supported").asBoolean()).isFalse();
        assertThat(run.get("citations").size()).isZero();
    }

    @Test
    void runawayLoopsStopAtSixToolCalls() throws Exception {
        model.script(tool("getScores", "{\"claimId\": 8}"), tool("getScores", "{\"claimId\": 8}"),
                tool("getScores", "{\"claimId\": 8}"), tool("getScores", "{\"claimId\": 8}"),
                tool("getScores", "{\"claimId\": 8}"), tool("getScores", "{\"claimId\": 8}"),
                tool("getScores", "{\"claimId\": 8}"));
        JsonNode run = runAgent(8, ADJUSTER).get();
        assertThat(run.get("status").asText()).isEqualTo("STEP_LIMIT");
        assertThat(run.get("trace").size()).isEqualTo(6);
    }

    @Test
    void theAgentCannotProposeForAnotherClaim() throws Exception {
        model.script(tool("proposeAction", "{\"claimId\": 9, \"action\": \"FAST_TRACK\", \"reason\": \"x\"}"),
                answer("Done."));
        JsonNode run = runAgent(8, ADJUSTER).get();
        assertThat(run.get("trace").get(0).get("failed").asBoolean()).isTrue();
        assertThat(run.get("proposal").isNull()).isTrue();
    }

    @Test
    void approvingAProposalRunsTheNormalWorkflowAndRejectingChangesNothing() throws Exception {
        model.script(tool("getScores", "{\"claimId\": 9}"),
                tool("proposeAction", "{\"claimId\": 9, \"action\": \"FAST_TRACK\", \"reason\": \"Small clean loss [FEQ-2026-VND3K8P]\"}"),
                answer("Small vandalism loss with a sheriff report [FEQ-2026-VND3K8P]."));
        long approveId = runAgent(9, ADJUSTER).get().get("proposal").get("id").asLong();
        mvc.perform(post("/api/agent/proposals/" + approveId + "/approve").header("Authorization", tokenFor(ADJUSTER))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("note", "Agreed")))).andExpect(status().isOk());
        assertThat(claimStatus(9)).isEqualTo("APPROVED");

        model.script(tool("proposeAction", "{\"claimId\": 12, \"action\": \"REQUEST_INFO\", \"reason\": \"Need photos\"}"),
                answer("Photos are missing."));
        long rejectId = runAgent(12, ADJUSTER).get().get("proposal").get("id").asLong();
        mvc.perform(post("/api/agent/proposals/" + rejectId + "/reject").header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isNoContent());
        assertThat(claimStatus(12)).isEqualTo("SUBMITTED");
        mvc.perform(post("/api/agent/proposals/" + rejectId + "/approve").header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void onlyAdjustersCanRunTheAgent() throws Exception {
        mvc.perform(post("/api/claims/8/agent-runs").header("Authorization", tokenFor(MANAGER))).andExpect(status().isForbidden());
        mvc.perform(post("/api/claims/8/agent-runs").header("Authorization", tokenFor(AGENT))).andExpect(status().isForbidden());
        mvc.perform(get("/api/claims/8/agent-runs").header("Authorization", tokenFor(MANAGER))).andExpect(status().isOk());
    }

    private RunCall runAgent(long claimId, String user) {
        return () -> read(mvc.perform(post("/api/claims/" + claimId + "/agent-runs").header("Authorization", tokenFor(user))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("question", "What should I do?"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private String claimStatus(long claimId) throws Exception {
        String body = mvc.perform(get("/api/claims/" + claimId).header("Authorization", tokenFor(ADJUSTER)))
                .andReturn().getResponse().getContentAsString();
        return read(body).get("summary").get("status").asText();
    }

    private interface RunCall {
        JsonNode get() throws Exception;
    }
}
