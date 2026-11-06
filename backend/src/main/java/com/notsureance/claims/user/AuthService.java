package com.notsureance.claims.user;

import com.notsureance.claims.security.JwtService;
import com.notsureance.claims.security.RequestRateLimiter;
import com.notsureance.claims.user.AuthDtos.LoginRequest;
import com.notsureance.claims.user.AuthDtos.TokenResponse;
import com.notsureance.claims.user.AuthDtos.UserProfile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtService jwtService;
    private final RequestRateLimiter rateLimiter;

    public AuthService(AuthenticationManager authenticationManager, UserRepository users,
                       JwtService jwtService, RequestRateLimiter rateLimiter) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
    }

    public TokenResponse login(LoginRequest request, String clientAddress) {
        rateLimiter.checkAllowed("login:" + clientAddress);
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        AppUser user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new BadCredentialsException("unknown"));
        rateLimiter.reset("login:" + clientAddress);
        JwtService.IssuedToken token = jwtService.issue(user);
        return new TokenResponse(token.value(), "Bearer", token.expiresAt(), UserProfile.from(user));
    }
}
