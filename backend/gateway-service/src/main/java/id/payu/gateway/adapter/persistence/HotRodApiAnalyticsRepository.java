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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /**
     * GW-CACHE-001: events per day are sharded across fixed-size {@code analytics:events:<date>:<shard>}
     * keys, tracked by an {@code analytics:index:<date>} key. A single unbounded day list grew past
     * the Hot Rod message limit, the server closed the connection (ISPN005064) and flushes were
     * lost. 200 events x ~300 B is ~60 KB per shard value — wide margin below 10 MB.
     */
    private static final int MAX_EVENTS_PER_SHARD = 200;

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
            return persistDay(dayOf(event.getTimestamp()), List.of(json));
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

        Map<String, List<String>> eventsByDay = new LinkedHashMap<>();

        for (ApiAnalyticsEvent event : events) {
            try {
                String json = objectMapper.writeValueAsString(event);
                eventsByDay.computeIfAbsent(dayOf(event.getTimestamp()), k -> new ArrayList<>()).add(json);
            } catch (JsonProcessingException e) {
                Log.warnf(e, "Failed to serialize event: %s", event.getId());
            }
        }

        Uni<Void> result = Uni.createFrom().voidItem();

        for (Map.Entry<String, List<String>> entry : eventsByDay.entrySet()) {
            result = result.chain(() ->
                persistDay(entry.getKey(), entry.getValue())
            );
        }

        return result;
    }

    @Override
    public Multi<ApiAnalyticsEvent> findByPartnerId(String partnerId, Instant from, Instant to) {
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
        // This is a simplified implementation
        return Uni.createFrom().item(0L);
    }

    @Override
    public Uni<Void> aggregateDailyMetrics(Instant day) {
        // Store aggregated results with longer retention
        return Uni.createFrom().voidItem();
    }

    private Multi<ApiAnalyticsEvent> findByTimeRange(Instant from, Instant to) {
        List<String> dates = new ArrayList<>();
        LocalDate current = from.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate end = to.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        while (!current.isAfter(end)) {
            dates.add(current.toString());
            current = current.plusDays(1);
        }

        return Multi.createFrom().iterable(dates)
            .onItem().transformToMultiAndConcatenate(this::readDayJson)
            .onItem().transform(this::parseEvent)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(event ->
                !event.getTimestamp().isBefore(from) &&
                !event.getTimestamp().isAfter(to)
            );
    }

    /**
     * Reads all events of one day by walking the day's shard index
     * ({@code analytics:index:<date>}) and then every shard it references
     * ({@code analytics:events:<date>:<nnnnnn>}).
     */
    private Multi<String> readDayJson(String date) {
        return cache.readList(indexKey(date))
            .onItem().transformToMulti(indexEntries ->
                Multi.createFrom().iterable(shardIndices(indexEntries))
                    .onItem().transformToUniAndConcatenate(shard -> cache.readList(shardKey(date, shard)))
                    .onItem().transformToIterable(java.util.function.Function.identity())
            );
    }

    /**
     * GW-CACHE-001: appends events for one day across fixed-size shards and records the
     * open shard + occupancy in the day index, so no single Hot Rod value grows past the
     * ~10 MB message limit (the old single-list-per-day design broke the connection with
     * ISPN005064 and dropped flushes). No batches are ever skipped — everything that does
     * not fit in the open shard starts the next shard.
     *
     * <p>ponytail: flush is single-writer (scheduler lock) — if a racing writer ever
     * overfills a shard, the value only exceeds the cap, the append still succeeds.
     */
    private Uni<Void> persistDay(String date, List<String> jsonEvents) {
        List<List<String>> batches = partition(jsonEvents, MAX_EVENTS_PER_SHARD);
        if (batches.size() > 1) {
            Log.infof("Analytics day %s: %d events split into %d batches (max %d per batch)",
                date, jsonEvents.size(), batches.size(), MAX_EVENTS_PER_SHARD);
        }

        return cache.readList(indexKey(date))
            .chain(indexEntries -> {
                int[] state = openShardState(indexEntries);
                // per batch: {shard, countAfterWrite}
                List<int[]> plans = new ArrayList<>();
                for (List<String> batch : batches) {
                    if (state[1] + batch.size() > MAX_EVENTS_PER_SHARD) {
                        state[0]++;
                        state[1] = 0;
                    }
                    state[1] += batch.size();
                    plans.add(new int[] {state[0], state[1]});
                }

                Uni<Void> result = Uni.createFrom().voidItem();
                for (int i = 0; i < batches.size(); i++) {
                    final int[] plan = plans.get(i);
                    final List<String> batch = batches.get(i);
                    for (String json : batch) {
                        result = result.chain(() ->
                            cache.appendToList(shardKey(date, plan[0]), json,
                                Duration.ofDays(DETAILED_RETENTION_DAYS)));
                    }
                    // last index line wins: it declares the open shard and its occupancy
                    result = result.chain(() ->
                        cache.appendToList(indexKey(date), plan[0] + ":" + plan[1],
                            Duration.ofDays(DETAILED_RETENTION_DAYS)));
                }
                final int eventsWritten = jsonEvents.size();
                final int totalShards = state[0] + 1;
                return result.eventually(() ->
                    Log.infof("Persisted %d analytics events for day %s (shards: %d)", eventsWritten, date, totalShards));
            });
    }

    static List<List<String>> partition(List<String> events, int maxPerBatch) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < events.size(); i += maxPerBatch) {
            batches.add(events.subList(i, Math.min(i + maxPerBatch, events.size())));
        }
        return batches;
    }

    static List<Integer> shardIndices(List<String> indexEntries) {
        int[] state = openShardState(indexEntries);
        List<Integer> shards = new ArrayList<>();
        for (int i = 0; i <= state[0]; i++) {
            shards.add(i);
        }
        return shards;
    }

    /** Index lines are {@code "<shard>:<count>"}; the last line is the open shard state. */
    private static int[] openShardState(List<String> indexEntries) {
        if (indexEntries == null || indexEntries.isEmpty()) {
            return new int[] {0, 0};
        }
        String last = indexEntries.get(indexEntries.size() - 1);
        int sep = last.lastIndexOf(':');
        try {
            return new int[] {
                Integer.parseInt(last.substring(0, sep)),
                Integer.parseInt(last.substring(sep + 1))
            };
        } catch (RuntimeException e) {
            return new int[] {0, 0};
        }
    }

    private static String shardKey(String date, int shard) {
        return ANALYTICS_KEY_PREFIX + date + ":" + String.format("%06d", shard);
    }

    private static String indexKey(String date) {
        return ANALYTICS_INDEX_PREFIX + date;
    }

    private static String dayOf(Instant timestamp) {
        return timestamp.atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString();
    }

    private Optional<ApiAnalyticsEvent> parseEvent(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, ApiAnalyticsEvent.class));
        } catch (Exception e) {
            Log.warnf(e, "Failed to parse analytics event");
            return Optional.empty();
        }
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
