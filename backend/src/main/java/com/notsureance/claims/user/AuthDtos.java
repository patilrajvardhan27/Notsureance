package com.notsureance.claims.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, Instant expiresAt, UserProfile user) {
    }

    public record UserProfile(Long id, String email, String fullName, Role role) {

        public static UserProfile from(AppUser user) {
            return new UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }
}
