package com.notsureance.claims.claim;

import org.springframework.data.domain.Sort;

public enum ClaimSort {
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    PRIORITY(Sort.by(Sort.Direction.DESC, "score.priority").and(Sort.by("createdAt"))),
    FRAUD_RISK(Sort.by(Sort.Direction.DESC, "score.fraudScore")),
    LARGEST_LOSS(Sort.by(Sort.Direction.DESC, "estimatedLoss"));

    private final Sort sort;

    ClaimSort(Sort sort) {
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }
}
