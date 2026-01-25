package id.payu.resilience.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Auto-configuration for Resilience4j patterns
 */
@AutoConfiguration
@EnableConfigurationProperties(ResilienceProperties.class)
@ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry")
@ConditionalOnProperty(prefix = "payu.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ResilienceAutoConfiguration.class);

    private final ResilienceProperties properties;

    public ResilienceAutoConfiguration(ResilienceProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        log.info("Initializing Circuit Breaker Registry with custom configuration");

        Map<String, CircuitBreakerConfig> configs = new HashMap<>();

        // Default circuit breaker config
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getCircuitBreaker().getFailureRateThreshold())
                .waitDurationInOpenState(properties.getCircuitBreaker().getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(
                        properties.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowSize(properties.getCircuitBreaker().getSlidingWindowSize())
                .minimumNumberOfCalls(properties.getCircuitBreaker().getMinimumNumberOfCalls())
                .slidingWindowType(toCircuitBreakerWindowType(properties.getCircuitBreaker().getSlidingWindowType()))
                .automaticTransitionFromOpenToHalfOpenEnabled(
                        properties.getCircuitBreaker().isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .build();

        configs.put("default", defaultConfig);

        // Service-specific configurations
        if (properties.getServices() != null) {
            properties.getServices().forEach((serviceName, serviceConfig) -> {
                if (serviceConfig.getCircuitBreaker() != null) {
                    ResilienceProperties.CircuitBreaker cbConfig = serviceConfig.getCircuitBreaker();
                    CircuitBreakerConfig customConfig = CircuitBreakerConfig.custom()
                            .failureRateThreshold(cbConfig.getFailureRateThreshold())
                            .waitDurationInOpenState(cbConfig.getWaitDurationInOpenState())
                            .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedNumberOfCallsInHalfOpenState())
                            .slidingWindowSize(cbConfig.getSlidingWindowSize())
                            .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                            .slidingWindowType(toCircuitBreakerWindowType(cbConfig.getSlidingWindowType()))
                            .automaticTransitionFromOpenToHalfOpenEnabled(
                                    cbConfig.isAutomaticTransitionFromOpenToHalfOpenEnabled())
                            .build();

                    configs.put(serviceName, customConfig);
                    log.info("Configured circuit breaker for service: {}", serviceName);
                }
            });
        }

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(configs);

        // Register Micrometer metrics
        // Note: In Spring Boot 3 with resilience4j-spring-boot3, metrics are auto-registered
        // Manual metrics binding can be added later if needed
        // For now, skip manual metrics registration to allow compilation

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryRegistry retryRegistry(MeterRegistry meterRegistry) {
        log.info("Initializing Retry Registry with custom configuration");

        Map<String, RetryConfig> configs = new HashMap<>();

        // Default retry config
        IntervalFunction intervalFunction = properties.getRetry().isEnableExponentialBackoff()
                ? IntervalFunction.ofExponentialBackoff(
                        properties.getRetry().getWaitDuration().toMillis(),
                        properties.getRetry().getExponentialBackoffMultiplier())
                : IntervalFunction.of(properties.getRetry().getWaitDuration().toMillis());

        RetryConfig.Builder<?> builder = RetryConfig.custom()
                .maxAttempts(properties.getRetry().getMaxAttempts())
                .intervalFunction(intervalFunction)
                .retryOnException(e -> true);

        // Note: randomizeWait() is not available in Resilience4j 2.x
        // Randomization can be achieved through custom IntervalFunction if needed

        configs.put("default", builder.build());

        // Service-specific configurations
        if (properties.getServices() != null) {
            properties.getServices().forEach((serviceName, serviceConfig) -> {
                if (serviceConfig.getRetry() != null) {
                    ResilienceProperties.Retry rConfig = serviceConfig.getRetry();

                    IntervalFunction serviceIntervalFunction = rConfig.isEnableExponentialBackoff()
                            ? IntervalFunction.ofExponentialBackoff(
                                    rConfig.getWaitDuration().toMillis(),
                                    rConfig.getExponentialBackoffMultiplier())
                            : IntervalFunction.of(rConfig.getWaitDuration().toMillis());

                    RetryConfig.Builder<?> serviceBuilder = RetryConfig.custom()
                            .maxAttempts(rConfig.getMaxAttempts())
                            .intervalFunction(serviceIntervalFunction);

                    // Note: randomizeWait() is not available in Resilience4j 2.x

                    configs.put(serviceName, serviceBuilder.build());
                    log.info("Configured retry for service: {}", serviceName);
                }
            });
        }

        RetryRegistry registry = RetryRegistry.of(configs);

        // Register Micrometer metrics
        // Note: In Spring Boot 3 with resilience4j-spring-boot3, metrics are auto-registered
        // Manual metrics binding can be added later if needed

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public BulkheadRegistry bulkheadRegistry(MeterRegistry meterRegistry) {
        log.info("Initializing Bulkhead Registry with custom configuration");

        Map<String, BulkheadConfig> configs = new HashMap<>();

        // Default bulkhead config
        BulkheadConfig defaultConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(properties.getBulkhead().getMaxConcurrentCalls())
                .maxWaitDuration(properties.getBulkhead().getMaxWaitDuration())
                .build();

        configs.put("default", defaultConfig);

        // Service-specific configurations
        if (properties.getServices() != null) {
            properties.getServices().forEach((serviceName, serviceConfig) -> {
                if (serviceConfig.getBulkhead() != null) {
                    ResilienceProperties.Bulkhead bhConfig = serviceConfig.getBulkhead();
                    BulkheadConfig customConfig = BulkheadConfig.custom()
                            .maxConcurrentCalls(bhConfig.getMaxConcurrentCalls())
                            .maxWaitDuration(bhConfig.getMaxWaitDuration())
                            .build();

                    configs.put(serviceName, customConfig);
                    log.info("Configured bulkhead for service: {}", serviceName);
                }
            });
        }

        BulkheadRegistry registry = BulkheadRegistry.of(configs);

        // Register Micrometer metrics
        // Note: In Spring Boot 3 with resilience4j-spring-boot3, metrics are auto-registered
        // Manual metrics binding can be added later if needed

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public TimeLimiterRegistry timeLimiterRegistry(MeterRegistry meterRegistry) {
        log.info("Initializing Time Limiter Registry with custom configuration");

        Map<String, TimeLimiterConfig> configs = new HashMap<>();

        // Default time limiter config
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(properties.getTimeLimiter().getTimeoutDuration())
                .cancelRunningFuture(properties.getTimeLimiter().isCancelRunningFuture())
                .build();

        configs.put("default", defaultConfig);

        // Service-specific configurations
        if (properties.getServices() != null) {
            properties.getServices().forEach((serviceName, serviceConfig) -> {
                if (serviceConfig.getTimeLimiter() != null) {
                    ResilienceProperties.TimeLimiter tlConfig = serviceConfig.getTimeLimiter();
                    TimeLimiterConfig customConfig = TimeLimiterConfig.custom()
                            .timeoutDuration(tlConfig.getTimeoutDuration())
                            .cancelRunningFuture(tlConfig.isCancelRunningFuture())
                            .build();

                    configs.put(serviceName, customConfig);
                    log.info("Configured time limiter for service: {}", serviceName);
                }
            });
        }

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(configs);

        // Register Micrometer metrics
        // Note: In Resilience4j 2.x, TimeLimiter metrics may use different API
        // io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetricsPublisher
        //         .ofTimeLimiterRegistry(meterRegistry)
        //         .register(registry);

        return registry;
    }

    private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType toCircuitBreakerWindowType(
            ResilienceProperties.SlidingWindowType windowType) {
        // In Resilience4j 2.x, use the enum directly
        return windowType == ResilienceProperties.SlidingWindowType.COUNT_BASED
                ? io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                : io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
    }
}
