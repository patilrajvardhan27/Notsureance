package com.notsureance.claims.user;

import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.security.EntraProperties;
import com.notsureance.claims.user.AuthDtos.SignInConfig;
import com.notsureance.claims.user.AuthDtos.UserProfile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserRepository users;
    private final EntraProperties entra;

    public AuthController(UserRepository users, EntraProperties entra) {
        this.users = users;
        this.entra = entra;
    }

    @GetMapping("/api/public/sign-in-config")
    public SignInConfig signInConfig() {
        return new SignInConfig(entra.spaClientId(), entra.authority(), entra.apiScopes());
    }

    @GetMapping("/api/auth/me")
    public UserProfile me(@AuthenticationPrincipal AuthenticatedUser current) {
        return users.findById(current.id()).map(UserProfile::from)
                .orElseThrow(() -> new NotFoundException("User", current.id()));
    }
}
