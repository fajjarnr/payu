package id.payu.commons.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cache.service.DistributedAtomicCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Boot Auto-Configuration for Idempotency support.
 * <p>
 * This auto-configuration sets up all necessary beans for idempotency handling:
 * <ul>
 *   <li>{@link IdempotencyRepository} - distributed-cache repository implementation</li>
 *   <li>{@link IdempotencyService} - Core idempotency service</li>
 *   <li>{@link IdempotencyInterceptor} - HTTP interceptor for automatic handling</li>
 *   <li>WebMvc configuration for interceptor registration</li>
 * </ul>
 * <p>
 * Configuration properties (prefix: <code>payu.idempotency</code>):
 * <pre>
 * payu:
 *   idempotency:
 *     enabled: true                    # Enable/disable idempotency
 *     ttl-hours: 24                    # Default TTL for entries
 *     header-name: Idempotency-Key     # Header name for idempotency key
 *     redis:
 *       key-prefix: idempotency        # Redis key prefix
 *     interceptor:
 *       path-pattern: /api/**          # Path pattern for interceptor
 * </pre>
 * <p>
 * To disable auto-configuration:
 * <pre>
 * payu.idempotency.enabled=false
 * </pre>
 *
 * @see IdempotencyProperties
 * @see IdempotencyService
 * @see IdempotencyInterceptor
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({DistributedAtomicCache.class, ObjectMapper.class})
@ConditionalOnProperty(prefix = "payu.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

    /**
     * Creates the distributed-cache idempotency repository.
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyRepository.class)
    @ConditionalOnBean(DistributedAtomicCache.class)
    public IdempotencyRepository idempotencyRepository(
            DistributedAtomicCache distributedCache,
            ObjectMapper objectMapper,
            IdempotencyProperties properties) {

        log.info("Initializing DistributedCacheIdempotencyRepository with prefix: {}",
                properties.getRedis().getKeyPrefix());
        return new DistributedCacheIdempotencyRepository(distributedCache, objectMapper, properties);
    }

    /**
     * Creates the idempotency service.
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyService.class)
    @ConditionalOnBean(IdempotencyRepository.class)
    public IdempotencyService idempotencyService(
            IdempotencyRepository repository,
            ObjectMapper objectMapper) {

        log.info("Initializing IdempotencyService");
        return new IdempotencyService(repository, objectMapper);
    }

    /**
     * Creates the idempotency interceptor.
     */
    @Bean
    @ConditionalOnMissingBean(IdempotencyInterceptor.class)
    @ConditionalOnBean(IdempotencyService.class)
    public IdempotencyInterceptor idempotencyInterceptor(
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {

        log.info("Initializing IdempotencyInterceptor");
        return new IdempotencyInterceptor(idempotencyService, objectMapper);
    }

    @Bean
    @ConditionalOnBean(IdempotencyInterceptor.class)
    public FilterRegistrationBean<IdempotencyRequestBodyFilter> idempotencyRequestBodyFilter() {
        FilterRegistrationBean<IdempotencyRequestBodyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyRequestBodyFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    /**
     * WebMvc configuration for registering the idempotency interceptor.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebMvcConfigurer.class)
    public static class IdempotencyWebMvcConfiguration {

        @Bean
        @ConditionalOnBean(IdempotencyInterceptor.class)
        public WebMvcConfigurer idempotencyWebMvcConfigurer(
                IdempotencyInterceptor idempotencyInterceptor,
                IdempotencyProperties properties) {

            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    String pathPattern = properties.getInterceptor().getPathPattern();
                    log.info("Registering IdempotencyInterceptor for path pattern: {}", pathPattern);
                    registry.addInterceptor(idempotencyInterceptor)
                            .addPathPatterns(pathPattern);
                }
            };
        }
    }
}
