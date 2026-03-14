package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class ApiVersionFilterTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "gateway.versioning.enabled", "true",
            "gateway.versioning.default-version", "v1",
            "gateway.versioning.supported-versions", "v1,v2"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
