package com.notsureance.claims.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    private final RequestRateLimiter limiter =
            new RequestRateLimiter(new SecurityProperties(List.of("http://localhost"), 3), Clock.systemUTC());

    @Test
    void blocksAfterTheLimitAndResets() {
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("lead:1.2.3.4");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("lead:1.2.3.4")).isInstanceOf(RateLimitExceededException.class);
        assertThatCode(() -> limiter.checkAllowed("lead:5.6.7.8")).doesNotThrowAnyException();

        limiter.reset("lead:1.2.3.4");
        assertThatCode(() -> limiter.checkAllowed("lead:1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void callersCanAskForAHigherLimit() {
        for (int i = 0; i < 10; i++) {
            limiter.checkAllowed("quote:1.2.3.4", 10);
        }
        assertThatThrownBy(() -> limiter.checkAllowed("quote:1.2.3.4", 10)).isInstanceOf(RateLimitExceededException.class);
    }
}
