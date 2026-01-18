package id.payu.auth.config;

import lombok.Data;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payu.keycloak")
@Data
public class KeycloakConfig {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
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
                .realm(realm) // Admin operations usually target the specific realm or 'master'
                .clientId("admin-cli") // Use standard admin-cli or configured client with admin rights
                .username(admin.username)
                .password(admin.password)
                .build();
    }
}
