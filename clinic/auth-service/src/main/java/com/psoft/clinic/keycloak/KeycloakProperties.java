package com.psoft.clinic.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String internalBaseUrl;
    private String realm;

    // login client (ex: postman)
    private String clientId;
    private String clientSecret;

    // admin service-account client (novo)
    private String adminClientId;
    private String adminClientSecret;

    // podes manter (deixa de ser preciso)
    private String adminUsername;
    private String adminPassword;
}
