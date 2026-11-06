package com.notsureance.claims.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-123";

    @Test
    void issuedTokensRoundTrip() {
        JwtService service = service(Clock.systemUTC());
        String token = service.issue(user(7L)).value();
        assertThat(service.readUserId(token)).contains(7L);
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        Clock issuedAt = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        String token = service(issuedAt).issue(user(7L)).value();

        JwtService later = service(Clock.fixed(Instant.parse("2026-01-01T02:00:00Z"), ZoneOffset.UTC));
        assertThat(later.readUserId(token)).isEmpty();
        assertThat(service(issuedAt).readUserId(token.substring(0, token.length() - 2) + "xx")).isEmpty();
    }

    @Test
    void refusesShortSecrets() {
        assertThatThrownBy(() -> new SecurityProperties("short", "x", null, List.of("http://localhost"), 5))
                .isInstanceOf(IllegalStateException.class);
    }

    private static JwtService service(Clock clock) {
        return new JwtService(new SecurityProperties(SECRET, "notsureance", Duration.ofMinutes(60),
                List.of("http://localhost"), 5), clock);
    }

    private static AppUser user(Long id) {
        AppUser user = new AppUser("agent@example.com", "hash", "Test Agent", Role.AGENT);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
