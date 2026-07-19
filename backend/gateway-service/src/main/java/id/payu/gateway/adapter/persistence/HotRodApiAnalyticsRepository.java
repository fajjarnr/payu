package id.payu.gateway.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.gateway.domain.entity.ApiAnalyticsEvent;
import id.payu.gateway.domain.repository.ApiAnalyticsRepository;
import id.payu.gateway.domain.vo.HttpMethod;
import id.payu.gateway.adapter.cache.HotRodCacheClient;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hot Rod-based implementation of ApiAnalyticsRepository.
 *
 * <p>
 * This implementation provides:
 * - Fast writes using atomic Data Grid lists for buffering
 * - Time-series data organization by day
 * - Aggregation support for metrics queries
 * - TTL-based automatic expiration (90 days detailed)
 *
 * <p>
 * For production with TimescaleDB, this can be extended or replaced
 * with a hybrid implementation that:
 * - Uses Redis for real-time buffering
 * - Persists to TimescaleDB for long-term storage
 */
@ApplicationScoped
public class HotRodApiAnalyticsRepository implements ApiAnalyticsRepository {

    private static final String ANALYTICS_KEY_PREFIX = "analytics:events:";
    private static final String ANALYTICS_INDEX_PREFIX = "analytics:index:";
    private static final String METRICS_KEY_PREFIX = "analytics:metrics:";
    private static final int DETAILED_RETENTION_DAYS = 90;

