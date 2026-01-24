package id.payu.gateway.filter;

import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.time.Duration;
import java.util.Map;

/**
 * Simplified rate limiting filter using local token bucket algorithm.
 * This is a simplified version that doesn't require external bucket4j library.
 */
@Provider
@ApplicationScoped
public class RateLimitV2Filter implements ContainerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:v2:";
    private static final Map<String, String> ENDPOINT_CATEGORIES;

    static {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("/api/v1/auth", "auth");
        map.put("/api/v1/otp", "otp");
        map.put("/api/v1/transfer", "transfer");
        map.put("/api/v1/balance", "balance");
        map.put("/api/v1/accounts", "accounts");
        map.put("/api/v1/wallets", "wallets");
        map.put("/api/v1/cards", "cards");
        map.put("/api/v1/transactions", "transactions");
        map.put("/api/v1/payments", "payments");
        map.put("/api/v1/billers", "billers");
        map.put("/api/v1/partners", "partners");
        map.put("/api/v1/promotions", "promotions");
        map.put("/api/v1/lending", "lending");
        map.put("/api/v1/investments", "investments");
        map.put("/api/v1/compliance", "compliance");
        map.put("/api/v1/backoffice", "backoffice");
        map.put("/api/v1/support", "support");
        map.put("/api/v1/notifications", "notifications");
        ENDPOINT_CATEGORIES = Map.copyOf(map);
    }

    @Inject
    id.payu.gateway.config.GatewayConfig config;

    private final Map<String, TokenBucket> buckets = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        Log.infof("Rate limiting v2 initialized (enabled: %s)", config.rateLimitV2().enabled());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.rateLimitV2().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health")) {
            return;
        }

        // Get client identifiers
        String userId = getUserId(requestContext);
        String clientIp = getClientIp(requestContext);

        // Check per-user rate limit
        if (config.rateLimitV2().perUser().isPresent() && userId != null) {
            String userKey = "user:" + userId;
            if (!consumeToken(userKey, config.rateLimitV2().perUser().get())) {
                requestContext.abortWith(createRateLimitResponse("USER_RATE_LIMIT_EXCEEDED"));
                return;
            }
        }

        // Check per-IP rate limit
        if (config.rateLimitV2().perIp().isPresent()) {
            String ipKey = "ip:" + clientIp;
            if (!consumeToken(ipKey, config.rateLimitV2().perIp().get())) {
                requestContext.abortWith(createRateLimitResponse("IP_RATE_LIMIT_EXCEEDED"));
                return;
            }
        }

        // Check endpoint-specific rate limit
        String category = determineCategory(path);
        id.payu.gateway.config.GatewayConfig.TokenBucketConfig endpointConfig = config.rateLimitV2().endpoints()
            .getOrDefault(category, config.rateLimitV2().defaultRule());

        String endpointKey = "endpoint:" + category + ":" + userId + ":" + clientIp;
        if (!consumeToken(endpointKey, endpointConfig)) {
            requestContext.abortWith(createRateLimitResponse("ENDPOINT_RATE_LIMIT_EXCEEDED"));
        }
    }

    private boolean consumeToken(String key, id.payu.gateway.config.GatewayConfig.TokenBucketConfig config) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(config));
        return bucket.tryConsume();
    }

    private Response createRateLimitResponse(String error) {
        return Response.status(429)
            .header("Retry-After", "60")
            .header("X-RateLimit-Limit", "100")
            .header("X-RateLimit-Remaining", "0")
            .header("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000))
            .entity(Map.of(
                "error", error,
                "message", "Rate limit exceeded. Please try again later.",
                "retryAfter", 60
            ))
            .build();
    }

    private String determineCategory(String path) {
        for (Map.Entry<String, String> entry : ENDPOINT_CATEGORIES.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "default";
    }

    private String getUserId(ContainerRequestContext requestContext) {
        // Try to get user ID from JWT token
        // For now, return null (per-user limiting disabled)
        return requestContext.getHeaderString("X-User-Id");
    }

    private String getClientIp(ContainerRequestContext requestContext) {
        // Check X-Forwarded-For header
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // Return unknown (should not happen in production)
        return "unknown";
    }

    /**
     * Simple token bucket implementation.
     */
    private static class TokenBucket {
        private final int capacity;
        private final int refillTokens;
        private final long refillDurationMillis;
        private long tokens;
        private long lastRefillTimestamp;

        public TokenBucket(id.payu.gateway.config.GatewayConfig.TokenBucketConfig config) {
            this.capacity = config.capacity();
            this.refillTokens = config.refillTokens();
            this.refillDurationMillis = config.refillDuration().toMillis();
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;

            if (elapsed >= refillDurationMillis) {
                long refillCount = elapsed / refillDurationMillis;
                tokens = Math.min(capacity, tokens + refillTokens * refillCount);
                lastRefillTimestamp = now;
            }
        }
    }
}
