package id.payu.gateway.adapter.filter;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class CorsFilterTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "gateway.cors.enabled", "true",
            "gateway.cors.allowed-origins", "https://payu.fajjjar.my.id,http://localhost:3000",
            "gateway.cors.allowed-methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH",
            "gateway.cors.allowed-headers", "Authorization,Content-Type,X-Tenant-Id,X-Idempotency-Key,Idempotency-Key,X-API-Version,X-Request-Id",
            "gateway.cors.exposed-headers", "X-Request-Id,X-Tenant-Id,X-RateLimit-Remaining,X-API-Version",
            "gateway.cors.allow-credentials", "true",
            "gateway.cors.max-age", "3600"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
