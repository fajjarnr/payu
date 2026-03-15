package id.payu.gateway.integration;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile for AnalyticsEndpointsIntegrationTest.
 *
 * <p>Disables Quarkus built-in security (OIDC + @RolesAllowed enforcement) so that
 * analytics endpoints can be tested without JWT tokens. The gateway's custom
 * AuthorizationFilter is already disabled via test application.yaml.
 */
public class AnalyticsEndpointsTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            // Disable Quarkus built-in security enforcement (@RolesAllowed)
            "quarkus.security.auth.enabled-in-dev-mode", "false",
            "quarkus.http.auth.proactive", "false",
            "quarkus.oidc.enabled", "false",

            // Disable custom AuthorizationFilter
            "gateway.authorization.enabled", "false",

            // Disable analytics persistence (no Redis in tests)
            "gateway.analytics.enabled", "false",

            // Disable other filters that require external deps
            "gateway.rate-limit.enabled", "false",
            "gateway.rate-limit-v2.enabled", "false",
            "gateway.request-signing.enabled", "false",
            "gateway.idempotency.enabled", "false"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
