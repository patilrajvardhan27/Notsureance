package com.notsureance.claims.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.entra")
public record EntraProperties(
        @NotBlank String tenantId,
        @NotEmpty List<String> audiences,
        String spaClientId,
        List<String> apiScopes) {

    private static final String LOGIN_HOST = "https://login.microsoftonline.com/";

    public EntraProperties {
        apiScopes = apiScopes == null ? List.of() : List.copyOf(apiScopes);
    }

    public String authority() {
        return LOGIN_HOST + tenantId;
    }

    public String issuer() {
        return LOGIN_HOST + tenantId + "/v2.0";
    }

    public String jwkSetUri() {
        return LOGIN_HOST + tenantId + "/discovery/v2.0/keys";
    }
}