    @Inject
    HotRodCacheClient cache;

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        Log.info("HotRodApiAnalyticsRepository initialized");
    }

    @Override
    public Uni<Void> save(ApiAnalyticsEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = buildDailyKey(event.getTimestamp());

            return cache.appendToList(key, json, Duration.ofDays(DETAILED_RETENTION_DAYS));
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialize analytics event");
            return Uni.createFrom().failure(e);
        }
    }

    @Override
    public Uni<Void> saveBatch(List<ApiAnalyticsEvent> events) {
        if (events.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        // Group events by day for efficient storage
        Map<String, List<String>> eventsByDay = new HashMap<>();

        for (ApiAnalyticsEvent event : events) {
            try {
                String json = objectMapper.writeValueAsString(event);
                String key = buildDailyKey(event.getTimestamp());
                eventsByDay.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(json);
            } catch (JsonProcessingException e) {
                Log.warnf(e, "Failed to serialize event: %s", event.getId());
            }
        }

        // Store each day's events
        Uni<Void> result = Uni.createFrom().voidItem();

        for (Map.Entry<String, List<String>> entry : eventsByDay.entrySet()) {
            String key = entry.getKey();
            List<String> jsonEvents = entry.getValue();

            result = result.chain(() ->
                appendAll(key, jsonEvents)
            );
        }

        return result;
    }

    @Override
    public Multi<ApiAnalyticsEvent> findByPartnerId(String partnerId, Instant from, Instant to) {
        // Scan through daily keys and filter by partner
        return findByTimeRange(from, to)
            .filter(event -> partnerId.equals(event.getPartnerId()));
    }

    @Override
    public Multi<ApiAnalyticsEvent> findByEndpoint(String endpoint, HttpMethod method, Instant from, Instant to) {
        return findByTimeRange(from, to)
            .filter(event ->
                endpoint.equals(event.getEndpoint()) &&
                (method == null || method == event.getMethod())
            );
    }

    @Override
    public Uni<PartnerMetrics> getPartnerMetrics(String partnerId, Instant from, Instant to) {
        return findByPartnerId(partnerId, from, to)
            .collect().asList()
            .map(events -> calculatePartnerMetrics(partnerId, events));
    }

    @Override
    public Uni<EndpointMetrics> getEndpointMetrics(String endpoint, HttpMethod method, Instant from, Instant to) {
        return findByEndpoint(endpoint, method, from, to)
            .collect().asList()
            .map(events -> calculateEndpointMetrics(endpoint, method, events));
    }

    @Override
    public Multi<EndpointUsage> getTopEndpoints(int limit, Instant from, Instant to) {
        return findByTimeRange(from, to)
            .collect().asList()
            .onItem().transformToMulti(events -> {
                // Group by endpoint and calculate metrics
                Map<String, List<ApiAnalyticsEvent>> byEndpoint = events.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.getMethod().name() + ":" + e.getEndpoint()
                    ));

                List<EndpointUsage> usages = byEndpoint.entrySet().stream()
                    .map(entry -> {
                        String[] parts = entry.getKey().split(":", 2);
                        HttpMethod method = HttpMethod.valueOf(parts[0]);
                        String path = parts[1];
                        List<ApiAnalyticsEvent> endpointEvents = entry.getValue();

                        long count = endpointEvents.size();
                        long errors = endpointEvents.stream()
                            .filter(ApiAnalyticsEvent::isError)
                            .count();
                        double avgTime = endpointEvents.stream()
                            .mapToLong(ApiAnalyticsEvent::getDurationMs)
                            .average()
                            .orElse(0);

                        return new EndpointUsage(path, method, count, avgTime,
                            count > 0 ? (double) errors / count * 100 : 0);
                    })
                    .sorted((a, b) -> Long.compare(b.requestCount(), a.requestCount()))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());

                return Multi.createFrom().iterable(usages);
            });
    }

    @Override
    public Uni<Long> deleteOlderThan(Instant cutoff) {
        // Calculate which daily keys to delete
        // This is a simplified implementation
        return Uni.createFrom().item(0L);
    }

    @Override
    public Uni<Void> aggregateDailyMetrics(Instant day) {
        // Aggregate metrics for the given day
        // Store aggregated results with longer retention
        return Uni.createFrom().voidItem();
    }

    private Multi<ApiAnalyticsEvent> findByTimeRange(Instant from, Instant to) {
        // Generate daily keys for the time range
        List<String> keys = new java.util.ArrayList<>();
        Instant current = from.truncatedTo(java.time.temporal.ChronoUnit.DAYS);

        while (!current.isAfter(to)) {
            keys.add(buildDailyKey(current));
            current = current.plus(Duration.ofDays(1));
        }

        // Fetch events from all keys
        return Multi.createFrom().iterable(keys)
            .onItem().transformToMultiAndConcatenate(key ->
                cache.readList(key)
                    .onItem().transformToMulti(list ->
                        Multi.createFrom().iterable(list != null ? list : List.of())
                    )
            )
            .onItem().transform(this::parseEvent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(event ->
                !event.getTimestamp().isBefore(from) &&
                !event.getTimestamp().isAfter(to)
            );
    }

    private Optional<ApiAnalyticsEvent> parseEvent(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, ApiAnalyticsEvent.class));
        } catch (Exception e) {
            Log.warnf(e, "Failed to parse analytics event");
            return Optional.empty();
        }
    }

    private Uni<Void> appendAll(String key, List<String> jsonEvents) {
        Uni<Void> result = Uni.createFrom().voidItem();
        for (String jsonEvent : jsonEvents) {
            result = result.chain(() -> cache.appendToList(key, jsonEvent, Duration.ofDays(DETAILED_RETENTION_DAYS)));
        }
        return result;
    }

    private String buildDailyKey(Instant timestamp) {
        String date = timestamp.atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString();
        return ANALYTICS_KEY_PREFIX + date;
    }

    private PartnerMetrics calculatePartnerMetrics(String partnerId, List<ApiAnalyticsEvent> events) {
        long total = events.size();
        long success = events.stream().filter(ApiAnalyticsEvent::isSuccess).count();
        long errors = events.stream().filter(ApiAnalyticsEvent::isError).count();
        long serverErrors = events.stream().filter(ApiAnalyticsEvent::isServerError).count();

        double avgTime = events.stream()
            .mapToLong(ApiAnalyticsEvent::getDurationMs)
            .average()
            .orElse(0);
        long minTime = events.stream()
            .mapToLong(ApiAnalyticsEvent::getDurationMs)
            .min()
            .orElse(0);
        long maxTime = events.stream()
            .mapToLong(ApiAnalyticsEvent::getDurationMs)
            .max()
            .orElse(0);

        Map<Integer, Long> statusDistribution = events.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ApiAnalyticsEvent::getStatusCode,
                java.util.stream.Collectors.counting()
            ));

        return new PartnerMetrics(
            partnerId, total, success, errors, serverErrors,
            avgTime, minTime, maxTime, statusDistribution
        );
    }

    private EndpointMetrics calculateEndpointMetrics(String endpoint, HttpMethod method,
                                                      List<ApiAnalyticsEvent> events) {
        long total = events.size();
        long success = events.stream().filter(ApiAnalyticsEvent::isSuccess).count();
        long errors = events.stream().filter(ApiAnalyticsEvent::isError).count();

        double avgTime = events.stream()
            .mapToLong(ApiAnalyticsEvent::getDurationMs)
            .average()
            .orElse(0);
        long minTime = events.stream()
            .mapToLong(ApiAnalyticsEvent::getDurationMs)
            .min()
            .orElse(0);
        long maxTime = events.stream()
            .mapToLong(ApiAnalyticsEvent::getDurationMs)
            .max()
            .orElse(0);

        Map<Integer, Long> statusDistribution = events.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ApiAnalyticsEvent::getStatusCode,
                java.util.stream.Collectors.counting()
            ));

        return new EndpointMetrics(
            endpoint, method, total, success, errors,
            avgTime, minTime, maxTime, statusDistribution
        );
    }
}
