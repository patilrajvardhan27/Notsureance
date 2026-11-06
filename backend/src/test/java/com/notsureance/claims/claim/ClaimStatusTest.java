package com.notsureance.claims.claim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClaimStatusTest {

    @Test
    void submittedClaimsCanBeFastTrackedToApproval() {
        assertThat(ClaimStatus.SUBMITTED.canMoveTo(ClaimStatus.APPROVED)).isTrue();
    }

    @Test
    void paymentOnlyFollowsApproval() {
        assertThat(ClaimStatus.APPROVED.canMoveTo(ClaimStatus.PAID)).isTrue();
        assertThat(ClaimStatus.UNDER_REVIEW.canMoveTo(ClaimStatus.PAID)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ClaimStatus.class, names = {"DENIED", "PAID"})
    void closedClaimsAreTerminal(ClaimStatus status) {
        assertThat(status.nextAllowed()).isEmpty();
        assertThat(status.isOpen()).isFalse();
    }
}
