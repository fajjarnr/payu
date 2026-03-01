package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Spring-managed thread pool configuration for Cache operations.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Bounded thread pools to prevent OOM under load</li>
 *   <li>Micrometer metrics for monitoring</li>
 *   <li>Graceful shutdown support (awaitTermination = 30s)</li>
 *   <li>Named threads for easier debugging</li>
 * </ul>
 *
 * <p>Replaces static {@code Executors.newCachedThreadPool()} in CacheService
 * which caused thread leaks on pod restart.</p>
 *
 * @see id.payu.cache.service.CacheService
 * @since IMP-068
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "payu.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheThreadPoolConfig {

    /**
     * Thread name prefix for cache refresh operations.
     */
    private static final String CACHE_REFRESH_THREAD_PREFIX = "cache-refresh-";

    /**
     * Graceful shutdown timeout in seconds.
     */
    private static final int AWAIT_TERMINATION_SECONDS = 30;

    /**
     * Cache refresh executor for stale-while-revalidate pattern.
     *
     * <p>This executor is used for asynchronous cache refresh operations when
     * stale data is served while fresh data is being fetched in the background.</p>
     *
     * @param properties Cache properties for configuration
     * @param meterRegistry Micrometer registry for metrics
     * @return Executor configured for cache refresh operations
     */
    @Bean(name = "cacheRefreshExecutor")
    @ConditionalOnMissingBean(name = "cacheRefreshExecutor")
    public Executor cacheRefreshExecutor(CacheProperties properties, MeterRegistry meterRegistry) {
        int refreshThreadPoolSize = properties.getStaleWhileRevalidate().getRefreshThreadPoolSize();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(refreshThreadPoolSize);
        executor.setMaxPoolSize(refreshThreadPoolSize * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix(CACHE_REFRESH_THREAD_PREFIX);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.setTaskDecorator(new CacheTaskDecorator());
        executor.initialize();

        // Register Micrometer metrics
        ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor.getThreadPoolExecutor(),
                "cache.refresh.executor",
                java.util.Collections.emptyList()
        );

        log.info("Cache refresh executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity=100",
                refreshThreadPoolSize, refreshThreadPoolSize * 2);

        return executor;
    }

    /**
     * Task decorator to propagate context (e.g., MDC, tracing) to async cache refresh threads.
     */
    private static class CacheTaskDecorator implements org.springframework.core.task.TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture current context
            org.slf4j.MDC.MDCCopyContext mdcContext = org.slf4j.MDC.getCopyOfContextMap();

            return () -> {
                try {
                    if (mdcContext != null) {
                        org.slf4j.MDC.setContextMap(mdcContext);
                    }
                    runnable.run();
                } finally {
                    org.slf4j.MDC.clear();
                }
            };
        }
    }
}
