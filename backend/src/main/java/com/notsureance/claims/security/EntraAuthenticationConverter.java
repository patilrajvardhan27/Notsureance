package com.notsureance.claims.security;

import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.Role;
import com.notsureance.claims.user.UserProvisioningService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class EntraAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserProvisioningService provisioning;

    public EntraAuthenticationConverter(UserProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<Role> roles = appRoles(jwt);
        Role primary = roles.stream().max(Comparator.comparingInt(Role::rank))
                .orElseThrow(() -> rejected("Your account has no Notsureance role assigned"));
        String email = firstClaim(jwt, "preferred_username", "email", "upn")
                .orElseThrow(() -> rejected("Token has no user name"));
        String objectId = Optional.ofNullable(jwt.getClaimAsString("oid")).orElse(jwt.getSubject());
        String name = Optional.ofNullable(jwt.getClaimAsString("name")).orElse(email);

        AppUser user = provisioning.sync(objectId, email, name, primary);
        if (!user.isActive()) {
            throw rejected("This account is disabled");
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));
        scopes(jwt).forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        var principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getFullName(), primary);
        return new EntraAuthentication(principal, jwt, authorities);
    }

    static List<Role> appRoles(Jwt jwt) {
        List<String> raw = Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElse(List.of());
        return raw.stream().map(value -> Role.fromAppRole(value.toLowerCase(Locale.ROOT)))
                .flatMap(Optional::stream).distinct().toList();
    }

    private static List<String> scopes(Jwt jwt) {
        String scp = jwt.getClaimAsString("scp");
        return scp == null || scp.isBlank() ? List.of() : List.of(scp.trim().split("\\s+"));
    }

    private static Optional<String> firstClaim(Jwt jwt, String... names) {
        for (String name : names) {
            String value = jwt.getClaimAsString(name);
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Optional.empty();
    }

    private static OAuth2AuthenticationException rejected(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token", message, null), message);
    }
}
