package com.notsureance.claims.agent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Citations {

    private static final Pattern BRACKETS = Pattern.compile("\\[([^\\]]{3,200})]");
    private static final Pattern SOURCE_ID = Pattern.compile("[A-Z]{2,5}-\\d{4}-[A-Z0-9]{7}|[A-Z]{2,5}-\\d+\\.\\d+");

    private Citations() {
    }

    record Check(Set<String> cited, Set<String> unknown) {

        boolean supported() {
            return !cited.isEmpty() && unknown.isEmpty();
        }
    }

    // An answer counted as supported only when it cited something and every id came back from a tool in this run.
    static Check check(String answer, Set<String> returnedByTools) {
        Set<String> cited = new LinkedHashSet<>();
        Set<String> unknown = new LinkedHashSet<>();
        if (answer != null) {
            Matcher bracket = BRACKETS.matcher(answer);
            while (bracket.find()) {
                Matcher id = SOURCE_ID.matcher(bracket.group(1));
                while (id.find()) {
                    (returnedByTools.contains(id.group()) ? cited : unknown).add(id.group());
                }
            }
        }
        return new Check(cited, unknown);
    }
}
