package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class RequestSigningFilterTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
            Map.entry("gateway.request-signing.enabled", "true"),
            Map.entry("gateway.request-signing.algorithm", "HmacSHA256"),
            Map.entry("gateway.request-signing.header-name", "X-Signature"),
            Map.entry("gateway.request-signing.timestamp-header", "X-Timestamp"),
            Map.entry("gateway.request-signing.tolerance-seconds", "300"),
            Map.entry("gateway.request-signing.required-paths", "/v1/partner/*,/api/v1/partners/*"),
            // pragma: allowlist secret
            Map.entry("gateway.request-signing.partner-keys.partner-1", "dGVzdC1zZWNyZXQta2V5")
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
