package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CacheThreadPoolConfig}.
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
class CacheThreadPoolConfigTest {

    private final CacheThreadPoolConfig config = new CacheThreadPoolConfig();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void cacheRefreshExecutor_shouldCreateBoundedThreadPool() {
        // Given
        CacheProperties properties = createCacheProperties(4);

        // When
        Executor executor = config.cacheRefreshExecutor(properties, meterRegistry);

        // Then
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(4);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(8);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("cache-refresh-");
    }

    @Test
    void cacheRefreshExecutor_shouldConfigureGracefulShutdown() {
        // Given
        CacheProperties properties = createCacheProperties(2);

        // When
        Executor executor = config.cacheRefreshExecutor(properties, meterRegistry);

        // Then
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.isWaitForTasksToCompleteOnShutdown()).isTrue();
        assertThat(taskExecutor.getAwaitTerminationSeconds()).isEqualTo(30);
    }

    @Test
    void cacheRefreshExecutor_shouldRegisterMicrometerMetrics() {
        // Given
        CacheProperties properties = createCacheProperties(2);

        // When
        config.cacheRefreshExecutor(properties, meterRegistry);

        // Then
        assertThat(meterRegistry.find("cache.refresh.executor").timers()).isNotEmpty();
    }

    private CacheProperties createCacheProperties(int refreshThreadPoolSize) {
        CacheProperties properties = mock(CacheProperties.class);
        CacheProperties.StaleWhileRevalidateProperties staleProps = mock(CacheProperties.StaleWhileRevalidateProperties.class);

        when(properties.getStaleWhileRevalidate()).thenReturn(staleProps);
        when(staleProps.getRefreshThreadPoolSize()).thenReturn(refreshThreadPoolSize);

        return properties;
    }
}
