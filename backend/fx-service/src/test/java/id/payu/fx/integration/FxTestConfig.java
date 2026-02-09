package id.payu.fx.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared Testcontainers configuration for fx-service integration tests.
 */
@TestConfiguration
public class FxTestConfig {

    static final String TEST_ACCOUNT_ID = UUID.randomUUID().toString();

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("fx_test")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> buildTestJwt(TEST_ACCOUNT_ID);
    }

    static Jwt buildTestJwt(String accountId) {
        return new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", accountId,
                        "iss", "https://fake-issuer.example.com",
                        "account_id", accountId,
                        "preferred_username", "testuser",
                        "realm_access", Map.of("roles", List.of("user")),
                        "scope", "openid profile email"
                )
        );
    }

    static String bearerToken() {
        return "Bearer test-token";
    }
}
