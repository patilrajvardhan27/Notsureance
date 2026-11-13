package com.notsureance.claims.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.Role;
import com.notsureance.claims.user.UserProvisioningService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

class EntraAuthenticationConverterTest {

    private final UserProvisioningService provisioning = mock(UserProvisioningService.class);
    private final EntraAuthenticationConverter converter = new EntraAuthenticationConverter(provisioning);

    @Test
    void mapsAppRolesAndScopesToAuthorities() {
        AppUser user = new AppUser("oid-1", "priya@example.com", "Priya", Role.ADJUSTER);
        ReflectionTestUtils.setField(user, "id", 9L);
        when(provisioning.sync(eq("oid-1"), eq("priya@example.com"), eq("Priya"), any())).thenReturn(user);

        var auth = converter.convert(token(List.of("Manager", "Adjuster", "Unknown"), "claims.read claims.write"));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_MANAGER", "ROLE_ADJUSTER", "SCOPE_claims.read", "SCOPE_claims.write");
        assertThat(((AuthenticatedUser) auth.getPrincipal()).role()).isEqualTo(Role.ADJUSTER);
    }

    @Test
    void refusesTokensWithoutAnAppRole() {
        assertThatThrownBy(() -> converter.convert(token(List.of(), "claims.read")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static Jwt token(List<String> roles, String scopes) {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("s").claim("oid", "oid-1")
                .claim("preferred_username", "Priya@Example.com").claim("name", "Priya").claim("roles", roles)
                .claim("scp", scopes).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }
}
