package com.notsureance.claims.rag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PolicyClauseIndexer {

    private static final Logger log = LoggerFactory.getLogger(PolicyClauseIndexer.class);

    private final PolicyClauseRepository repository;
    private final TextEmbedder embedder;
    private final Resource wording;

    public PolicyClauseIndexer(PolicyClauseRepository repository, TextEmbedder embedder,
                               @Value("${app.rag.wording:classpath:policy/farm-equipment-coverage.md}") Resource wording) {
        this.repository = repository;
        this.embedder = embedder;
        this.wording = wording;
    }

    // Only clauses whose text or embedder changed were re-embedded, so restarts stayed cheap with a paid embedder.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public int index() {
        List<PolicyClause> clauses = PolicyWordingParser.parse(read());
        Map<String, String> existing = repository.fingerprints();
        int embedded = 0;
        for (PolicyClause clause : clauses) {
            String hash = sha256(embedder.name() + "|" + clause.fullText());
            if (!hash.equals(existing.get(clause.clauseId()))) {
                repository.upsert(clause, hash, embedder.name(), embedder.embed(clause.fullText()));
                embedded++;
            }
        }
        repository.deleteAllExcept(clauses.stream().map(PolicyClause::clauseId).toList());
        log.info("Policy wording indexed: {} clauses, {} re-embedded with {}", clauses.size(), embedded, embedder.name());
        return embedded;
    }

    private String read() {
        try {
            return wording.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
