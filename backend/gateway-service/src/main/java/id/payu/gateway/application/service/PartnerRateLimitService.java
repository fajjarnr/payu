package id.payu.gateway.application.service;

import id.payu.gateway.domain.entity.PartnerRatePlan;
import id.payu.gateway.domain.entity.RatePlan;
import id.payu.gateway.domain.repository.PartnerRatePlanRepository;
import id.payu.gateway.domain.repository.RatePlanRepository;
import id.payu.gateway.domain.vo.RateLimit;
import id.payu.gateway.domain.vo.TimeWindow;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Application Service for partner-specific rate limiting.
 * <p>
 * Manages rate limits per partner based on their assigned RatePlan.
 * Uses Redis for distributed rate limit counters with sliding window.
 * <p>
 * Features:
 * - Per-partner rate limits from assigned RatePlan
 * - Per-endpoint overrides within plans
 * - Distributed Redis-backed counters
 * - Configurable time windows (minute, hour, day)
 */
@ApplicationScoped
public class PartnerRateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "partner:ratelimit:";
    private static final String PARTNER_REQUEST_COUNT_KEY = "partner:requests:";

    @Inject
    RatePlanRepository ratePlanRepository;

    @Inject
    PartnerRatePlanRepository partnerRatePlanRepository;

    @Inject
    ReactiveRedisDataSource redisDataSource;

    private ReactiveValueCommands<String, String> valueCommands;

    @PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
        Log.info("PartnerRateLimitService initialized");
    }

    /**
     * Check if a partner's request is within their rate limits.
     *
     * @param partnerId The partner ID
     * @param endpoint  The API endpoint being accessed
     * @return Uni containing RateLimitCheck result
     */
    public Uni<RateLimitCheck> checkRateLimit(String partnerId, String endpoint) {
        return getEffectiveRateLimit(partnerId, endpoint)
            .flatMap(rateLimit -> {
                if (rateLimit == null) {
                    // No rate limit configured, allow
                    return Uni.createFrom().item(RateLimitCheck.allowAll());
                }

                return checkAllWindows(partnerId, endpoint, rateLimit);
            })
            .onFailure().recoverWithItem(e -> {
                Log.warnf(e, "Rate limit check failed for partner %s, allowing request", partnerId);
                return RateLimitCheck.allowAll();
            });
    }

    /**
     * Get the effective rate limit for a partner on a specific endpoint.
     */
    public Uni<RateLimit> getEffectiveRateLimit(String partnerId, String endpoint) {
        return partnerRatePlanRepository.findEffectiveByPartnerId(partnerId)
            .flatMap(optionalAssignment -> {
                if (optionalAssignment.isEmpty()) {
                    return Uni.createFrom().nullItem();
                }

                PartnerRatePlan assignment = optionalAssignment.get();
                return ratePlanRepository.findById(assignment.getRatePlanId())
                    .map(optionalPlan ->
                        optionalPlan.map(plan -> plan.getEffectiveLimit(endpoint)).orElse(null)
                    );
            });
    }

    /**
     * Get the assigned rate plan for a partner.
     */
    public Uni<Optional<RatePlan>> getPartnerRatePlan(String partnerId) {
        return partnerRatePlanRepository.findEffectiveByPartnerId(partnerId)
            .flatMap(optionalAssignment -> {
                if (optionalAssignment.isEmpty()) {
                    return Uni.createFrom().item(Optional.<RatePlan>empty());
                }
                return ratePlanRepository.findById(optionalAssignment.get().getRatePlanId());
            });
    }

    /**
     * Assign a rate plan to a partner.
     */
    public Uni<PartnerRatePlan> assignRatePlan(String partnerId, String ratePlanId) {
        // Deactivate existing assignment first
        return partnerRatePlanRepository.deactivateByPartnerId(partnerId)
            .flatMap(unused -> {
                PartnerRatePlan newAssignment = new PartnerRatePlan(
                    java.util.UUID.randomUUID().toString(),
                    partnerId,
                    ratePlanId
                );
                return partnerRatePlanRepository.save(newAssignment);
            });
    }

    /**
     * Record a request for analytics (non-blocking).
     */
    public void recordRequest(String partnerId, String endpoint) {
        String minuteKey = buildCounterKey(partnerId, endpoint, "minute");
        String hourKey = buildCounterKey(partnerId, endpoint, "hour");
        String dayKey = buildCounterKey(partnerId, endpoint, "day");

        long now = Instant.now().getEpochSecond();

        // Increment counters with TTL
        incrementCounter(minuteKey, 60);
        incrementCounter(hourKey, 3600);
        incrementCounter(dayKey, 86400);
    }

    private void incrementCounter(String key, long ttlSeconds) {
        // Use Redis INCR and EXPIRE
        io.vertx.mutiny.redis.client.RedisAPI redisAPI =
            io.vertx.mutiny.redis.client.RedisAPI.api(redisDataSource.getRedis());

        redisAPI.incr(key)
            .flatMap(response -> redisAPI.expire(java.util.List.of(key, String.valueOf(ttlSeconds))))
            .subscribe()
            .with(
                unused -> {},
                failure -> Log.debugf("Failed to increment counter %s: %s", key, failure.getMessage())
            );
    }

    private Uni<RateLimitCheck> checkAllWindows(String partnerId, String endpoint, RateLimit rateLimit) {
        String minuteKey = buildCounterKey(partnerId, endpoint, "minute");
        String hourKey = buildCounterKey(partnerId, endpoint, "hour");
        String dayKey = buildCounterKey(partnerId, endpoint, "day");

        io.vertx.mutiny.redis.client.RedisAPI redisAPI =
            io.vertx.mutiny.redis.client.RedisAPI.api(redisDataSource.getRedis());

        // Get all counters in parallel
        Uni<Long> minuteCount = getCounterValue(redisAPI, minuteKey);
        Uni<Long> hourCount = getCounterValue(redisAPI, hourKey);
        Uni<Long> dayCount = getCounterValue(redisAPI, dayKey);

        return Uni.combine().all()
            .unis(minuteCount, hourCount, dayCount)
            .with((min, hour, day) -> {
                boolean allowed = true;
                String limitingWindow = null;
                long limit = 0;
                long current = 0;
                long retryAfter = 0;

                if (min > rateLimit.requestsPerMinute()) {
                    allowed = false;
                    limitingWindow = "minute";
                    limit = rateLimit.requestsPerMinute();
                    current = min;
                    retryAfter = 60;
                } else if (hour > rateLimit.requestsPerHour()) {
                    allowed = false;
                    limitingWindow = "hour";
                    limit = rateLimit.requestsPerHour();
                    current = hour;
                    retryAfter = 3600 - (Instant.now().getEpochSecond() % 3600);
                } else if (day > rateLimit.requestsPerDay()) {
                    allowed = false;
                    limitingWindow = "day";
                    limit = rateLimit.requestsPerDay();
                    current = day;
                    retryAfter = 86400 - (Instant.now().getEpochSecond() % 86400);
                }

                return new RateLimitCheck(
                    allowed,
                    allowed ? rateLimit.requestsPerMinute() - min : 0,
                    limit,
                    current,
                    retryAfter,
                    limitingWindow
                );
            });
    }

    private Uni<Long> getCounterValue(io.vertx.mutiny.redis.client.RedisAPI redisAPI, String key) {
        return redisAPI.get(key)
            .map(response -> {
                if (response == null || response.toString() == null) {
                    return 0L;
                }
                try {
                    return Long.parseLong(response.toString());
                } catch (NumberFormatException e) {
                    return 0L;
                }
            })
            .onFailure().recoverWithItem(0L);
    }

    private String buildCounterKey(String partnerId, String endpoint, String window) {
        String endpointHash = String.valueOf(endpoint.hashCode());
        return RATE_LIMIT_KEY_PREFIX + partnerId + ":" + endpointHash + ":" + window;
    }

    /**
     * Result of a rate limit check.
     */
    public record RateLimitCheck(
        boolean allowed,
        long remaining,
        long limit,
        long current,
        long retryAfter,
        String limitingWindow
    ) {
        public static RateLimitCheck allowAll() {
            return new RateLimitCheck(true, Long.MAX_VALUE, Long.MAX_VALUE, 0, 0, null);
        }

        public static RateLimitCheck denied(long limit, long current, long retryAfter, String window) {
            return new RateLimitCheck(false, 0, limit, current, retryAfter, window);
        }
    }
}
