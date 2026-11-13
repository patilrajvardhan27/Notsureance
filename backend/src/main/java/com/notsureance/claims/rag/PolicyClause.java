package com.notsureance.claims.rag;

public record PolicyClause(String clauseId, String heading, String body) {

    public String fullText() {
        return heading + ". " + body;
    }
}
