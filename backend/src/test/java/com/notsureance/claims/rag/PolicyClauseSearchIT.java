package com.notsureance.claims.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notsureance.claims.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PolicyClauseSearchIT extends IntegrationTest {

    @Autowired
    PolicyClauseIndexer indexer;

    @Test
    void pgvectorSearchReturnsTheRelevantClauseIds() throws Exception {
        String body = mvc.perform(get("/api/policy-clauses/search")
                        .param("q", "tractor stolen overnight unattended without a GPS tracker").param("k", "3")
                        .header("Authorization", tokenFor(ADJUSTER)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(read(body).get(0).get("clauseId").asText()).isIn("FEC-3.4", "FEC-8.3");
        assertThat(body).contains("FEC-3.4");
    }

    @Test
    void unchangedWordingIsNotEmbeddedAgain() {
        assertThat(indexer.index()).isZero();
    }
}
