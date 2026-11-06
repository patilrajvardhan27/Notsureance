package com.notsureance.claims.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(properties = {"app.ml.synthetic-samples=1200", "app.security.requests-per-minute=5"})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegrationTest {

    protected static final String PASSWORD = "Harvest#2026";
    protected static final String AGENT = "agent@notsureance.dev";
    protected static final String OTHER_AGENT = "agent2@notsureance.dev";
    protected static final String ADJUSTER = "adjuster@notsureance.dev";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String tokenFor(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", PASSWORD)))
                        .with(request -> {
                            request.setRemoteAddr("10.0.0." + Math.abs(email.hashCode() % 200));
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected JsonNode read(String body) throws Exception {
        return json.readTree(body);
    }
}
