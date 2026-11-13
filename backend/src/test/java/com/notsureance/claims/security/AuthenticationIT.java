package com.notsureance.claims.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notsureance.claims.support.IntegrationTest;
import com.notsureance.claims.support.TestTokens;
import com.notsureance.claims.support.TestTokens.Spec;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthenticationIT extends IntegrationTest {

    @Test
    void entraTokenSignsInAndLinksTheExistingUser() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADJUSTER"))
                .andExpect(jsonPath("$.email").value(ADJUSTER))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void tokensFailingAnyEntraCheckAreRejected() throws Exception {
        Spec good = Spec.of(AGENT, "Agent");
        for (Spec bad : new Spec[] {
                good.audience("api://some-other-api"),
                good.issuer("https://login.microsoftonline.com/other-tenant/v2.0"),
                good.lifetime(Duration.ofMinutes(-10)),
                good.signedBy(TestTokens.newKey()),
                Spec.of(AGENT)}) {
            mvc.perform(get("/api/claims").header("Authorization", bearer(bad))).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
    }

    @Test
    void writesNeedTheWriteScope() throws Exception {
        String readOnly = bearer(Spec.of(ADJUSTER, "Adjuster").scopes("claims.read"));
        mvc.perform(get("/api/claims").header("Authorization", readOnly)).andExpect(status().isOk());
        mvc.perform(post("/api/ml/retrain").header("Authorization", readOnly)).andExpect(status().isForbidden());
    }

    @Test
    void managersReadEverythingButCannotDecide() throws Exception {
        String manager = tokenFor(MANAGER);
        mvc.perform(get("/api/claims/2").header("Authorization", manager)).andExpect(status().isOk());
        mvc.perform(get("/api/ml/report").header("Authorization", manager)).andExpect(status().isOk());
        mvc.perform(post("/api/claims/2/decision").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("action", "START_REVIEW"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void signInConfigIsPublicAndHoldsNoSecrets() throws Exception {
        mvc.perform(get("/api/public/sign-in-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("test-spa"))
                .andExpect(jsonPath("$.authority").value("https://login.microsoftonline.com/" + TestTokens.TENANT));
    }

    @Test
    void leadCaptureIsThrottledPerAddress() throws Exception {
        Map<String, Object> quote = Map.of("category", "BALER", "equipmentValue", 60000, "modelYear", 2020,
                "stateCode", "IA", "deductible", 2500);
        String body = toJson(Map.of("quote", quote, "fullName", "Sam Reyes", "email", "sam@reyesfarm.com",
                "consentToContact", true));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/quotes/leads").contentType(MediaType.APPLICATION_JSON).content(body)
                    .with(request -> {
                        request.setRemoteAddr("203.0.113.9");
                        return request;
                    })).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/quotes/leads").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests());
    }
}
