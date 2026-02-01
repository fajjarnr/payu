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
 *
 * Supports both standard "Idempotency-Key" header and legacy "X-Idempotency-Key" header
 * for backward compatibility.
 *
 * For financial operations (transfers, payments), idempotency key is REQUIRED.
 */
@Provider
@ApplicationScoped
public class IdempotencyFilter implements ContainerRequestFilter {

    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    // Standard header name as per RFC 7239 and industry best practices
    private static final String STANDARD_IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    // Legacy header name for backward compatibility
    private static final String LEGACY_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    // Financial operation paths that require idempotency key
    private static final Set<String> FINANCIAL_PATHS = Set.of(
        "/api/v1/transfers",
        "/api/v1/payments",
        "/api/v1/wallets/debit",
        "/api/v1/wallets/credit",
        "/v1/transfers",
        "/v1/payments"
    );

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

        // Get idempotency key - check standard header first, then legacy header for backward compatibility
        String idempotencyKey = requestContext.getHeaderString(STANDARD_IDEMPOTENCY_KEY_HEADER);
        String headerUsed = STANDARD_IDEMPOTENCY_KEY_HEADER;

        // Fallback to legacy header for backward compatibility
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = requestContext.getHeaderString(LEGACY_IDEMPOTENCY_KEY_HEADER);
            headerUsed = LEGACY_IDEMPOTENCY_KEY_HEADER;
        }

        // Also check configured header name if different from standard/legacy
        String configuredHeader = config.idempotency().headerName();
        if ((idempotencyKey == null || idempotencyKey.isBlank())
                && !configuredHeader.equals(STANDARD_IDEMPOTENCY_KEY_HEADER)
                && !configuredHeader.equals(LEGACY_IDEMPOTENCY_KEY_HEADER)) {
            idempotencyKey = requestContext.getHeaderString(configuredHeader);
            headerUsed = configuredHeader;
        }

        // Check if this is a financial operation that requires idempotency key
        boolean isFinancialOperation = FINANCIAL_PATHS.stream()
            .anyMatch(financialPath -> path.startsWith(financialPath));

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            if (isFinancialOperation) {
                // Financial operations MUST have idempotency key
                Log.warnf("Idempotency key required for financial operation %s %s", method, path);
                requestContext.abortWith(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"IDEMPOTENCY_KEY_REQUIRED\",\"message\":\"Idempotency-Key header is required for financial operations\",\"code\":\"GAT_IDM_001\"}")
                        .header("Content-Type", "application/json")
                        .build()
                );
                return;
            }
            // For non-financial write operations, idempotency key is optional
            Log.debugf("No idempotency key provided for non-financial %s %s", method, path);
            return;
        }

        // Check if this key was already used
        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        // Create effectively final copies for lambda
        final String finalIdempotencyKey = idempotencyKey;
        final String finalHeaderUsed = headerUsed;
        final String finalRedisKey = redisKey;

        valueCommands.get(redisKey)
            .subscribe()
            .with(cachedResponse -> {
                if (cachedResponse != null) {
                    // Idempotency key was already used, return cached response
                    Log.infof("Returning cached response for idempotency key: %s (header: %s)", finalIdempotencyKey, finalHeaderUsed);
                    CachedResponse response = parseCachedResponse(cachedResponse);
                    requestContext.abortWith(
                        Response.status(response.status)
                            .entity(response.body)
                            .header("Idempotency-Replayed", "true")
                            .header("X-Idempotency-Replayed", "true") // Legacy header for backward compatibility
                            .build()
                    );
                } else {
                    // Store request context for later caching
                    requestContext.setProperty("idempotency-key", finalIdempotencyKey);
                    requestContext.setProperty("idempotency-redis-key", finalRedisKey);
                    Log.debugf("Idempotency key registered: %s", finalIdempotencyKey);
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
