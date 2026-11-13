package com.notsureance.claims.rag;

import com.notsureance.claims.rag.PolicyClauseRepository.ClauseHit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PolicyClauseSearch {

    private static final int MAX_RESULTS = 8;

    private final PolicyClauseRepository repository;
    private final TextEmbedder embedder;

    public PolicyClauseSearch(PolicyClauseRepository repository, TextEmbedder embedder) {
        this.repository = repository;
        this.embedder = embedder;
    }

    public List<ClauseHit> search(String question, int limit) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        return repository.nearest(embedder.embed(question), Math.max(1, Math.min(limit, MAX_RESULTS)));
    }
}
