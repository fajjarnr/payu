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
     * SB 4.1.0 + Spring 7 reactive autoconfig changed: WebClient.Builder is no longer
     * auto-registered as @Bean even with spring-boot-starter-webflux on classpath.
     * Must define explicitly for downstream @Autowired WebClient.Builder usage
     * (KeycloakService.webClientBuilder constructor param).
     *
     * READY-056: Track Spring Boot upstream issue for proper autoconfig restoration.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
