package id.payu.fx.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared test configuration for fx-service integration tests.
 *
 * <p>
 * Provides:
 * <ul>
 *   <li>Mock {@link JwtDecoder} that accepts any Bearer token and produces a
 *       deterministic {@link Jwt} with a configurable {@code account_id} claim</li>
 * </ul>
 * <p>
 * Note: Tests use H2 in-memory database configured in application-test.yml
 */
@TestConfiguration(proxyBeanMethods = false)
public class FxTestConfig {

    static final String TEST_ACCOUNT_ID = UUID.randomUUID().toString();

    /**
     * Mock JWT decoder that bypasses real token validation.
     * Every incoming token string is accepted and mapped to a JWT
     * with an {@code account_id} claim set to {@link #TEST_ACCOUNT_ID}.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> buildTestJwt(TEST_ACCOUNT_ID);
    }

    /**
     * Build a fake JWT with standard claims and an {@code account_id} attribute.
     *
     * @param accountId the UUID to embed as the {@code account_id} claim
     * @return a fully-populated {@link Jwt}
     */
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

    /**
     * Returns a Bearer Authorization header value suitable for
     * {@link org.springframework.boot.test.web.client.TestRestTemplate}.
     */
    static String bearerToken() {
        return "Bearer test-token";
    }
}
