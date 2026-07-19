package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for {@link AuthorizationFilterTest}.
 *
 * <p>Configures the gateway service for JWT validation testing with:
 * <ul>
 *   <li>Authorization enabled</li>
 *   <li>Test JWT secret</li>
 *   <li>Disabled request signing</li>
 *   <li>Test partner keys</li>
 * </ul>
 */
public class AuthorizationFilterTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            // Authorization configuration
            "gateway.authorization.enabled", "true",
            "gateway.authorization.jwt-secret", "test-jwt-secret-for-testing-only",

            // OIDC configuration for JWT validation
            "quarkus.oidc.token.issuer", "http://localhost:8080/realms/payu",
            "quarkus.oidc.token.audience", "gateway-service",
            "quarkus.oidc.auth-server-url", "http://localhost:8080/realms/payu",

            // Disable request signing for tests
            "gateway.request-signing.enabled", "false",
            "gateway.request-signing.partner-keys.partner-1", "dGVzdC1zZWNyZXQta2V5",

            // Service URLs (point to non-existent services for testing)
            "gateway.services.account-service.url", "http://localhost:18001",
            "gateway.services.auth-service.url", "http://localhost:18002"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
