package id.payu.cache.service;

import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cache warming service that pre-loads cache entries on application startup.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Warm-up specified cache keys on startup</li>
 *   <li>Async execution with configurable thread pool</li>
 *   <li>Metrics tracking for warm-up operations</li>
 *   <li>Configurable startup delay</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "payu.cache.cache-warming",
    name = "enabled",
    havingValue = "true"
)
public class CacheWarmingService implements ApplicationListener<ApplicationReadyEvent> {

    private final CacheService cacheService;
    private final CacheProperties properties;
    private final ThreadPoolTaskExecutor cacheWarmExecutor;

    private final io.micrometer.core.instrument.Counter warmedCounter;
    private final io.micrometer.core.instrument.Counter failedCounter;
    private final io.micrometer.core.instrument.Timer warmTimer;

    public CacheWarmingService(
            CacheService cacheService,
            CacheProperties properties,
            ThreadPoolTaskExecutor cacheWarmExecutor) {
        this.cacheService = cacheService;
        this.properties = properties;
        this.cacheWarmExecutor = cacheWarmExecutor;

        String prefix = properties.getMetrics().getPrefix();
        this.warmedCounter = Metrics.counter(prefix + ".warmed");
        this.failedCounter = Metrics.counter(prefix + ".warm.failed");
        this.warmTimer = Metrics.timer(prefix + ".warm");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!properties.getCacheWarming().isEnabled()) {
            log.info("Cache warming is disabled");
            return;
        }

        log.info("Scheduling cache warming with delay: {}",
            properties.getCacheWarming().getStartupDelay());

        // Schedule cache warming after configured delay
        if (properties.getCacheWarming().isAsync()) {
            CompletableFuture.runAsync(this::warmCache, cacheWarmExecutor);
        } else {
            warmCache();
        }
    }

    /**
     * Warm up cache entries based on configuration.
     */
    private void warmCache() {
        try {
            // Wait for configured delay
            TimeUnit.MILLISECONDS.sleep(
                properties.getCacheWarming().getStartupDelay().toMillis()
            );
        } catch (InterruptedException e) {
            log.warn("Cache warming delay interrupted", e);
            Thread.currentThread().interrupt();
            return;
        }

        log.info("Starting cache warming...");
        io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start();

        try {
            int totalWarmed = 0;
            int totalFailed = 0;

            // Warm caches configured for each cache region
            for (var entry : properties.getCaches().entrySet()) {
                String cacheName = entry.getKey();
                var cacheConfig = entry.getValue();

                if (cacheConfig.getWarmKeys().isEmpty()) {
                    continue;
                }

                log.info("Warming cache '{}' with {} keys", cacheName, cacheConfig.getWarmKeys().size());

                for (String key : cacheConfig.getWarmKeys()) {
                    try {
                        warmCacheKey(cacheName, key);
                        totalWarmed++;
                        warmedCounter.increment();
                    } catch (Exception e) {
                        log.warn("Failed to warm key '{}' in cache '{}': {}", key, cacheName, e.getMessage());
                        totalFailed++;
                        failedCounter.increment();
                    }
                }
            }

            sample.stop(warmTimer);
            log.info("Cache warming completed: {} entries warmed, {} failed", totalWarmed, totalFailed);
        } catch (Exception e) {
            log.error("Error during cache warming", e);
            failedCounter.increment();
        }
    }

    /**
     * Warm a specific cache key.
     * This method can be overridden in subclasses to implement custom warm-up logic.
     */
    protected void warmCacheKey(String cacheName, String key) {
        // Default implementation: just check if the key exists
        // Actual warm-up logic should be provided by the application
        log.debug("Warming key '{}' in cache '{}'", key, cacheName);

        // Trigger cache load via CacheService
        // The actual data loading should be handled by CacheWithTTLAspect fallback suppliers
    }

    /**
     * Manually trigger cache warming for a specific cache name.
     */
    public void warmCache(String cacheName) {
        log.info("Manual cache warming triggered for '{}'", cacheName);
        var cacheConfig = properties.getCaches().get(cacheName);

        if (cacheConfig == null || cacheConfig.getWarmKeys().isEmpty()) {
            log.warn("No warm keys configured for cache '{}'", cacheName);
            return;
        }

        for (String key : cacheConfig.getWarmKeys()) {
            try {
                warmCacheKey(cacheName, key);
                warmedCounter.increment();
            } catch (Exception e) {
                log.warn("Failed to warm key '{}' in cache '{}': {}", key, cacheName, e.getMessage());
                failedCounter.increment();
            }
        }
    }
}
