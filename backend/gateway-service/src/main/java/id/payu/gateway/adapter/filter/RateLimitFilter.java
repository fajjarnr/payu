package id.payu.gateway.adapter.filter;

import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.adapter.cache.HotRodCacheClient;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Consolidated rate limiting filter using Redis sorted sets for sliding window.
 * <p>
 * This replaces both the old fixed-window RateLimitFilter and the in-memory
 * RateLimitV2Filter with a single, distributed Redis-backed sliding window
 * algorithm (IMP-005).
 * <p>
 * Sliding Window Algorithm:
 * - Uses Redis sorted sets (ZSET) with timestamp as score
 * - Each request adds a member with current timestamp
 * - Window cleanup removes entries older than window start
 * - Count of remaining entries = requests in window
 * - Atomic via Redis pipeline (MULTI/EXEC)
 * <p>
 * Features:
 * 1. Distributed across pods (Redis-backed)
 * 2. True sliding window (not fixed window)
 * 3. Per-endpoint category limits from config
 * 4. IP-based + user-based tracking
 * 5. Fail-open strategy if Redis is down
 * 6. Proper Retry-After and rate limit headers
 */
@Provider
@ApplicationScoped
@RunOnVirtualThread
public class RateLimitFilter implements ContainerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:sw:";

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
        map.put("/api/v1/public", "contents");
        map.put("/api/v1/contents", "contents");
        ENDPOINT_CATEGORIES = Map.copyOf(map);
    }

    // Window size in seconds per category
    private static final Map<String, Integer> CATEGORY_WINDOWS = Map.of(
            "auth", 300,     // 5 minutes for auth
            "otp", 300,      // 5 minutes for OTP
            "transfer", 60,  // 1 minute for transfers
            "default", 60    // 1 minute default
    );

    @Inject
    GatewayConfig config;

    @Inject
    HotRodCacheClient cache;

    @PostConstruct
    void init() {
        Log.info("RateLimitFilter initialized (sliding window, Data Grid-backed)");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!config.rateLimit().enabled()) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        // Skip public auth endpoints — OIDC code exchange is per-auth-code, not per-IP sliding window.
        // Keeping it in the sliding window blocked the event loop without virtual threads (see 1.18.78).
        if (path.startsWith("/api/v1/auth") || path.startsWith("/api/auth") || path.startsWith("/v1/auth")) {
            return;
        }

        // Skip health and metrics endpoints
        if (path.startsWith("/q/") || path.equals("/health") || path.equals("/status")
                || path.equals("/version")) {
            return;
        }

        // IMP-070: Test mode bypass — skip rate limiting when X-E2E-Test header is present
        // and test-mode is enabled. This allows E2E test suites to run without hitting
        // rate limits while keeping production limits unchanged.
        if (config.rateLimit().testMode()) {
            String e2eHeader = requestContext.getHeaderString("X-E2E-Test");
            if (e2eHeader != null && !e2eHeader.isBlank()) {
                Log.debugf("Rate limit bypassed for E2E test: path=%s", path);
                return;
            }
        }

        String category = determineCategory(path);
        GatewayConfig.RateLimitRule rule = getRule(category);
        int windowSeconds = CATEGORY_WINDOWS.getOrDefault(category, 60);

        String clientId = getClientId(requestContext);

        // DEVSECOPS-003: Global rate limit — hard cap across ALL endpoints/IPs/users
        var globalOpt = config.rateLimitV2().globalRule();
        if (globalOpt.isPresent()) {
            var globalRule = globalOpt.get();
            String globalKey = RATE_LIMIT_PREFIX + "global:all";
            SlidingWindowResult globalResult = checkSlidingWindow(globalKey, globalRule.capacity(), 1);
            if (globalResult.limited()) {
                long retryAfter = Math.max(1, globalResult.windowResetEpoch() - Instant.now().getEpochSecond());
                Log.warnf("Global rate limit exceeded: count=%d, limit=%d", globalResult.count(), globalRule.capacity());
                requestContext.abortWith(
                        Response.status(429)
                                .header("Retry-After", String.valueOf(retryAfter))
                                .entity(Map.of(
                                        "error", "GLOBAL_RATE_LIMIT_EXCEEDED",
                                        "message", "Service temporarily unavailable due to high traffic. Please retry shortly.",
                                        "retryAfter", retryAfter
                                ))
                                .build()
                );
                return;
            }
        }

        try {
            String key = RATE_LIMIT_PREFIX + category + ":" + clientId;
            SlidingWindowResult result = checkSlidingWindow(key, rule.requestsPerMinute(), windowSeconds);

            // Add RFC-compliant rate limit headers
            int remaining = Math.max(0, rule.requestsPerMinute() - (int) result.count());
            requestContext.getHeaders().add("X-RateLimit-Limit", String.valueOf(rule.requestsPerMinute()));
            requestContext.getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            requestContext.getHeaders().add("X-RateLimit-Window", String.valueOf(windowSeconds));
            requestContext.getHeaders().add("X-RateLimit-Reset",
                    String.valueOf(result.windowResetEpoch()));

            if (result.limited()) {
                long retryAfter = Math.max(1, result.windowResetEpoch() - Instant.now().getEpochSecond());
                Log.warnf("Rate limit exceeded: client=%s, category=%s, count=%d, limit=%d",
                        clientId, category, result.count(), rule.requestsPerMinute());
                requestContext.abortWith(
                        Response.status(429)
                                .header("Retry-After", String.valueOf(retryAfter))
                                .entity(Map.of(
                                        "error", "RATE_LIMIT_EXCEEDED",
                                        "message", "Too many requests. Please try again later.",
                                        "retryAfter", retryAfter,
                                        "category", category
                                ))
                                .build()
                );
            }
        } catch (Exception e) {
            // Fail-open: if Data Grid is unavailable, allow the request
            Log.warnf(e, "Rate limit check failed (Data Grid unavailable?), allowing request for client=%s", clientId);
        }
    }

    /**
     * Sliding window rate limit check using one Hot Rod versioned cache entry.
     */
    private SlidingWindowResult checkSlidingWindow(String key, int maxRequests, int windowSeconds) {
        HotRodCacheClient.SlidingWindow window = cache.recordSlidingWindowRequest(key, Duration.ofSeconds(windowSeconds))
                .await().atMost(Duration.ofSeconds(2));
        long windowResetEpoch = (window.oldestRequestEpochMillis() / 1000) + windowSeconds;
        return new SlidingWindowResult(window.count(), window.count() > maxRequests, windowResetEpoch);
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

    /**
     * Get client identifier from request context.
     * Uses IP for unauthenticated, IP+user for authenticated.
     */
    String getClientId(ContainerRequestContext requestContext) {
        StringBuilder clientId = new StringBuilder();
        String ip = getClientIp(requestContext);
        clientId.append(ip.replace(":", "_")); // IPv6 safe

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String userId = requestContext.getHeaderString("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                clientId.append(":").append(userId);
                return clientId.toString();
            }

            String accountId = requestContext.getHeaderString("X-Account-Id");
            if (accountId != null && !accountId.isBlank()) {
                clientId.append(":").append(accountId);
                return clientId.toString();
            }

            clientId.append(":authenticated");
        }

        return clientId.toString();
    }

    private String getClientIp(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return "unknown";
    }

    /**
     * Result of a sliding window rate limit check.
     */
    record SlidingWindowResult(long count, boolean limited, long windowResetEpoch) {}
}
