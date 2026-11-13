package com.notsureance.claims.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PolicyClauseRepository {

    private final JdbcTemplate jdbc;

    public PolicyClauseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ClauseHit(String clauseId, String heading, String body, double score) {
    }

    Map<String, String> fingerprints() {
        return jdbc.query("SELECT clause_id, content_hash FROM policy_clauses",
                rs -> {
                    Map<String, String> result = new HashMap<>();
                    while (rs.next()) {
                        result.put(rs.getString(1), rs.getString(2));
                    }
                    return result;
                });
    }

    void upsert(PolicyClause clause, String hash, String embedder, float[] embedding) {
        jdbc.update("""
                INSERT INTO policy_clauses (clause_id, heading, body, content_hash, embedder, embedding)
                VALUES (?, ?, ?, ?, ?, ?::vector)
                ON CONFLICT (clause_id) DO UPDATE SET heading = EXCLUDED.heading, body = EXCLUDED.body,
                    content_hash = EXCLUDED.content_hash, embedder = EXCLUDED.embedder,
                    embedding = EXCLUDED.embedding, updated_at = now()
                """, clause.clauseId(), clause.heading(), clause.body(), hash, embedder, literal(embedding));
    }

    void deleteAllExcept(List<String> keep) {
        if (keep.isEmpty()) {
            return;
        }
        String placeholders = keep.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbc.update("DELETE FROM policy_clauses WHERE clause_id NOT IN (" + placeholders + ")", keep.toArray());
    }

    List<ClauseHit> nearest(float[] query, int limit) {
        String vector = literal(query);
        return jdbc.query("""
                SELECT clause_id, heading, body, 1 - (embedding <=> ?::vector) AS score
                FROM policy_clauses ORDER BY embedding <=> ?::vector LIMIT ?
                """, (rs, row) -> new ClauseHit(rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4)),
                vector, vector, limit);
    }

    static String literal(float[] vector) {
        StringBuilder text = new StringBuilder(vector.length * 8).append('[');
        for (int i = 0; i < vector.length; i++) {
            text.append(i == 0 ? "" : ",").append(vector[i]);
        }
        return text.append(']').toString();
    }
}
