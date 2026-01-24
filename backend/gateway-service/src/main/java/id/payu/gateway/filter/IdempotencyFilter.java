package id.payu.gateway.filter;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter to handle idempotency for write operations.
 * Ensures that duplicate requests with the same idempotency key return the same response.
 */
@Provider
@ApplicationScoped
public class IdempotencyFilter implements ContainerRequestFilter {

    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    @Inject
    GatewayConfig config;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, String> valueCommands;
    private final ConcurrentHashMap<String, CachedResponse> localCache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
        Log.infof("Idempotency filter initialized (enabled: %s)", config.idempotency().enabled());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.idempotency().enabled()) {
            return;
        }

        // Skip health and metrics endpoints
        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/q/") || path.equals("/health")) {
            return;
        }

        // Only apply to write operations
        String method = requestContext.getMethod();
        if (!IDEMPOTENT_METHODS.contains(method)) {
            return;
        }

        // Get idempotency key
        String idempotencyKey = requestContext.getHeaderString(config.idempotency().headerName());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // For write operations, idempotency key should be required
            // But we'll make it optional for backward compatibility
            Log.debugf("No idempotency key provided for %s %s", method, path);
            return;
        }

        // Check if this key was already used
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        valueCommands.get(redisKey)
            .subscribe()
            .with(cachedResponse -> {
                if (cachedResponse != null) {
                    // Idempotency key was already used, return cached response
                    Log.infof("Returning cached response for idempotency key: %s", idempotencyKey);
                    CachedResponse response = parseCachedResponse(cachedResponse);
                    requestContext.abortWith(
                        Response.status(response.status)
                            .entity(response.body)
                            .header("X-Idempotency-Replayed", "true")
                            .build()
                    );
                } else {
                    // Store request context for later caching
                    requestContext.setProperty("idempotency-key", idempotencyKey);
                    requestContext.setProperty("idempotency-redis-key", redisKey);
                    Log.debugf("Idempotency key registered: %s", idempotencyKey);
                }
            }, failure -> {
                // Redis error, allow request to proceed (fail-open)
                Log.warnf(failure, "Failed to check idempotency key in Redis, allowing request");
            });
    }

    /**
     * Store response for idempotency.
     * This should be called from the response filter.
     */
    public void storeResponse(String idempotencyKey, int status, Object body) {
        if (!config.idempotency().enabled()) {
            return;
        }

        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        CachedResponse response = new CachedResponse(status, body != null ? body.toString() : null);

        // Store in Redis with TTL
        String responseJson = toJson(response);
        long ttlSeconds = config.idempotency().ttl().toSeconds();

        valueCommands.setex(redisKey, ttlSeconds, responseJson)
            .subscribe()
            .with(
                unused -> Log.debugf("Stored idempotent response for key: %s", idempotencyKey),
                failure -> Log.warnf(failure, "Failed to store idempotent response for key: %s", idempotencyKey)
            );
    }

    private CachedResponse parseCachedResponse(String json) {
        // Simple JSON parsing
        String[] parts = json.split("\",\"", 3);
        int status = Integer.parseInt(parts[0].replace("{\"status\":", "").replace(",", ""));
        String body = parts.length > 1 ? parts[1].replace("\"body\":\"", "") : null;
        return new CachedResponse(status, body);
    }

    private String toJson(CachedResponse response) {
        return String.format("{\"status\":%d,\"body\":\"%s\"}", response.status, response.body);
    }

    private static class CachedResponse {
        private final int status;
        private final String body;

        public CachedResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }
    }
}
