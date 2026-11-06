package com.notsureance.claims.security;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many attempts. Please wait a minute and try again.");
    }
}
