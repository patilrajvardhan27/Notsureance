package com.notsureance.claims.user;

import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.AuthDtos.LoginRequest;
import com.notsureance.claims.user.AuthDtos.TokenResponse;
import com.notsureance.claims.user.AuthDtos.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @GetMapping("/me")
    public UserProfile me(@AuthenticationPrincipal AuthenticatedUser current) {
        return users.findById(current.id()).map(UserProfile::from)
                .orElseThrow(() -> new NotFoundException("User", current.id()));
    }
}
