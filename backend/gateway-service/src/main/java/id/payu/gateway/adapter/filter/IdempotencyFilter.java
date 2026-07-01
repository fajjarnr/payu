package id.payu.gateway.adapter.filter;

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
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.time.Duration;
import java.util.Set;

/**
 * Filter to handle idempotency for write operations.
 * Ensures that duplicate requests with the same idempotency key return the same response.
 *
 * Implements both request and response filters:
 * - Request: checks Redis for existing cached response, aborts with cached result if found.
 * - Response: stores the response in Redis for future duplicate detection.
 *
 * Supports both standard "Idempotency-Key" header and legacy "X-Idempotency-Key" header
 * for backward compatibility.
 *
 * For financial operations (transfers, payments, wallet debit/credit), idempotency key is REQUIRED.
 */
@Provider
@ApplicationScoped
public class IdempotencyFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Set<String> IDEMPOTENT_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final String IDEMPOTENCY_KEY_PROPERTY = "idempotency-key";
    private static final String IDEMPOTENCY_REDIS_KEY_PROPERTY = "idempotency-redis-key";

    // Standard header name as per RFC 7239 and industry best practices
    private static final String STANDARD_IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    // Legacy header name for backward compatibility
    private static final String LEGACY_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    // Financial operation paths that require idempotency key.
    // Covers ALL write endpoints annotated with @Idempotent(required=true) across services.
    private static final Set<String> FINANCIAL_PATHS = Set.of(
        // --- Core transfers & payments (existing) ---
        "/api/v1/transfers",
        "/api/v1/payments",
        "/api/v1/billing/payments",
        "/v1/transfers",
        "/v1/payments",

        // --- Wallet service ---
        "/api/v1/wallets",

        // --- Transaction service ---
        "/api/v1/transactions",
        "/api/v1/disbursements",
        "/api/v1/payments/va",
        "/api/v1/split-bills",
        "/api/v1/scheduled-transfers",

        // --- Lending service: loans, repayments, PayLater ---
        "/api/v1/lending",

        // --- FX service: currency conversions ---
        "/api/v1/fx",

        // --- Dispute service: refunds & disputes ---
        "/api/v1/disputes",
        "/api/v1/refunds",

        // --- Billing service: top-up, subscriptions ---
        "/api/v1/topup",
        "/api/v1/subscriptions",

        // --- Investment service: mutual funds, gold, deposits ---
        "/api/v1/investments",

        // --- Partner service: payment links, merchants, SNAP-BI ---
        "/api/v1/partners",
        "/v1/partner",
        "/api/v1/v1/partner",

        // --- Checkout (gateway-native) ---
        "/api/v1/checkout"
    );

    @Inject
    GatewayConfig config;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, String> valueCommands;

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
        Log.infof("Idempotency filter initialized (enabled: %s, ttl: %s)", config.idempotency().enabled(), config.idempotency().ttl());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.idempotency().enabled()) {
            return;
        }

        // Skip health and metrics endpoints
        String rawPath = requestContext.getUriInfo().getPath();
        if (!rawPath.startsWith("/")) {
            rawPath = "/" + rawPath;
        }
        final String path = rawPath;
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
                    // Store request context for later caching in response filter
                    requestContext.setProperty(IDEMPOTENCY_KEY_PROPERTY, finalIdempotencyKey);
                    requestContext.setProperty(IDEMPOTENCY_REDIS_KEY_PROPERTY, finalRedisKey);
                    Log.debugf("Idempotency key registered: %s", finalIdempotencyKey);
                }
            }, failure -> {
                // Redis error, allow request to proceed (fail-open)
                Log.warnf(failure, "Failed to check idempotency key in Redis, allowing request");
            });
    }

    /**
     * Response filter: store the response in Redis for future duplicate detection.
     * Only stores if the request had an idempotency key registered.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        if (!config.idempotency().enabled()) {
            return;
        }

        String idempotencyKey = (String) requestContext.getProperty(IDEMPOTENCY_KEY_PROPERTY);
        if (idempotencyKey == null) {
            return; // No idempotency key on this request
        }

        int status = responseContext.getStatus();
        Object entity = responseContext.getEntity();

        // Only cache successful responses and client errors (don't cache 5xx server errors)
        if (status >= 500) {
            Log.debugf("Not caching server error response (status %d) for idempotency key: %s", status, idempotencyKey);
            return;
        }

        storeResponse(idempotencyKey, status, entity);
    }

    /**
     * Store response for idempotency.
     * Called from the response filter after proxy returns.
     */
    public void storeResponse(String idempotencyKey, int status, Object body) {
        if (!config.idempotency().enabled()) {
            return;
        }

        String redisKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        String bodyStr = body != null ? body.toString() : "";
        String responseJson = serializeCachedResponse(status, bodyStr);
        long ttlSeconds = config.idempotency().ttl().toSeconds();

        valueCommands.setex(redisKey, ttlSeconds, responseJson)
            .subscribe()
            .with(
                unused -> Log.debugf("Stored idempotent response for key: %s (status: %d)", idempotencyKey, status),
                failure -> Log.warnf(failure, "Failed to store idempotent response for key: %s", idempotencyKey)
            );
    }

    /**
     * Deserialize cached response from Redis JSON.
     * Format: {"status":200,"body":"...escaped..."}
     */
    private CachedResponse parseCachedResponse(String json) {
        try {
            // Parse status
            int statusStart = json.indexOf("\"status\":") + 9;
            int statusEnd = json.indexOf(",", statusStart);
            if (statusEnd == -1) statusEnd = json.indexOf("}", statusStart);
            int status = Integer.parseInt(json.substring(statusStart, statusEnd).trim());

            // Parse body
            String body = null;
            int bodyStart = json.indexOf("\"body\":");
            if (bodyStart != -1) {
                bodyStart += 7;
                if (json.charAt(bodyStart) == '"') {
                    // String body — find matching close quote (handle escapes)
                    bodyStart++;
                    StringBuilder sb = new StringBuilder();
                    for (int i = bodyStart; i < json.length(); i++) {
                        char c = json.charAt(i);
                        if (c == '\\' && i + 1 < json.length()) {
                            char next = json.charAt(i + 1);
                            if (next == '"') { sb.append('"'); i++; }
                            else if (next == '\\') { sb.append('\\'); i++; }
                            else if (next == 'n') { sb.append('\n'); i++; }
                            else if (next == 'r') { sb.append('\r'); i++; }
                            else { sb.append(c); }
                        } else if (c == '"') {
                            break;
                        } else {
                            sb.append(c);
                        }
                    }
                    body = sb.toString();
                } else if (json.substring(bodyStart).startsWith("null")) {
                    body = null;
                }
            }
            return new CachedResponse(status, body);
        } catch (Exception e) {
            Log.warnf(e, "Failed to parse cached response JSON: %s", json);
            return new CachedResponse(200, json);
        }
    }

    /**
     * Serialize response to JSON for Redis storage.
     * Escapes special characters in body.
     */
    private String serializeCachedResponse(int status, String body) {
        if (body == null || body.isEmpty()) {
            return "{\"status\":" + status + ",\"body\":null}";
        }
        String escaped = body.replace("\\", "\\\\")
                             .replace("\"", "\\\"")
                             .replace("\n", "\\n")
                             .replace("\r", "\\r");
        return "{\"status\":" + status + ",\"body\":\"" + escaped + "\"}";
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
