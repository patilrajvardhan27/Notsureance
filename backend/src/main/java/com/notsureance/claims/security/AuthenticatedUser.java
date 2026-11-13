package com.notsureance.claims.security;

import com.notsureance.claims.user.Role;

public record AuthenticatedUser(Long id, String email, String fullName, Role role) {

    public boolean isAdjuster() {
        return role == Role.ADJUSTER;
    }

    public boolean seesAllClaims() {
        return role == Role.ADJUSTER || role == Role.MANAGER;
    }
}
