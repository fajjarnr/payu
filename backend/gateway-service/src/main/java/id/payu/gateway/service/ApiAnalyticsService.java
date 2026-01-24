package id.payu.gateway.service;

import id.payu.gateway.config.GatewayConfig;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track API usage analytics.
 * Tracks request counts, response times, error rates, and other metrics.
 */
@ApplicationScoped
public class ApiAnalyticsService {

    private final Map<String, ApiMetrics> metricsBuffer = new ConcurrentHashMap<>();
    private final AtomicLong bufferSize = new AtomicLong(0);

    @Inject
    GatewayConfig config;

    @Inject
    ReactiveRedisDataSource redis;

    private boolean enabled;

    @PostConstruct
    void init() {
        this.enabled = config.analytics().enabled();
        Log.infof("API Analytics service initialized (enabled: %s)", enabled);
    }

    /**
     * Record a request for analytics.
     */
    public void recordRequest(String path, String method, int statusCode, long duration) {
        if (!enabled) {
            return;
        }

        String key = buildMetricsKey(path, method, statusCode);

        metricsBuffer.compute(key, (k, metrics) -> {
            if (metrics == null) {
                return new ApiMetrics(path, method, statusCode, duration);
            } else {
                metrics.record(duration);
                return metrics;
            }
        });

        long currentSize = bufferSize.incrementAndGet();

        // Flush if buffer size exceeds threshold
        if (currentSize >= config.analytics().batchSize()) {
            flushMetrics();
        }
    }

    /**
     * Get metrics for a specific endpoint.
     */
    public Uni<Map<String, Object>> getMetrics(String path, String method) {
        String key = buildMetricsKey(path, method, 0);
        ApiMetrics metrics = metricsBuffer.get(key);

        if (metrics != null) {
            return Uni.createFrom().item(metrics.toMap());
        }

        // Try to fetch from Redis
        String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
        String redisKey = "analytics:" + today + ":" + key;

        return redis.key().exists(redisKey)
            .flatMap(exists -> {
                if (exists) {
                    return redis.value(String.class).get(redisKey)
                        .map(value -> {
                            Map<?, ?> parsed = parseMetricsJson(value);
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = (Map<String, Object>) parsed;
                            return result;
                        });
                }
                return Uni.createFrom().item(Map.<String, Object>of());
            })
            .onFailure().recoverWithItem(Map.<String, Object>of());
    }

    /**
     * Scheduled flush of metrics to Redis.
     */
    @Scheduled(every = "{gateway.analytics.flush-interval}", delayed = "1m")
    void flushMetrics() {
        if (!enabled || metricsBuffer.isEmpty()) {
            return;
        }

        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);

            metricsBuffer.forEach((key, metrics) -> {
                String redisKey = "analytics:" + today + ":" + key;
                String metricsJson = toJson(metrics);

                // Store in Redis with TTL
                redis.value(String.class)
                    .setex(redisKey, config.analytics().retentionDays() * 86400, metricsJson)
                    .subscribe()
                    .with(unused -> {}, failure -> Log.warnf(failure, "Failed to store metrics for %s", key));
            });

            long flushedSize = bufferSize.getAndSet(0);
            metricsBuffer.clear();

            Log.debugf("Flushed %d metrics to Redis", flushedSize);

        } catch (Exception e) {
            Log.errorf(e, "Failed to flush metrics to Redis");
        }
    }

    private String buildMetricsKey(String path, String method, int statusCode) {
        return method + ":" + path;
    }

    private String toJson(ApiMetrics metrics) {
        return String.format(
            "{\"path\":\"%s\",\"method\":\"%s\",\"count\":%d,\"totalTime\":%d,\"minTime\":%d,\"maxTime\":%d,\"avgTime\":%.2f,\"errorCount\":%d}",
            metrics.path, metrics.method, metrics.count, metrics.totalTime,
            metrics.minTime, metrics.maxTime, metrics.getAverageTime(), metrics.errorCount
        );
    }

    private Map<String, Object> parseMetricsJson(String json) {
        // Simple JSON parsing - in production, use proper JSON library
        return Map.of("raw", json);
    }

    /**
     * Inner class to hold metrics for a specific endpoint.
     */
    public static class ApiMetrics {
        private final String path;
        private final String method;
        private final int statusCode;
        private long count = 1;
        private long totalTime;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = 0;
        private long errorCount = 0;
        private final Instant firstSeen = Instant.now();
        private Instant lastSeen = Instant.now();

        public ApiMetrics(String path, String method, int statusCode, long duration) {
            this.path = path;
            this.method = method;
            this.statusCode = statusCode;
            this.totalTime = duration;
            this.minTime = duration;
            this.maxTime = duration;
            this.errorCount = (statusCode >= 400) ? 1 : 0;
        }

        public void record(long duration) {
            count++;
            totalTime += duration;
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
            lastSeen = Instant.now();
        }

        public double getAverageTime() {
            return count > 0 ? (double) totalTime / count : 0;
        }

        public double getErrorRate() {
            return count > 0 ? (double) errorCount / count * 100 : 0;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                "path", path,
                "method", method,
                "count", count,
                "totalTime", totalTime,
                "minTime", minTime,
                "maxTime", maxTime,
                "averageTime", getAverageTime(),
                "errorRate", getErrorRate(),
                "firstSeen", firstSeen.toString(),
                "lastSeen", lastSeen.toString()
            );
        }
    }
}
