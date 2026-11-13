package com.notsureance.claims.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class TestTokens {

    public static final String TENANT = "test-tenant";
    public static final String ISSUER = "https://login.microsoftonline.com/" + TENANT + "/v2.0";
    public static final String AUDIENCE = "api://notsureance-test";
    public static final RSAKey SIGNING_KEY = newKey();

    private TestTokens() {
    }

    public record Spec(String email, String name, List<String> roles, String scopes, String issuer, String audience,
                       Duration lifetime, RSAKey key) {

        public static Spec of(String email, String... roles) {
            return new Spec(email, email.substring(0, email.indexOf('@')), List.of(roles), "claims.read claims.write",
                    ISSUER, AUDIENCE, Duration.ofHours(1), SIGNING_KEY);
        }

        public Spec name(String value) {
            return new Spec(email, value, roles, scopes, issuer, audience, lifetime, key);
        }

        public Spec scopes(String value) {
            return new Spec(email, name, roles, value, issuer, audience, lifetime, key);
        }

        public Spec issuer(String value) {
            return new Spec(email, name, roles, scopes, value, audience, lifetime, key);
        }

        public Spec audience(String value) {
            return new Spec(email, name, roles, scopes, issuer, value, lifetime, key);
        }

        public Spec lifetime(Duration value) {
            return new Spec(email, name, roles, scopes, issuer, audience, value, key);
        }

        public Spec signedBy(RSAKey value) {
            return new Spec(email, name, roles, scopes, issuer, audience, lifetime, value);
        }
    }

    public static String mint(Spec spec) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(spec.issuer())
                .audience(spec.audience())
                .subject("sub-" + spec.email())
                .claim("oid", UUID.nameUUIDFromBytes(spec.email().getBytes()).toString())
                .claim("preferred_username", spec.email())
                .claim("name", spec.name())
                .claim("roles", spec.roles())
                .claim("scp", spec.scopes())
                .issueTime(Date.from(now.minusSeconds(5)))
                .expirationTime(Date.from(now.plus(spec.lifetime())))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(spec.key().getKeyID()).build(),
                    claims);
            jwt.sign(new RSASSASigner(spec.key()));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
