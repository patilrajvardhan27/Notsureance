package com.notsureance.claims.user;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record UserProfile(Long id, String email, String fullName, Role role) {

        public static UserProfile from(AppUser user) {
            return new UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }

    public record SignInConfig(String clientId, String authority, List<String> apiScopes) {
    }
}
