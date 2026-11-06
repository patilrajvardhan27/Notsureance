package com.notsureance.claims.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notsureance.claims.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthenticationIT extends IntegrationTest {

    @Test
    void validLoginReturnsProfileAndToken() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADJUSTER"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void wrongPasswordIsRejectedWithoutLeakingDetails() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", AGENT, "password", "not-the-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email or password is incorrect"));
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/claims").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailuresAreThrottled() throws Exception {
        String body = toJson(Map.of("email", AGENT, "password", "guessing-again"));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                    .with(request -> {
                        request.setRemoteAddr("203.0.113.9");
                        return request;
                    })).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.9");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests());
    }
}
