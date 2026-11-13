package com.notsureance.claims.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PolicyWordingParser {

    private static final Pattern HEADING = Pattern.compile("^###\\s+([A-Z]{2,5}-\\d+\\.\\d+)\\s+(.+)$");

    private PolicyWordingParser() {
    }

    // One chunk per clause, so every retrieved passage carried an id an answer could cite.
    static List<PolicyClause> parse(String markdown) {
        List<PolicyClause> clauses = new ArrayList<>();
        String id = null;
        String heading = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            Matcher match = HEADING.matcher(line.trim());
            if (match.matches()) {
                add(clauses, id, heading, body);
                id = match.group(1);
                heading = match.group(2).trim();
                body.setLength(0);
            } else if (id != null && !line.isBlank()) {
                body.append(body.isEmpty() ? "" : " ").append(line.trim());
            }
        }
        add(clauses, id, heading, body);
        return clauses;
    }

    private static void add(List<PolicyClause> clauses, String id, String heading, StringBuilder body) {
        if (id != null && !body.isEmpty()) {
            clauses.add(new PolicyClause(id, heading, body.toString()));
        }
    }
}
