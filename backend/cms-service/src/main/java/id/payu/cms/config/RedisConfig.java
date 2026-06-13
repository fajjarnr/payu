package id.payu.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for caching
 * Supports Red Hat Data Grid in RESP mode
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.cache.ttl:10m}")
    private Duration cacheTtl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        if (redisUsername != null && !redisUsername.isEmpty()) {
            config.setUsername(redisUsername);
        }
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(cacheTtl)
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(buildValueSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }

    /**
     * READY-001: Build a value serializer that preserves the runtime type of cached values,
     * including top-level {@link java.util.List} payloads.
     *
     * <p>{@code @Cacheable} hits are served via Spring's
     * {@link org.springframework.cache.interceptor.CacheInterceptor}, which calls
     * {@code serializer.deserialize(byte[])} without a target type hint. The
     * default {@code GenericJackson2JsonRedisSerializer} + plain {@code ObjectMapper}
     * produced {@code LinkedHashMap} payloads and caused
     * {@code ClassCastException: LinkedHashMap cannot be cast to ContentResponse}
     * on every cache hit (E2E-2026-06-13-06).</p>
     *
     * <p>The {@link TypedJsonRedisSerializer} writes the fully qualified type name
     * as a prefix on the serialized JSON ({@code <type>|<json>}), so the
     * original concrete class — including a top-level {@code List<ContentResponse>}
     * returned by {@code getActiveContentByType} — can be reconstructed on every
     * cache hit without relying on Jackson polymorphic typing.</p>
     */
    org.springframework.data.redis.serializer.RedisSerializer<Object> buildValueSerializer() {
        return new TypedJsonRedisSerializer();
    }
}
