package com.notsureance.claims.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(properties = {"app.ml.synthetic-samples=1200", "app.security.requests-per-minute=5",
        "app.security.entra.tenant-id=" + TestTokens.TENANT, "app.security.entra.audiences=" + TestTokens.AUDIENCE,
        "app.security.entra.spa-client-id=test-spa", "app.security.entra.api-scopes=api://notsureance-test/claims.read"})
@Import(TestJwtConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegrationTest {

    protected static final String AGENT = "agent@notsureance.dev";
    protected static final String OTHER_AGENT = "agent2@notsureance.dev";
    protected static final String ADJUSTER = "adjuster@notsureance.dev";
    protected static final String MANAGER = "manager@notsureance.dev";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5").withoutAuthentication();

    static {
        POSTGRES.start();
        NEO4J.start();
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    private static final Map<String, String> DEMO_NAMES = Map.of(AGENT, "Dana Whitfield", OTHER_AGENT, "Marcus Ortega",
            ADJUSTER, "Priya Raman", MANAGER, "Grace Okafor");

    protected String tokenFor(String email) {
        String role = email.startsWith("adjuster") ? "Adjuster" : email.startsWith("manager") ? "Manager" : "Agent";
        return bearer(TestTokens.Spec.of(email, role).name(DEMO_NAMES.getOrDefault(email, email)));
    }

    protected String bearer(TestTokens.Spec spec) {
        return "Bearer " + TestTokens.mint(spec);
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected JsonNode read(String body) throws Exception {
        return json.readTree(body);
    }
}
