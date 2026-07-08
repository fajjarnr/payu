package id.payu.cache.config;

import id.payu.cache.properties.CacheProperties;
import id.payu.cache.serializer.TypedJsonRedisSerializer;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for Redis/Red Hat Data Grid cache with support for:
 * - Standalone, Sentinel, and Cluster modes
 * - Stale-while-revalidate pattern
 * - Custom TTL per cache
 * - Connection pooling
 * - Automatic reconnection
 *
 * <p><b>Red Hat Data Grid Compatibility:</b> This configuration uses the Lettuce client
 * which communicates via the RESP (Redis Serialization Protocol). Red Hat Data Grid
 * supports RESP mode, making it a drop-in replacement. All serialization uses
 * {@code GenericJackson2JsonRedisSerializer} (JSON-based) ensuring cross-platform
 * portability — no Java-specific serialization that would break with Data Grid.</p>
 */
@Slf4j
@AutoConfiguration(before = DataRedisAutoConfiguration.class)
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "payu.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RedisCacheConfig {

    private final CacheProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(properties.getRedis().getCommandTimeout())
                .shutdownTimeout(Duration.ofMillis(100))
                .clientOptions(createClientOptions())
                .build();

        if (properties.getRedis().isCluster()) {
            return createClusterConnectionFactory(clientConfig);
        } else if (properties.getRedis().getSentinelMaster() != null) {
            return createSentinelConnectionFactory(clientConfig);
        } else {
            return createStandaloneConnectionFactory(clientConfig);
        }
    }

    private ClientOptions createClientOptions() {
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(properties.getRedis().getTimeout())
                .build();

        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .fixedTimeout(properties.getRedis().getCommandTimeout())
                .build();

        return ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(timeoutOptions)
                .protocolVersion(ProtocolVersion.RESP2)
                .autoReconnect(true)
                .pingBeforeActivateConnection(false)
                .build();
    }

    private LettuceConnectionFactory createStandaloneConnectionFactory(LettuceClientConfiguration clientConfig) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getRedis().getHost());
        config.setPort(properties.getRedis().getPort());

        if (properties.getRedis().getUsername() != null && !properties.getRedis().getUsername().isBlank()) {
            config.setUsername(properties.getRedis().getUsername());
        }
        if (properties.getRedis().getPassword() != null) {
            config.setPassword(properties.getRedis().getPassword());
        }
        config.setDatabase(properties.getRedis().getDatabase());

        log.info("Configuring standalone Redis connection to {}:{}",
                properties.getRedis().getHost(), properties.getRedis().getPort());

        return new LettuceConnectionFactory(config, clientConfig);
    }

    private LettuceConnectionFactory createSentinelConnectionFactory(LettuceClientConfiguration clientConfig) {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.setMaster(properties.getRedis().getSentinelMaster());
        config.setSentinelPassword(properties.getRedis().getPassword());

        // Parse sentinel nodes from clusterNodes property
        if (properties.getRedis().getClusterNodes() != null) {
            String[] nodes = properties.getRedis().getClusterNodes().split(",");
            for (String node : nodes) {
                String[] parts = node.trim().split(":");
                config.sentinel(parts[0], Integer.parseInt(parts[1]));
            }
        }

        log.info("Configuring Redis Sentinel connection for master: {}",
                properties.getRedis().getSentinelMaster());

        return new LettuceConnectionFactory(config, clientConfig);
    }

    private LettuceConnectionFactory createClusterConnectionFactory(LettuceClientConfiguration clientConfig) {
        RedisClusterConfiguration config = new RedisClusterConfiguration();

        if (properties.getRedis().getClusterNodes() != null) {
            String[] nodes = properties.getRedis().getClusterNodes().split(",");
            for (String node : nodes) {
                String[] parts = node.trim().split(":");
                config.clusterNode(parts[0], Integer.parseInt(parts[1]));
            }
        }

        if (properties.getRedis().getPassword() != null) {
            config.setPassword(properties.getRedis().getPassword());
        }

        log.info("Configuring Redis Cluster connection");

        // Enable adaptive topology refresh for cluster
        ClusterTopologyRefreshOptions topologyOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofMinutes(5))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        ClusterClientOptions clusterOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyOptions)
                .protocolVersion(ProtocolVersion.RESP2)
                .autoReconnect(true)
                .build();

        LettuceClientConfiguration clusterClientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(properties.getRedis().getCommandTimeout())
                .clientOptions(clusterOptions)
                .build();

        return new LettuceConnectionFactory(config, clusterClientConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> valueSerializer = buildValueSerializer(properties);
        log.info("Using {} as cache value serializer", valueSerializer.getClass().getSimpleName());

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .entryTtl(properties.getDefaultTtl())
                .disableCachingNullValues();

        // Build per-cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        properties.getCaches().forEach((cacheName, cacheConfig) -> {
            RedisCacheConfiguration config = defaultConfig;

            if (cacheConfig.getTtl() != null) {
                config = config.entryTtl(cacheConfig.getTtl());
            }

            // Enable caching null values for specific caches if needed
            // (e.g., to prevent cache stampede)
            // config = config.disableCachingNullValues();

            cacheConfigurations.put(cacheName, config);

            log.debug("Configured cache '{}' with TTL: {}",
                    cacheName, cacheConfig.getTtl() != null ? cacheConfig.getTtl() : properties.getDefaultTtl());
        });

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * NEW-003: Select the value serializer used by {@link CacheManager} (and therefore
     * every {@code @Cacheable} hit in the service).
     *
     * <p>The typed serializer preserves the runtime class on the wire, including
     * for top-level {@link java.util.List} payloads.</p>
     */
    static RedisSerializer<Object> buildValueSerializer(CacheProperties properties) {
        String mode = properties.getSerializer();
        if (mode != null && !"typed".equalsIgnoreCase(mode)
                && !TypedJsonRedisSerializer.class.getSimpleName().equals(mode)) {
            log.warn("Unsupported payu.cache.serializer '{}'; using typed serializer", mode);
        }
        return new TypedJsonRedisSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean(name = "payuCacheRedisTemplate")
    public RedisTemplate<String, Object> payuCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // NEW-003: Use the same typed serializer as CacheManager so the wire
        // format is consistent across both @Cacheable and direct RedisTemplate
        // usage (DistributedCacheService, CacheWarmingService, etc).
        RedisSerializer<Object> valueSerializer = buildValueSerializer(properties);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
