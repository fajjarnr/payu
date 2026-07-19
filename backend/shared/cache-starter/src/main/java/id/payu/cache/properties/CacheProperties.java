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
 * <p>Uses native Red Hat Data Grid (Infinispan) Hot Rod.</p>
 *
 * <p>Configuration example:</p>
 * <pre>
 * payu:
 *   cache:
 *     enabled: true
 *     provider: hotrod
 *     hotrod:
 *       server-list: payu-cache.payu-infra.svc.cluster.local:11222
 *       use-ssl: true
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
     * Cache provider mode. Native Hot Rod is the only supported protocol.
     */
    private String provider = "hotrod";

    /**
     * Red Hat Data Grid Hot Rod native client configuration properties.
     */
    private HotRod hotrod = new HotRod();

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
    public static class HotRod {
        /**
         * Named Data Grid cache used by PayU services.
         */
        private String cacheName = "payu";

        /**
         * Server list (host:port, comma separated). Default: "localhost:11222".
         */
        private String serverList = "localhost:11222";

        /**
         * Data Grid authentication username.
         */
        private String authUsername;

        /**
         * Data Grid authentication password.
         */
        private String authPassword;

        /**
         * Data Grid authentication realm.
         */
        private String authRealm = "default";

        /**
         * SASL authentication mechanism.
         */
        private String saslMechanism = "DIGEST-SHA-256";

        /**
         * Enable SSL.
         */
        private boolean useSsl = false;

        /**
         * PKCS12 trust store used to verify the Data Grid endpoint certificate.
         */
        private String trustStoreFileName;

        /**
         * Password for the Data Grid endpoint trust store.
         */
        private String trustStorePassword;

        /**
         * Trust store format.
         */
        private String trustStoreType = "PKCS12";

        /**
         * Optional PKCS12 client key store for Data Grid mTLS.
         */
        private String keyStoreFileName;

        /**
         * Password for the Data Grid client key store.
         */
        private String keyStorePassword;

        /**
         * Client key store format.
         */
        private String keyStoreType = "PKCS12";

        /**
         * Optional client key alias for Data Grid mTLS.
         */
        private String keyAlias;

        /**
         * TLS SNI hostname for the Data Grid endpoint.
         */
        private String sniHostName;

        /**
         * Verify the Data Grid certificate hostname.
         */
        private boolean hostnameValidation = true;

        /**
         * Client intelligence strategy.
         */
        private String clientIntelligence = "HASH_DISTRIBUTION_AWARE";
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
         * Enable local cache fallback when Data Grid is unavailable.
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
        private String topic = "payu.cache.invalidation.v1";

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
