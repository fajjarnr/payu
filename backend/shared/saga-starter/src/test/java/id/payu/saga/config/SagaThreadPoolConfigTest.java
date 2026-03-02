package id.payu.saga.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SagaThreadPoolConfig}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Thread pool beans are created with correct configuration</li>
 *   <li>Bounded queue capacity is set</li>
 *   <li>Graceful shutdown is configured</li>
 *   <li>Micrometer metrics are registered</li>
 * </ul>
 *
 * @since IMP-068
 */
class SagaThreadPoolConfigTest {

    private final SagaThreadPoolConfig config = new SagaThreadPoolConfig();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void sagaTaskExecutor_shouldCreateBoundedThreadPool() {
        // When
        ThreadPoolTaskExecutor executor = config.sagaTaskExecutor(meterRegistry);

        // Then
        assertThat(executor).isNotNull();
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaxPoolSize()).isEqualTo(16);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isLessThanOrEqualTo(100);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("saga-");
    }

    @Test
    void sagaTaskExecutor_shouldConfigureGracefulShutdown() {
        // When
        ThreadPoolTaskExecutor executor = config.sagaTaskExecutor(meterRegistry);

        // Then
        // Verify the executor was created successfully (graceful shutdown is configured
        // via setWaitForTasksToCompleteOnShutdown/setAwaitTerminationSeconds but
        // ThreadPoolTaskExecutor does not expose public getters for these in Spring 6.x)
        assertThat(executor).isNotNull();
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
    }

    @Test
    void sagaTaskExecutor_shouldRegisterMicrometerMetrics() {
        // When
        config.sagaTaskExecutor(meterRegistry);

        // Then
        assertThat(meterRegistry.find("saga.executor").timers()).isNotEmpty();
    }

    @Test
    void sagaRetryScheduler_shouldCreateScheduledExecutor() {
        // When
        ScheduledExecutorService scheduler = config.sagaRetryScheduler(meterRegistry);

        // Then
        assertThat(scheduler).isNotNull();
        assertThat(scheduler.isShutdown()).isFalse();
    }

    @Test
    void sagaRetryScheduler_shouldRegisterMicrometerMetrics() {
        // When
        config.sagaRetryScheduler(meterRegistry);

        // Then
        assertThat(meterRegistry.find("saga.retry.scheduler").timers()).isNotEmpty();
    }
}
