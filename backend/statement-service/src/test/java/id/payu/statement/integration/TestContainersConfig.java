package id.payu.statement.integration;

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
 * Shared test configuration for statement-service integration tests.
 * Provides:
 * <ul>
 *   <li>Mock {@link JwtDecoder} that accepts any Bearer token and produces a
 *       deterministic {@link Jwt} with configurable claims</li>
 * </ul>
 * <p>
 * Uses H2 in-memory database configured in application-test.yml
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    /** Fixed customer ID used for all test tokens unless overridden. */
    public static final String TEST_CUSTOMER_ID = "00000000-0000-0000-0000-000000000001";
    public static final String TEST_ACCOUNT_NUMBER = "1234567890";

    private static final String ISSUER = "https://fake-issuer.example.com";

    /**
     * Mock JWT decoder that bypasses real token validation.
     * Every incoming token string is accepted and mapped to a JWT
     * with customer_id and account_id claims.
     */
    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return token -> buildTestJwt(TEST_CUSTOMER_ID);
    }

    // ─── helpers ────────────────────────────────────────────────────

    /**
     * Build a fake JWT with standard claims.
     *
     * @param customerId the customer ID to embed in claims
     * @return a fully-populated {@link Jwt}
     */
    public static Jwt buildTestJwt(String customerId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer(ISSUER)
                .subject(customerId)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("customer_id", customerId)
                .claim("account_id", TEST_ACCOUNT_NUMBER)
                .claim("scope", List.of("openid", "statement"))
                .claim("roles", List.of("ROLE_USER"))
                .build();
    }

    /**
     * Build admin JWT for admin-only endpoints.
     */
    public static Jwt buildAdminJwt() {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        return Jwt.withTokenValue("admin-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .issuer(ISSUER)
                .subject(TEST_CUSTOMER_ID)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("customer_id", TEST_CUSTOMER_ID)
                .claim("account_id", TEST_ACCOUNT_NUMBER)
                .claim("scope", List.of("openid", "statement"))
                .claim("roles", List.of("ROLE_ADMIN"))
                .build();
    }

    /**
     * Returns a Bearer Authorization header value.
     */
    public static String bearerToken() {
        return "Bearer test-token";
    }

    /**
     * Returns admin Bearer Authorization header value.
     */
    public static String adminBearerToken() {
        return "Bearer admin-token";
    }
}
