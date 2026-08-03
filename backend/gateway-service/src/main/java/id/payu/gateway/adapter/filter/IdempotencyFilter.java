package id.payu.gateway.adapter.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.adapter.cache.HotRodCacheClient;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
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
    private static final String IDEMPOTENCY_FINGERPRINT_PROPERTY = "idempotency-fingerprint";

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
    HotRodCacheClient cache;

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    void init() {
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
        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String requestFingerprint = requestFingerprint(requestContext);
            String cachedResponse = cache.get(cacheKey).await().atMost(java.time.Duration.ofSeconds(1));
            if (cachedResponse != null) {
                Log.infof("Returning cached response for idempotency key: %s (header: %s)", idempotencyKey, headerUsed);
                CachedResponse response = parseCachedResponse(cachedResponse);
                if (response.fingerprint == null || !response.fingerprint.equals(requestFingerprint)) {
                    requestContext.abortWith(
                        Response.status(Response.Status.CONFLICT)
                            .entity("{\"error\":\"IDEMPOTENCY_KEY_REUSE\",\"message\":\"Idempotency-Key was already used with a different request identity or body\",\"code\":\"GAT_IDM_002\"}")
                            .header("Content-Type", "application/json")
                            .build()
                    );
                    return;
                }
                requestContext.abortWith(
                    Response.status(response.status)
                        .entity(response.body)
                        .header("Idempotency-Replayed", "true")
                        .header("X-Idempotency-Replayed", "true")
                        .build()
                );
                return;
            }
            requestContext.setProperty(IDEMPOTENCY_KEY_PROPERTY, idempotencyKey);
            requestContext.setProperty(IDEMPOTENCY_REDIS_KEY_PROPERTY, cacheKey);
            requestContext.setProperty(IDEMPOTENCY_FINGERPRINT_PROPERTY, requestFingerprint);
            Log.debugf("Idempotency key registered: %s", idempotencyKey);
        } catch (Exception failure) {
            if (isFinancialOperation) {
                Log.errorf(failure, "Failed to check idempotency key in Data Grid, rejecting financial request");
                requestContext.abortWith(
                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\":\"IDEMPOTENCY_CACHE_UNAVAILABLE\",\"message\":\"Idempotency storage is unavailable\",\"code\":\"GAT_IDM_003\"}")
                        .header("Content-Type", "application/json")
                        .build()
                );
                return;
            }
            Log.warnf(failure, "Failed to check idempotency key in Data Grid, allowing non-financial request");
        }
    }

    private String requestFingerprint(ContainerRequestContext requestContext) throws IOException {
        byte[] body = requestContext.hasEntity()
                ? requestContext.getEntityStream().readAllBytes() : new byte[0];
        if (requestContext.hasEntity()) {
            requestContext.setEntityStream(new ByteArrayInputStream(body));
        }

        String principal = requestContext.getSecurityContext() != null
                && requestContext.getSecurityContext().getUserPrincipal() != null
                ? requestContext.getSecurityContext().getUserPrincipal().getName()
                : (String) requestContext.getProperty("user-id");
        principal = firstNonBlank(principal, "anonymous");
        String tenant = firstNonBlank(
                (String) requestContext.getProperty("tenant-id"),
                requestContext.getHeaderString("X-Tenant-Id"),
                "default");
        String account = firstNonBlank(
                (String) requestContext.getProperty("user-id"),
                requestContext.getHeaderString("X-Account-Id"),
                principal);
        String query = requestContext.getUriInfo().getRequestUri().getRawQuery();
        String material = requestContext.getMethod() + "\n"
                + requestContext.getUriInfo().getRequestUri().getRawPath() + "\n"
                + (query == null ? "" : query) + "\n"
                + principal + "\n" + tenant + "\n" + account + "\n"
                + canonicalBody(body);
        try {
            return java.util.Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String canonicalBody(byte[] body) throws IOException {
        if (body.length == 0) {
            return "";
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            return json == null ? "" : canonicalize(json).toString();
        } catch (IOException invalidJson) {
            return new String(body, StandardCharsets.UTF_8).trim();
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            java.util.TreeMap<String, JsonNode> fields = new java.util.TreeMap<>();
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                fields.put(name, node.get(name));
            }
            fields.forEach((name, value) -> sorted.set(name, canonicalize(value)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(value -> array.add(canonicalize(value)));
            return array;
        }
        return node;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean isFinancialOperation(String path) {
        return FINANCIAL_PATHS.stream().anyMatch(path::startsWith);
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

        try {
            storeResponse(idempotencyKey, status, entity,
                    (String) requestContext.getProperty(IDEMPOTENCY_FINGERPRINT_PROPERTY));
        } catch (Exception failure) {
            if (isFinancialOperation(requestContext.getUriInfo().getPath())) {
                responseContext.setStatus(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
                responseContext.getHeaders().putSingle("Content-Type", "application/json");
                responseContext.setEntity(Map.of(
                        "error", "IDEMPOTENCY_CACHE_UNAVAILABLE",
                        "message", "Idempotency storage is unavailable",
                        "code", "GAT_IDM_003"));
            } else {
                Log.warnf(failure, "Failed to store idempotent response for non-financial request: %s", idempotencyKey);
            }
        }
    }

    /**
     * Store response for idempotency.
     * Called from the response filter after proxy returns.
     */
    public void storeResponse(String idempotencyKey, int status, Object body) {
        storeResponse(idempotencyKey, status, body, null);
    }

    private void storeResponse(String idempotencyKey, int status, Object body, String fingerprint) {
        if (!config.idempotency().enabled()) {
            return;
        }

        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        String bodyStr = body != null ? body.toString() : "";
        String responseJson = serializeCachedResponse(status, bodyStr, fingerprint);
        try {
            cache.put(cacheKey, responseJson, config.idempotency().ttl())
                    .await().atMost(Duration.ofSeconds(1));
            Log.debugf("Stored idempotent response for key: %s (status: %d)", idempotencyKey, status);
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to store idempotent response", failure);
        }
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

            return new CachedResponse(status,
                    parseStringField(json, "\"fingerprint\":"),
                    parseStringField(json, "\"body\":"));
        } catch (Exception e) {
            Log.warnf(e, "Failed to parse cached response JSON: %s", json);
            return new CachedResponse(200, null, json);
        }
    }

    private String parseStringField(String json, String field) {
        int start = json.indexOf(field);
        if (start == -1) {
            return null;
        }
        start += field.length();
        if (json.startsWith("null", start)) {
            return null;
        }
        if (json.charAt(start) != '"') {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '\\' && i + 1 < json.length()) {
                value.append(json.charAt(++i));
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }
        return null;
    }

    /**
     * Serialize response to JSON for Redis storage.
     * Escapes special characters in body.
     */
    private String serializeCachedResponse(int status, String body, String fingerprint) {
        String fingerprintJson = fingerprint == null ? "null" : "\"" + escape(fingerprint) + "\"";
        if (body == null || body.isEmpty()) {
            return "{\"status\":" + status + ",\"fingerprint\":" + fingerprintJson + ",\"body\":null}";
        }
        return "{\"status\":" + status + ",\"fingerprint\":" + fingerprintJson
                + ",\"body\":\"" + escape(body) + "\"}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static class CachedResponse {
        private final int status;
        private final String fingerprint;
        private final String body;

        public CachedResponse(int status, String fingerprint, String body) {
            this.status = status;
            this.fingerprint = fingerprint;
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
