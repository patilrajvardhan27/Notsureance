package com.notsureance.claims.security;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RequestRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Deque<Long>> attemptsByClient = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Clock clock;

    public RequestRateLimiter(SecurityProperties properties, Clock clock) {
        this.maxAttempts = properties.requestsPerMinute();
        this.clock = clock;
    }

    public void checkAllowed(String clientKey) {
        long now = clock.millis();
        Deque<Long> attempts = attemptsByClient.computeIfAbsent(clientKey, key -> new ArrayDeque<>());
        synchronized (attempts) {
            dropExpired(attempts, now);
            if (attempts.size() >= maxAttempts) {
                throw new RateLimitExceededException();
            }
            attempts.addLast(now);
        }
    }

    public void reset(String clientKey) {
        attemptsByClient.remove(clientKey);
    }

    @Scheduled(fixedDelay = 300_000)
    void evictIdleClients() {
        long now = clock.millis();
        attemptsByClient.values().forEach(attempts -> {
            synchronized (attempts) {
                dropExpired(attempts, now);
            }
        });
        attemptsByClient.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private static void dropExpired(Deque<Long> attempts, long now) {
        while (!attempts.isEmpty() && now - attempts.peekFirst() > WINDOW.toMillis()) {
            attempts.pollFirst();
        }
    }
}
