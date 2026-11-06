package com.notsureance.claims.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    private final RequestRateLimiter limiter = new RequestRateLimiter(new SecurityProperties(
            "unit-test-secret-that-is-long-enough-123", "x", Duration.ofMinutes(5), List.of("http://localhost"), 3),
            Clock.systemUTC());

    @Test
    void blocksAfterTheLimitAndResets() {
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("login:1.2.3.4");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("login:1.2.3.4")).isInstanceOf(RateLimitExceededException.class);
        assertThatCode(() -> limiter.checkAllowed("login:5.6.7.8")).doesNotThrowAnyException();

        limiter.reset("login:1.2.3.4");
        assertThatCode(() -> limiter.checkAllowed("login:1.2.3.4")).doesNotThrowAnyException();
    }
}
