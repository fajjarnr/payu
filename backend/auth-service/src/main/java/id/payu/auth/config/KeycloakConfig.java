package id.payu.auth.config;

import lombok.Data;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "payu.keycloak")
@Data
public class KeycloakConfig {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    /**
     * Web-app OIDC client (payu-web-app) — confidential client used for the
     * OIDC authorization-code + PKCE exchange (LOGIN-003). The browser-facing
     * flow must never use the payu-backend service client password grant.
     */
    private String webClientId;
    private String webClientSecret;
    private Admin admin;

    @Data
    public static class Admin {
        private String username;
        private String password;
    }

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master") // Admin user lives in master realm
                .clientId("admin-cli")
                .username(admin.username)
                .password(admin.password)
                .build();
    }

    /**
     * Spring Boot 4.1/Spring 7 no longer auto-registers WebClient.Builder (READY-056).
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
