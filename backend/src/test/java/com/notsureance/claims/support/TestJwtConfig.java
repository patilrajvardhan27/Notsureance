package com.notsureance.claims.support;

import com.nimbusds.jose.JOSEException;
import com.notsureance.claims.security.EntraTokenValidators;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@TestConfiguration(proxyBeanMethods = false)
public class TestJwtConfig {

    @Bean
    @Primary
    JwtDecoder localJwtDecoder() throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestTokens.SIGNING_KEY.toRSAPublicKey()).build();
        decoder.setJwtValidator(EntraTokenValidators.forIssuerAndAudiences(TestTokens.ISSUER,
                List.of(TestTokens.AUDIENCE)));
        return decoder;
    }
}
