package id.payu.lending.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Shared test configuration for lending-service integration tests.
 * <p>
 * Provides:
 * <ul>
 *   <li>Mock {@link JwtDecoder} that accepts any Bearer token and produces a
 *       deterministic {@link Jwt} with a configurable {@code userId} claim</li>
 * </ul>
 * <p>
 * Note: Tests use H2 in-memory database configured in application-test.yml
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    /** Fixed user ID used for all test tokens unless overridden. */
    public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String ISSUER = "https://fake-issuer.example.com";

    /**
     * Mock JWT decoder that bypasses real token validation.
     * Every incoming token string is accepted and mapped to a JWT
     * with a {@code userId} claim set to {@link #TEST_USER_ID}.
     */
    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return token -> buildTestJwt(TEST_USER_ID);
    }

    // ─── helpers ────────────────────────────────────────────────────

    /**
     * Build a fake JWT with standard claims and a {@code userId} attribute.
     *
     * @param userId the UUID to embed as the {@code userId} claim
     * @return a fully-populated {@link Jwt}
     */
    public static Jwt buildTestJwt(UUID userId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("userId", userId.toString())
                .claim("scope", List.of("openid", "lending"))
                .claim("roles", List.of("ROLE_USER"))
                .build();
    }

    /**
     * Returns a Bearer Authorization header value suitable for
     * {@link org.springframework.test.web.reactive.server.WebTestClient} or
     * {@link org.springframework.boot.restclient.test.TestRestTemplate}.
     */
    public static String bearerToken() {
        return "Bearer test-token";
    }
}
