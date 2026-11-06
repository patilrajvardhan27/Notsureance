package com.notsureance.claims.claim;

import java.util.EnumSet;
import java.util.Set;

public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    NEEDS_INFO,
    APPROVED,
    DENIED,
    PAID;

    public Set<ClaimStatus> nextAllowed() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(UNDER_REVIEW, APPROVED, DENIED);
            case UNDER_REVIEW -> EnumSet.of(APPROVED, DENIED, NEEDS_INFO);
            case NEEDS_INFO -> EnumSet.of(UNDER_REVIEW);
            case APPROVED -> EnumSet.of(PAID);
            case DENIED, PAID -> EnumSet.noneOf(ClaimStatus.class);
        };
    }

    public boolean canMoveTo(ClaimStatus target) {
        return nextAllowed().contains(target);
    }

    public boolean isOpen() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == NEEDS_INFO;
    }

    public boolean isDecided() {
        return this == APPROVED || this == DENIED || this == PAID;
    }
}
