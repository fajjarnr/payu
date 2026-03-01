package id.payu.saga.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring-managed thread pool configuration for Saga orchestration.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Bounded thread pools to prevent OOM under load</li>
 *   <li>Micrometer metrics for monitoring</li>
 *   <li>Graceful shutdown support (awaitTermination = 60s)</li>
 *   <li>Named threads for easier debugging</li>
 * </ul>
 *
 * <p>Replaces static {@code Executors.newCachedThreadPool()} which caused thread leaks.</p>
 *
 * @see id.payu.saga.orchestrator.SagaOrchestrator
 * @since IMP-068
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "payu.saga", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SagaThreadPoolConfig {

    /**
     * Core pool size for saga execution.
     * Default: 4 threads.
     */
    private static final int CORE_POOL_SIZE = 4;

    /**
     * Maximum pool size for saga execution.
     * Default: 16 threads.
     */
    private static final int MAX_POOL_SIZE = 16;

    /**
     * Queue capacity for pending saga tasks.
     * Default: 100 (bounded to prevent OOM).
     */
    private static final int QUEUE_CAPACITY = 100;

    /**
     * Thread name prefix for saga workers.
     */
    private static final String SAGA_THREAD_PREFIX = "saga-";

    /**
     * Thread name prefix for saga retry scheduler.
     */
    private static final String RETRY_THREAD_PREFIX = "saga-retry-";

    /**
     * Graceful shutdown timeout in seconds.
     */
    private static final int AWAIT_TERMINATION_SECONDS = 60;

    /**
     * Saga task executor for asynchronous saga execution.
     *
     * <p>Configured with bounded queue to prevent memory exhaustion under load.</p>
     *
     * @param meterRegistry Micrometer registry for metrics
     * @return ThreadPoolTaskExecutor configured for saga execution
     */
    @Bean(name = "sagaTaskExecutor")
    @ConditionalOnMissingBean(name = "sagaTaskExecutor")
    public ThreadPoolTaskExecutor sagaTaskExecutor(MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(SAGA_THREAD_PREFIX);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.setTaskDecorator(new SagaTaskDecorator());
        executor.initialize();

        // Register Micrometer metrics
        ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor.getThreadPoolExecutor(),
                "saga.executor",
                java.util.Collections.emptyList()
        );

        log.info("Saga task executor configured: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);

        return executor;
    }

    /**
     * Scheduled executor service for saga retry operations.
     *
     * <p>Used for non-blocking retry delays with exponential backoff.</p>
     *
     * @param meterRegistry Micrometer registry for metrics
     * @return ScheduledExecutorService configured for saga retries
     */
    @Bean(name = "sagaRetryScheduler")
    @ConditionalOnMissingBean(name = "sagaRetryScheduler")
    public ScheduledExecutorService sagaRetryScheduler(MeterRegistry meterRegistry) {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(
                2,
                new SagaThreadFactory(RETRY_THREAD_PREFIX)
        );
        scheduler.setRemoveOnCancelPolicy(true);

        // Register Micrometer metrics
        ExecutorServiceMetrics.monitor(
                meterRegistry,
                scheduler,
                "saga.retry.scheduler",
                java.util.Collections.emptyList()
        );

        log.info("Saga retry scheduler configured with 2 threads");

        return scheduler;
    }

    /**
     * Custom thread factory for saga threads with meaningful names.
     */
    private static class SagaThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        SagaThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * Task decorator to propagate context (e.g., MDC, tracing) to async threads.
     */
    private static class SagaTaskDecorator implements org.springframework.core.task.TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture current MDC context
            Map<String, String> mdcContext = org.slf4j.MDC.getCopyOfContextMap();

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
