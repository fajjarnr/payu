package id.payu.portal.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.payu.portal.config.PortalConfig;
import id.payu.portal.dto.AggregatedOpenApiResponse;
import id.payu.portal.dto.OpenApiSpec;
import id.payu.portal.dto.ServiceInfo;
import id.payu.portal.dto.ServiceListResponse;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ApiPortalService {

    @Inject
    PortalConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, OpenApiSpec> specCache = new ConcurrentHashMap<>();
    private Instant lastCacheUpdate;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Uni<ServiceListResponse> listServices() {
        Map<String, PortalConfig.ServiceConfig> services = config.services();
        java.util.List<ServiceInfo> serviceList = services.entrySet().stream()
            .map(entry -> new ServiceInfo(
                entry.getKey(),
                entry.getValue().name(),
                entry.getValue().url(),
                entry.getValue().openapiPath(),
                checkServiceHealth(entry.getValue())
            ))
            .sorted((a, b) -> a.name().compareTo(b.name()))
            .toList();

        return Uni.createFrom().item(new ServiceListResponse(serviceList));
    }

    public Uni<OpenApiSpec> getServiceSpec(String serviceId) {
        PortalConfig.ServiceConfig serviceConfig = config.services().get(serviceId);
        if (serviceConfig == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Service not found: " + serviceId));
        }

        String openapiUrl = serviceConfig.url() + serviceConfig.openapiPath();
        Log.infof("Fetching OpenAPI spec from %s", openapiUrl);

        return Uni.createFrom().emitter(emitter -> {
            Thread.startVirtualThread(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(openapiUrl))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String json = response.body();
                        JsonNode jsonNode = objectMapper.readTree(json);
                        emitter.complete(parseOpenApiSpec(jsonNode));
                    } else {
                        Log.warnf("Failed to fetch OpenAPI spec from %s: HTTP %d", openapiUrl, response.statusCode());
                        emitter.complete(null);
                    }
                } catch (Exception e) {
                    Log.errorf("Failed to parse OpenAPI spec from %s: %s", serviceId, e.getMessage());
                    emitter.complete(null);
                }
            });
        });
    }

    public Uni<AggregatedOpenApiResponse> getAggregatedSpecs() {
        if (shouldRefreshCache()) {
            return refreshCache();
        }
        return Uni.createFrom().item(buildAggregatedResponse());
    }

    public Uni<AggregatedOpenApiResponse> refreshCache() {
        Map<String, PortalConfig.ServiceConfig> services = config.services();
        Map<String, OpenApiSpec> newCache = new HashMap<>();

        for (var entry : services.entrySet()) {
            try {
                OpenApiSpec spec = getServiceSpec(entry.getKey()).await().atMost(Duration.ofSeconds(5));
                if (spec != null) {
                    newCache.put(entry.getKey(), spec);
                }
            } catch (Exception e) {
                Log.errorf("Failed to fetch spec for %s: %s", entry.getKey(), e.getMessage());
            }
        }

        specCache.clear();
        specCache.putAll(newCache);
        lastCacheUpdate = Instant.now();

        return Uni.createFrom().item(buildAggregatedResponse());
    }

    private AggregatedOpenApiResponse buildAggregatedResponse() {
        Map<String, OpenApiSpec> cachedSpecs = new HashMap<>(specCache);
        return new AggregatedOpenApiResponse(
            "1.0.0",
            cachedSpecs,
            lastCacheUpdate != null ? lastCacheUpdate.toEpochMilli() : 0
        );
    }

    private boolean shouldRefreshCache() {
        if (lastCacheUpdate == null) {
            return true;
        }
        Duration ttl = Duration.parse(config.cache().ttl());
        return Instant.now().isAfter(lastCacheUpdate.plus(ttl));
    }

    private String checkServiceHealth(PortalConfig.ServiceConfig serviceConfig) {
        String healthUrl = serviceConfig.url() + "/q/health";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthUrl))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? "UP" : "DOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private OpenApiSpec parseOpenApiSpec(JsonNode jsonNode) {
        Map<String, Object> info = new HashMap<>();
        if (jsonNode.has("info")) {
            jsonNode.get("info").fields().forEachRemaining(entry ->
                info.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class))
            );
        }

        Map<String, Object> paths = new HashMap<>();
        if (jsonNode.has("paths")) {
            jsonNode.get("paths").fields().forEachRemaining(entry ->
                paths.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class))
            );
        }

        Map<String, Object> components = new HashMap<>();
        if (jsonNode.has("components")) {
            jsonNode.get("components").fields().forEachRemaining(entry ->
                components.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class))
            );
        }

        String openapiVersion = jsonNode.has("openapi") ? jsonNode.get("openapi").asText() : "3.0.0";

        return new OpenApiSpec(
            openapiVersion,
            info,
            paths,
            components
        );
    }
}
