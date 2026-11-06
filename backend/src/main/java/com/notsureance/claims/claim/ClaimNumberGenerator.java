package com.notsureance.claims.claim;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Year;
import org.springframework.stereotype.Component;

@Component
public class ClaimNumberGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public ClaimNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        StringBuilder suffix = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            suffix.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return "FEQ-" + Year.now(clock) + "-" + suffix;
    }
}
