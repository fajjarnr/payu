package id.payu.gateway.domain.repository;

import id.payu.gateway.domain.entity.ApiAnalyticsEvent;
import id.payu.gateway.domain.vo.HttpMethod;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Repository port for API Analytics Events.
 * Follows hexagonal architecture - this is the output port.
 * <p>
 * Implementations handle persistence to TimescaleDB and Redis.
 */
public interface ApiAnalyticsRepository {

    /**
     * Save a single analytics event.
     */
    Uni<Void> save(ApiAnalyticsEvent event);

    /**
     * Save multiple analytics events in batch.
     */
    Uni<Void> saveBatch(List<ApiAnalyticsEvent> events);

    /**
     * Find events by partner ID within a time range.
     */
    Multi<ApiAnalyticsEvent> findByPartnerId(String partnerId, Instant from, Instant to);

    /**
     * Find events by endpoint within a time range.
     */
    Multi<ApiAnalyticsEvent> findByEndpoint(String endpoint, HttpMethod method, Instant from, Instant to);

    /**
     * Get aggregated metrics for a partner.
     */
    Uni<PartnerMetrics> getPartnerMetrics(String partnerId, Instant from, Instant to);

    /**
     * Get aggregated metrics for an endpoint.
     */
    Uni<EndpointMetrics> getEndpointMetrics(String endpoint, HttpMethod method, Instant from, Instant to);

    /**
     * Get top endpoints by request count.
     */
    Multi<EndpointUsage> getTopEndpoints(int limit, Instant from, Instant to);

    /**
     * Delete events older than the retention period.
     * Returns the number of deleted events.
     */
    Uni<Long> deleteOlderThan(Instant cutoff);

    /**
     * Aggregate daily metrics for long-term storage.
     */
    Uni<Void> aggregateDailyMetrics(Instant day);

    /**
     * Value object for partner metrics.
     */
    record PartnerMetrics(
        String partnerId,
        long totalRequests,
        long successfulRequests,
        long errorRequests,
        long serverErrors,
        double avgResponseTime,
        long minResponseTime,
        long maxResponseTime,
        Map<Integer, Long> statusCodeDistribution
    ) {}

    /**
     * Value object for endpoint metrics.
     */
    record EndpointMetrics(
        String endpoint,
        HttpMethod method,
        long totalRequests,
        long successfulRequests,
        long errorRequests,
        double avgResponseTime,
        long minResponseTime,
        long maxResponseTime,
        Map<Integer, Long> statusCodeDistribution
    ) {}

    /**
     * Value object for endpoint usage ranking.
     */
    record EndpointUsage(
        String endpoint,
        HttpMethod method,
        long requestCount,
        double avgResponseTime,
        double errorRate
    ) {}
}
