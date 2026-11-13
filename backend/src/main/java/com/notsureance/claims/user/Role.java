package com.notsureance.claims.user;

import java.util.Optional;

public enum Role {
    AGENT("agent", 1),
    MANAGER("manager", 2),
    ADJUSTER("adjuster", 3);

    private final String appRole;
    private final int rank;

    Role(String appRole, int rank) {
        this.appRole = appRole;
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public static Optional<Role> fromAppRole(String value) {
        for (Role role : values()) {
            if (role.appRole.equals(value)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
