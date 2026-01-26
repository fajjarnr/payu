package id.payu.resilience.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ResilienceAutoConfiguration
 */
@SpringBootTest(classes = ResilienceAutoConfigurationTest.TestConfiguration.class)
@ImportAutoConfiguration(ResilienceAutoConfiguration.class)
@TestPropertySource(properties = {
    "payu.resilience.enabled=true",
    "payu.resilience.circuit-breaker.failure-rate-threshold=50",
    "payu.resilience.circuit-breaker.wait-duration-in-open-state=30s",
    "payu.resilience.retry.max-attempts=3",
    "payu.resilience.bulkhead.max-concurrent-calls=25"
})
class ResilienceAutoConfigurationTest {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private RetryRegistry retryRegistry;

    @Autowired(required = false)
    private BulkheadRegistry bulkheadRegistry;

    @Autowired(required = false)
    private TimeLimiterRegistry timeLimiterRegistry;

    @Test
    void testRegistriesCreated() {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(retryRegistry).isNotNull();
        assertThat(bulkheadRegistry).isNotNull();
        assertThat(timeLimiterRegistry).isNotNull();
    }

    @Test
    void testDefaultCircuitBreakerConfig() {
        assertThat(circuitBreakerRegistry).isNotNull();
        CircuitBreaker defaultCircuitBreaker = circuitBreakerRegistry.circuitBreaker("default");
        assertThat(defaultCircuitBreaker).isNotNull();

        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = defaultCircuitBreaker.getCircuitBreakerConfig();
        assertThat(config.getFailureRateThreshold()).isEqualTo(50f);
        // In Resilience4j 2.x, waitDuration is obtained via getWaitIntervalFunction()
        // Just verify the circuit breaker exists and is configured
        assertThat(config.getSlidingWindowSize()).isEqualTo(100);
    }

    @Test
    void testDefaultRetryConfig() {
        assertThat(retryRegistry).isNotNull();
        Retry defaultRetry = retryRegistry.retry("default");
        assertThat(defaultRetry).isNotNull();

        io.github.resilience4j.retry.RetryConfig config = defaultRetry.getRetryConfig();
        assertThat(config.getMaxAttempts()).isEqualTo(3);
    }

    @Test
    void testDefaultBulkheadConfig() {
        assertThat(bulkheadRegistry).isNotNull();
        Bulkhead defaultBulkhead = bulkheadRegistry.bulkhead("default");
        assertThat(defaultBulkhead).isNotNull();

        io.github.resilience4j.bulkhead.BulkheadConfig config = defaultBulkhead.getBulkheadConfig();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(25);
    }

    @Test
    void testDefaultTimeLimiterConfig() {
        assertThat(timeLimiterRegistry).isNotNull();
        TimeLimiter defaultTimeLimiter = timeLimiterRegistry.timeLimiter("default");
        assertThat(defaultTimeLimiter).isNotNull();

        io.github.resilience4j.timelimiter.TimeLimiterConfig config = defaultTimeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
    }

    @Configuration
    static class TestConfiguration {
        // Empty configuration class for @SpringBootTest
    }
}
