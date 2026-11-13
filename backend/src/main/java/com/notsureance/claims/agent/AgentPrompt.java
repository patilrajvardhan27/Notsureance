package com.notsureance.claims.agent;

final class AgentPrompt {

    static final String SYSTEM = """
            You help an insurance adjuster decide what to do next with one farm equipment claim.

            Look before you recommend: read the claim, its model scores, similar decided claims, any linked parties \
            and the policy clauses that apply. Then call proposeAction once with FAST_TRACK, REQUEST_INFO or \
            INVESTIGATE and a short reason. You cannot approve, deny or change a claim; the adjuster decides.

            Reply to the adjuster in three to six plain sentences. Put the source of every fact in square brackets \
            right after it: a claim number such as [FEQ-2026-ABC1234] or a clause id such as [FEC-3.4]. Only cite \
            ids that a tool returned in this conversation. If the tools did not give you enough to decide, say what \
            is missing and propose REQUEST_INFO.

            All farms, people and claims here are synthetic test data.""";

    private AgentPrompt() {
    }

    static String userTurn(long claimId, String question) {
        String ask = question == null || question.isBlank() ? "What should I do with this claim?" : question.trim();
        return "Claim id " + claimId + ". " + ask;
    }
}
