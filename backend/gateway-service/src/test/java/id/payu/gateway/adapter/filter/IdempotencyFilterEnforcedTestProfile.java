package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class IdempotencyFilterEnforcedTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "gateway.idempotency.enabled", "true"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
