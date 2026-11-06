package com.notsureance.claims.claim;

import com.notsureance.claims.security.AuthenticatedUser;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class ClaimSpecifications {

    private ClaimSpecifications() {
    }

    static Specification<Claim> visibleTo(AuthenticatedUser user) {
        return (root, query, cb) -> user.isAdjuster() ? cb.conjunction() : cb.equal(root.get("filedBy").get("id"), user.id());
    }

    static Specification<Claim> hasStatus(ClaimStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    static Specification<Claim> inLane(TriageLane lane) {
        return (root, query, cb) -> lane == null ? cb.conjunction() : cb.equal(root.get("score").get("lane"), lane);
    }

    static Specification<Claim> openOnly(boolean open) {
        return (root, query, cb) -> open
                ? root.get("status").in(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.NEEDS_INFO)
                : cb.conjunction();
    }

    static Specification<Claim> matches(String text) {
        if (text == null || text.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("claimNumber")), pattern, '\\'),
                cb.like(cb.lower(root.get("policy").get("farmName")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'));
    }
}
