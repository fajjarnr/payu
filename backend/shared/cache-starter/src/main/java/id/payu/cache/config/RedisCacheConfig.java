package id.payu.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.payu.cache.properties.CacheProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for Redis cache with support for:
 * - Standalone, Sentinel, and Cluster modes
 * - Stale-while-revalidate pattern
 * - Custom TTL per cache
 * - Connection pooling
 * - Automatic reconnection
 */
@Slf4j
@AutoConfiguration
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
                .autoReconnect(true)
                .build();
    }

    private LettuceConnectionFactory createStandaloneConnectionFactory(LettuceClientConfiguration clientConfig) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getRedis().getHost());
        config.setPort(properties.getRedis().getPort());

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
        // Create ObjectMapper for JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
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

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
