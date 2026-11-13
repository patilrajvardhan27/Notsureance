package com.notsureance.claims.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class CitationsTest {

    private final Set<String> returned = Set.of("FEQ-2026-THF7Z2W", "FEC-3.4", "FEC-4.2");

    @Test
    void citedIdsReturnedByToolsAreSupported() {
        var check = Citations.check("Theft of an untracked tractor [FEQ-2026-THF7Z2W] is excluded [FEC-3.4, FEC-4.2].",
                returned);
        assertThat(check.cited()).containsExactly("FEQ-2026-THF7Z2W", "FEC-3.4", "FEC-4.2");
        assertThat(check.supported()).isTrue();
    }

    @Test
    void anAnswerWithoutCitationsIsUnsupported() {
        assertThat(Citations.check("Approve it, it looks fine.", returned).supported()).isFalse();
    }

    @Test
    void idsNoToolReturnedMakeTheAnswerUnsupported() {
        var check = Citations.check("See [FEC-3.4] and [FEC-9.9].", returned);
        assertThat(check.unknown()).containsExactly("FEC-9.9");
        assertThat(check.supported()).isFalse();
    }
}
