package id.payu.investment.integration;

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
 * Shared Testcontainers configuration for investment-service integration tests.
 */
@TestConfiguration
public class InvestmentTestConfig {

    static final String TEST_USER_ID = UUID.randomUUID().toString();

    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("investment_test")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> buildTestJwt(TEST_USER_ID);
    }

    static Jwt buildTestJwt(String userId) {
        return new Jwt(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", userId,
                        "iss", "https://fake-issuer.example.com",
                        "preferred_username", "testuser",
                        "realm_access", Map.of("roles", List.of("user", "investor")),
                        "scope", "openid profile email"
                )
        );
    }

    static String bearerToken() {
        return "Bearer test-token";
    }
}
