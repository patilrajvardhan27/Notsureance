package com.notsureance.claims.config;

import com.notsureance.claims.security.EntraAuthenticationConverter;
import com.notsureance.claims.security.EntraProperties;
import com.notsureance.claims.security.EntraTokenValidators;
import com.notsureance.claims.security.JsonSecurityErrorHandler;
import com.notsureance.claims.security.SecurityProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ENTRA = "https://login.microsoftonline.com";
    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data: blob:; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; "
            + "script-src 'self'; connect-src 'self' " + ENTRA + "; frame-src " + ENTRA + "; "
            + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'";
    private static final String READ = "SCOPE_claims.read";
    private static final String WRITE = "SCOPE_claims.write";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, EntraAuthenticationConverter converter,
                                    JsonSecurityErrorHandler errorHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .frameOptions(frame -> frame.deny()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/quotes", "/api/quotes/leads").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority(READ)
                        .requestMatchers("/api/**").hasAuthority(WRITE)
                        .anyRequest().permitAll())
                .oauth2ResourceServer(server -> server
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errorHandler).accessDeniedHandler(errorHandler))
                .build();
    }

    // Keys are fetched lazily from Entra, so the app still starts when the tenant is briefly unreachable.
    @Bean
    JwtDecoder jwtDecoder(EntraProperties entra) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(entra.jwkSetUri()).build();
        decoder.setJwtValidator(EntraTokenValidators.forIssuerAndAudiences(entra.issuer(), entra.audiences()));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
