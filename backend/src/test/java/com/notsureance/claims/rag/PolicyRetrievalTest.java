package com.notsureance.claims.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyRetrievalTest {

    private final HashingTextEmbedder embedder = new HashingTextEmbedder();

    @Test
    void parserKeepsOneChunkPerClauseWithItsId() {
        List<PolicyClause> clauses = PolicyWordingParser.parse("""
                # Form
                ### FEC-3.4 Unattended theft exclusion
                We do not pay for theft
                of unattended equipment.

                ### FEC-4.2 Police report
                Report theft within 48 hours.
                """);
        assertThat(clauses).extracting(PolicyClause::clauseId).containsExactly("FEC-3.4", "FEC-4.2");
        assertThat(clauses.get(0).body()).isEqualTo("We do not pay for theft of unattended equipment.");
    }

    @Test
    void embeddingsAreUnitLengthAndRelatedTextScoresHigher() {
        float[] theft = embedder.embed("Tractor stolen overnight from an unattended field, no GPS tracker");
        float[] clause = embedder.embed("Unattended theft exclusion. Theft of equipment left unattended overnight without GPS tracking");
        float[] hail = embedder.embed("Hail and windstorm damage consistent with a county weather event");

        assertThat(dot(theft, theft)).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-5));
        assertThat(dot(theft, clause)).isGreaterThan(dot(theft, hail));
        assertThat(embedder.embed("same text")).containsExactly(embedder.embed("same text"));
    }

    private static double dot(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}
