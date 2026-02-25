package id.payu.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for PayU Cache Starter.
 *
 * <p>Supports both <b>Redis</b> and <b>Red Hat Data Grid</b> (Infinispan) in RESP protocol mode.
 * Data Grid operates as a drop-in replacement for Redis when configured in RESP mode —
 * simply point {@code payu.cache.redis.host} and {@code payu.cache.redis.port} to your
 * Data Grid endpoint.</p>
 *
 * <p>Configuration example (Redis):</p>
 * <pre>
 * payu:
 *   cache:
 *     enabled: true
 *     redis:
 *       host: localhost
 *       port: 6379
 *       timeout: 5s
 *     default-ttl: 5m
 *     stale-while-revalidate:
 *       enabled: true
 *       soft-ttl-multiplier: 0.5
 *     cache-warming:
 *       enabled: true
 *       startup-delay: 10s
 *     invalidation:
 *       enabled: true
 *       kafka-topic: cache-invalidation
 *     caches:
 *       account:
 *         ttl: 10m
 *         stale-while-revalidate: true
 *       balance:
 *         ttl: 30s
 *         stale-while-revalidate: true
 * </pre>
 *
 * <p>Configuration example (Red Hat Data Grid in RESP mode):</p>
 * <pre>
 * payu:
 *   cache:
 *     enabled: true
 *     redis:
 *       host: datagrid-resp.payu-infra.svc.cluster.local  # Data Grid RESP endpoint
 *       port: 11222                                        # Data Grid default port
 *       password: ${DATAGRID_PASSWORD}
 *       ssl: true                                          # Recommended for Data Grid
 *       timeout: 5s
 *     default-ttl: 5m
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "payu.cache")
public class CacheProperties {

    /**
     * Enable/disable caching.
     */
    private boolean enabled = true;

    /**
     * Redis / Red Hat Data Grid connection configuration.
     * Uses Lettuce client which communicates via RESP protocol — compatible with both
     * Redis and Data Grid. Point host/port to either Redis or Data Grid RESP endpoint.
     */
    private Redis redis = new Redis();

    /**
     * Default TTL for cache entries.
     */
    private Duration defaultTtl = Duration.ofMinutes(5);

    /**
     * Stale-while-revalidate configuration.
     */
    private StaleWhileRevalidate staleWhileRevalidate = new StaleWhileRevalidate();

    /**
     * Per-cache configuration.
     */
    private Map<String, CacheConfig> caches = new HashMap<>();

    /**
     * Local fallback cache configuration.
     */
    private LocalCache localCache = new LocalCache();

    /**
     * Cache warming configuration.
     */
    private CacheWarming cacheWarming = new CacheWarming();

    /**
     * Cache invalidation via Kafka configuration.
     */
    private Invalidation invalidation = new Invalidation();

    /**
     * Metrics configuration.
     */
    private Metrics metrics = new Metrics();

    @Data
    public static class Redis {
        /**
         * Redis host.
         */
        private String host = "localhost";

        /**
         * Redis port.
         */
        private int port = 6379;

        /**
         * Redis password (optional).
         */
        private String password;

        /**
         * Redis database index.
         */
        private int database = 0;

        /**
         * Connection timeout.
         */
        private Duration timeout = Duration.ofSeconds(5);

        /**
         * Command timeout.
         */
        private Duration commandTimeout = Duration.ofSeconds(3);

        /**
         * Connection pool size.
         */
        private int poolSize = 10;

        /**
         * Enable SSL.
         */
        private boolean ssl = false;

        /**
         * Enable cluster mode.
         */
        private boolean cluster = false;

        /**
         * Cluster nodes (comma-separated).
         */
        private String clusterNodes;

        /**
         * Sentinel master name.
         */
        private String sentinelMaster;
    }

    @Data
    public static class StaleWhileRevalidate {
        /**
         * Enable stale-while-revalidate pattern.
         */
        private boolean enabled = true;

        /**
         * Multiplier for soft TTL (soft TTL = hard TTL * multiplier).
         * Default 0.5 means soft TTL is 50% of hard TTL.
         */
        private double softTtlMultiplier = 0.5;

        /**
         * Thread pool size for async refresh.
         */
        private int refreshThreadPoolSize = 4;
    }

    @Data
    public static class CacheConfig {
        /**
         * TTL for this cache.
         */
        private Duration ttl;

        /**
         * Enable stale-while-revalidate for this cache.
         */
        private Boolean staleWhileRevalidate;

        /**
         * Custom soft TTL multiplier.
         */
        private Double softTtlMultiplier;

        /**
         * Enable local cache fallback.
         */
        private Boolean localFallback;

        /**
         * Cache keys to warm on startup.
         */
        private List<String> warmKeys = new ArrayList<>();
    }

    @Data
    public static class LocalCache {
        /**
         * Enable local cache fallback when Redis is unavailable.
         */
        private boolean enabled = true;

        /**
         * Maximum size of local cache.
         */
        private long maxSize = 1000;

        /**
         * TTL for local cache entries.
         */
        private Duration ttl = Duration.ofMinutes(1);

        /**
         * Enable cache stats recording.
         */
        private boolean recordStats = true;
    }

    @Data
    public static class CacheWarming {
        /**
         * Enable cache warming on startup.
         */
        private boolean enabled = false;

        /**
         * Delay before starting cache warming.
         */
        private Duration startupDelay = Duration.ofSeconds(10);

        /**
         * Enable async cache warming.
         */
        private boolean async = true;

        /**
         * Thread pool size for cache warming.
         */
        private int threadPoolSize = 4;
    }

    @Data
    public static class Invalidation {
        /**
         * Enable cache invalidation via Kafka.
         */
        private boolean enabled = false;

        /**
         * Kafka topic for cache invalidation events.
         */
        private String topic = "cache-invalidation";

        /**
         * Consumer group for cache invalidation.
         */
        private String consumerGroup = "cache-invalidation-group";

        /**
         * Enable auto-commit for invalidation events.
         */
        private boolean autoCommit = true;
    }

    @Data
    public static class Metrics {
        /**
         * Enable cache metrics.
         */
        private boolean enabled = true;

        /**
         * Metrics prefix.
         */
        private String prefix = "cache";

        /**
         * Enable percentile metrics.
         */
        private boolean percentiles = true;

        /**
         * Enable histogram metrics.
         */
        private boolean histogram = true;
    }
}
