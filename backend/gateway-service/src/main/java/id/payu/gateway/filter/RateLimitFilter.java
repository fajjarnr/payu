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
import java.util.Map;

/**
 * Rate limiting filter using Redis for distributed rate limiting.
 * Implements sliding window algorithm.
 */
@Provider
@ApplicationScoped
public class RateLimitFilter implements ContainerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
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
        map.put("/api/v1/cashbacks", "cashbacks");
        map.put("/api/v1/loyalty-points", "loyalty-points");
        map.put("/api/v1/rewards", "rewards");
        map.put("/api/v1/referrals", "referrals");
        ENDPOINT_CATEGORIES = Map.copyOf(map);
    }

    @Inject
    GatewayConfig config;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, Long> valueCommands;

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(Long.class);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.rateLimit().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        
        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health")) {
            return;
        }

        // Determine rate limit rule
        String category = determineCategory(path);
        GatewayConfig.RateLimitRule rule = getRule(category);

        // Get client identifier (IP or user ID)
        String clientId = getClientId(requestContext);
        String key = RATE_LIMIT_PREFIX + category + ":" + clientId;

        // Check rate limit (blocking for simplicity, should be reactive in production)
        try {
            RateLimitResult result = checkRateLimit(key, rule);
            
            if (result.isRateLimited()) {
                Log.warnf("Rate limit exceeded for client=%s, category=%s", clientId, category);
                requestContext.abortWith(
                    Response.status(429)
                        .header("Retry-After", "60")
                        .header("X-RateLimit-Remaining", "0")
                        .entity(Map.of(
                            "error", "RATE_LIMIT_EXCEEDED",
                            "message", "Too many requests. Please try again later.",
                            "retryAfter", 60
                        ))
                        .build()
                );
            } else {
                requestContext.getHeaders().add("X-RateLimit-Remaining", 
                    String.valueOf(Math.max(0, rule.requestsPerMinute() - result.getCount())));
            }
        } catch (Exception e) {
            // If Redis is down, allow request (fail-open)
            Log.warnf(e, "Rate limit check failed, allowing request");
        }
    }

    private String determineCategory(String path) {
        for (Map.Entry<String, String> entry : ENDPOINT_CATEGORIES.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "default";
    }

    private GatewayConfig.RateLimitRule getRule(String category) {
        if ("default".equals(category)) {
            return config.rateLimit().defaultRule();
        }
        return config.rateLimit().endpoints().getOrDefault(category, config.rateLimit().defaultRule());
    }

    private String getClientId(ContainerRequestContext requestContext) {
        // Try to get user ID from JWT
        // For now, use IP address
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return "unknown";
    }

    private RateLimitResult checkRateLimit(String key, GatewayConfig.RateLimitRule rule) {
        // Sliding window rate limiting using Redis
        Long currentCount = valueCommands.get(key).await().atMost(Duration.ofSeconds(1));
        
        if (currentCount == null) {
            // First request, set counter with TTL
            valueCommands.setex(key, 60, 1L).await().atMost(Duration.ofSeconds(1));
            return new RateLimitResult(1, false);
        }

        // Check if limit exceeded
        if (currentCount >= rule.requestsPerMinute()) {
            return new RateLimitResult(currentCount, true);
        }

        // Increment counter with TTL to ensure it expires
        valueCommands.incr(key).await().atMost(Duration.ofSeconds(1));
        return new RateLimitResult(currentCount + 1, false);
    }

    private static class RateLimitResult {
        private final long count;
        private final boolean rateLimited;

        public RateLimitResult(long count, boolean rateLimited) {
            this.count = count;
            this.rateLimited = rateLimited;
        }

        public long getCount() {
            return count;
        }

        public boolean isRateLimited() {
            return rateLimited;
        }
    }
}
