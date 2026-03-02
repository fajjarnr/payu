package id.payu.gateway.application.service;

import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.domain.entity.ApiAnalyticsEvent;
import id.payu.gateway.domain.repository.ApiAnalyticsRepository;
import id.payu.gateway.domain.vo.HttpMethod;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced API Analytics Service with persistent storage.
 * Replaces the in-memory ApiAnalyticsService with TimescaleDB + Redis persistence.
 * <p>
 * Features:
 * - Per-partner, per-endpoint, per-method tracking
 * - 90 days detailed retention, 1 year aggregated retention
 * - Batch processing for high throughput
 * - Automatic aggregation and cleanup
 * <p>
 * This is an Application Service in the Hexagonal Architecture.
 */
@ApplicationScoped
public class PersistentAnalyticsService {

    private final ConcurrentLinkedQueue<ApiAnalyticsEvent> eventBuffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong bufferSize = new AtomicLong(0);

    @Inject
    GatewayConfig config;

    @Inject
    ApiAnalyticsRepository analyticsRepository;

    private boolean enabled;
    private int batchSize;
    private int detailedRetentionDays;
    private int aggregatedRetentionDays;

    @PostConstruct
    void init() {
        this.enabled = config.analytics().enabled();
        this.batchSize = config.analytics().batchSize();
        this.detailedRetentionDays = 90;
        this.aggregatedRetentionDays = 365;
        Log.infof("PersistentAnalyticsService initialized (enabled: %s, batchSize: %d)", enabled, batchSize);
    }

    /**
     * Record an analytics event from filter context.
     */
    public void recordEvent(String partnerId, String endpoint, String method,
                           int statusCode, long durationMs, String clientIp,
                           String userAgent, String correlationId) {
        if (!enabled) {
            return;
        }

        ApiAnalyticsEvent event = ApiAnalyticsEvent.builder()
            .id(UUID.randomUUID().toString())
            .partnerId(partnerId)
            .endpoint(endpoint)
            .method(method)
            .statusCode(statusCode)
            .durationMs(durationMs)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .correlationId(correlationId)
            .timestamp(Instant.now())
            .build();

        eventBuffer.offer(event);
        long currentSize = bufferSize.incrementAndGet();

        // Flush if buffer exceeds threshold
        if (currentSize >= batchSize) {
            flushBuffer();
        }
    }

    /**
     * Get metrics for a specific partner.
     */
    public Uni<ApiAnalyticsRepository.PartnerMetrics> getPartnerMetrics(String partnerId,
                                                                        Instant from, Instant to) {
        return analyticsRepository.getPartnerMetrics(partnerId, from, to);
    }

    /**
     * Get metrics for a specific endpoint.
     */
    public Uni<ApiAnalyticsRepository.EndpointMetrics> getEndpointMetrics(String endpoint,
                                                                          String method,
                                                                          Instant from, Instant to) {
        return analyticsRepository.getEndpointMetrics(endpoint, HttpMethod.fromString(method), from, to);
    }

    /**
     * Get top endpoints by usage.
     */
    public Multi<ApiAnalyticsRepository.EndpointUsage> getTopEndpoints(int limit, Instant from, Instant to) {
        return analyticsRepository.getTopEndpoints(limit, from, to);
    }

    /**
     * Scheduled flush of buffered events to persistent storage.
     */
    @Scheduled(every = "{gateway.analytics.flush-interval}", delayed = "1m")
    void flushBuffer() {
        if (!enabled || eventBuffer.isEmpty()) {
            return;
        }

        List<ApiAnalyticsEvent> batch = new ArrayList<>();
        ApiAnalyticsEvent event;
        int count = 0;

        while ((event = eventBuffer.poll()) != null && count < batchSize) {
            batch.add(event);
            count++;
        }

        if (batch.isEmpty()) {
            return;
        }

        bufferSize.addAndGet(-count);

        final int flushedCount = count;
        analyticsRepository.saveBatch(batch)
            .subscribe()
            .with(
                unused -> Log.debugf("Flushed %d analytics events", flushedCount),
                failure -> {
                    Log.errorf(failure, "Failed to flush analytics events");
                    // Re-queue events for retry
                    batch.forEach(eventBuffer::offer);
                    bufferSize.addAndGet(batch.size());
                }
            );
    }

    /**
     * Scheduled daily aggregation of metrics.
     * Runs at 2 AM daily to aggregate the previous day's data.
     */
    @Scheduled(cron = "0 2 * * * ?")
    void aggregateDailyMetrics() {
        if (!enabled) {
            return;
        }

        Instant yesterday = LocalDate.now().minusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant();

        analyticsRepository.aggregateDailyMetrics(yesterday)
            .subscribe()
            .with(
                unused -> Log.infof("Aggregated daily metrics for %s", yesterday),
                failure -> Log.errorf(failure, "Failed to aggregate daily metrics")
            );
    }

    /**
     * Scheduled cleanup of old detailed data.
     * Retains 90 days of detailed data.
     */
    @Scheduled(cron = "0 3 0 * * ?")
    void cleanupDetailedData() {
        if (!enabled) {
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(detailedRetentionDays));

        analyticsRepository.deleteOlderThan(cutoff)
            .subscribe()
            .with(
                deleted -> Log.infof("Cleaned up %d old analytics events (older than %d days)",
                    deleted, detailedRetentionDays),
                failure -> Log.errorf(failure, "Failed to cleanup old analytics data")
            );
    }

    /**
     * Get current buffer size for monitoring.
     */
    public long getBufferSize() {
        return bufferSize.get();
    }

    /**
     * Get retention configuration.
     */
    public Map<String, Integer> getRetentionConfig() {
        return Map.of(
            "detailedDays", detailedRetentionDays,
            "aggregatedDays", aggregatedRetentionDays
        );
    }
}
