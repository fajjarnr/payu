package id.payu.shared.restclient;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Auto-configuration for PayU standardized REST client.
 *
 * <p>Provides:
 * <ul>
 *   <li>Pre-configured {@link RestClient.Builder} with timeouts and error handling</li>
 *   <li>A ready-to-use {@link PayuRestClient} with circuit breaker and retry</li>
 *   <li>Dedicated {@link CircuitBreakerRegistry} and {@link RetryRegistry} for REST calls</li>
 * </ul>
 *
 * <p>Enabled by default. Disable with {@code payu.rest-client.enabled=false}.
 */
@AutoConfiguration
@EnableConfigurationProperties(RestClientProperties.class)
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "payu.rest-client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RestClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RestClientAutoConfiguration.class);

    private final RestClientProperties properties;

    public RestClientAutoConfiguration(RestClientProperties properties) {
        this.properties = properties;
    }

    /**
     * Provides a pre-configured {@link RestClient.Builder} with PayU defaults.
     * Services can inject this builder to create customized RestClient instances.
     */
    @Bean
    @ConditionalOnMissingBean(name = "payuRestClientBuilder")
    public RestClient.Builder payuRestClientBuilder() {
        log.info("Initializing PayU REST Client Builder [connectTimeout={}ms, readTimeout={}ms]",
                properties.getConnectTimeout(), properties.getReadTimeout());

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "PayU-RestClient/1.0")
                .defaultStatusHandler(new RestClientErrorHandler());
    }

    /**
     * Creates the default {@link RestClient} instance from the builder.
     */
    @Bean
    @ConditionalOnMissingBean(name = "payuRestClient")
    public RestClient payuDefaultRestClient(RestClient.Builder payuRestClientBuilder) {
        return payuRestClientBuilder.build();
    }

    /**
     * Creates a dedicated {@link CircuitBreakerRegistry} for REST client calls.
     * This is separate from the general resilience-starter registry to allow
     * independent tuning for external HTTP calls.
     */
    @Bean
    @ConditionalOnMissingBean(name = "restClientCircuitBreakerRegistry")
    public CircuitBreakerRegistry restClientCircuitBreakerRegistry() {
        log.info("Initializing REST Client Circuit Breaker Registry " +
                        "[failureRate={}%, waitDuration={}s, windowSize={}]",
                properties.getCircuitBreakerFailureRateThreshold(),
                properties.getCircuitBreakerWaitDuration().getSeconds(),
                properties.getCircuitBreakerSlidingWindowSize());

        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.getCircuitBreakerFailureRateThreshold())
                .waitDurationInOpenState(properties.getCircuitBreakerWaitDuration())
                .slidingWindowSize(properties.getCircuitBreakerSlidingWindowSize())
                .minimumNumberOfCalls(properties.getCircuitBreakerMinimumCalls())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .permittedNumberOfCallsInHalfOpenState(3)
                // Client errors (4xx except 429) should not trip the circuit
                .ignoreException(e -> e instanceof RestClientErrorHandler.ExternalServiceClientException)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    /**
     * Creates a dedicated {@link RetryRegistry} for REST client calls.
     * Only retries on server errors and network failures, not on client errors.
     */
    @Bean
    @ConditionalOnMissingBean(name = "restClientRetryRegistry")
    public RetryRegistry restClientRetryRegistry() {
        log.info("Initializing REST Client Retry Registry [maxRetries={}, backoff={}ms]",
                properties.getMaxRetries(), properties.getRetryBackoff());

        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(properties.getMaxRetries())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        properties.getRetryBackoff(), 2.0))
                .retryExceptions(
                        IOException.class,
                        SocketTimeoutException.class,
                        RestClientErrorHandler.ExternalServiceUnavailableException.class)
                .ignoreExceptions(
                        RestClientErrorHandler.ExternalServiceClientException.class)
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    /**
     * Creates the main {@link PayuRestClient} bean with resilience wiring.
     */
    @Bean
    @ConditionalOnMissingBean(PayuRestClient.class)
    public PayuRestClient payuRestClientWrapper(
            RestClient payuDefaultRestClient,
            @Autowired CircuitBreakerRegistry restClientCircuitBreakerRegistry,
            @Autowired RetryRegistry restClientRetryRegistry) {
        log.info("PayU REST Client initialized with circuit breaker and retry support");
        return new PayuRestClient(payuDefaultRestClient,
                restClientCircuitBreakerRegistry, restClientRetryRegistry);
    }
}
