package id.payu.cache.config;

import id.payu.cache.aspect.CacheWithTTLAspect;
import id.payu.cache.service.CacheService;
import id.payu.cache.service.DistributedCacheService;
import id.payu.cache.service.LocalCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Main auto-configuration for PayU Cache Starter.
 *
 * <p>Registers:</p>
 * <ul>
 *   <li>CacheService - Primary cache service with distributed + local fallback</li>
 *   <li>DistributedCacheService - Redis-based cache service</li>
 *   <li>LocalCacheService - Caffeine-based local cache fallback</li>
 *   <li>CacheWithTTLAspect - Aspect for @CacheWithTTL annotation</li>
 *   <li>Async refresh executor for stale-while-revalidate</li>
 * </ul>
 */
@AutoConfiguration
@RequiredArgsConstructor
@EnableAspectJAutoProxy
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "payu.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheAutoConfiguration {

    private final CacheProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public LocalCacheService localCacheService() {
        return new LocalCacheService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedCacheService distributedCacheService(
            RedisConnectionFactory connectionFactory) {
        return new DistributedCacheService(connectionFactory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheService cacheService(
            DistributedCacheService distributedCacheService,
            LocalCacheService localCacheService) {
        return new CacheService(distributedCacheService, localCacheService, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheWithTTLAspect cacheWithTTLAspect(CacheService cacheService) {
        return new CacheWithTTLAspect(cacheService);
    }

    @Bean(name = "cacheRefreshExecutor")
    @ConditionalOnMissingBean(name = "cacheRefreshExecutor")
    public Executor cacheRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getStaleWhileRevalidate().getRefreshThreadPoolSize());
        executor.setMaxPoolSize(properties.getStaleWhileRevalidate().getRefreshThreadPoolSize() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-refresh-");
        executor.initialize();
        return executor;
    }
}
