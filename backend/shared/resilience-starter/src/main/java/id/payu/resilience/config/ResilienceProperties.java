package id.payu.resilience.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration properties for Resilience4j patterns
 */
@Data
@Component
@ConfigurationProperties(prefix = "payu.resilience")
public class ResilienceProperties {

    /**
     * Circuit breaker configuration
     */
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    /**
     * Retry configuration
     */
    private Retry retry = new Retry();

    /**
     * Bulkhead configuration
     */
    private Bulkhead bulkhead = new Bulkhead();

    /**
     * Time limiter configuration
     */
    private TimeLimiter timeLimiter = new TimeLimiter();

    /**
     * Service-specific configurations
     */
    private Map<String, ServiceConfig> services;

    @Data
    public static class CircuitBreaker {
        /**
         * Failure rate threshold in percentage
         */
        private float failureRateThreshold = 50f;

        /**
         * Wait duration in open state
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);

        /**
         * Permitted number of calls in half-open state
         */
        private int permittedNumberOfCallsInHalfOpenState = 5;

        /**
         * Sliding window size
         */
        private int slidingWindowSize = 100;

        /**
         * Minimum number of calls before calculating error rate
         */
        private int minimumNumberOfCalls = 10;

        /**
         * Sliding window type (COUNT_BASED or TIME_BASED)
         */
        private SlidingWindowType slidingWindowType = SlidingWindowType.COUNT_BASED;

        /**
         * Automatic transition from open to half-open
         */
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;
    }

    @Data
    public static class Retry {
        /**
         * Max retry attempts
         */
        private int maxAttempts = 3;

        /**
         * Wait duration between retries
         */
        private Duration waitDuration = Duration.ofMillis(500);

        /**
         * Enable exponential backoff
         */
        private boolean enableExponentialBackoff = false;

        /**
         * Exponential backoff multiplier
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * Randomize wait duration
         */
        private boolean randomizeWait = false;

        /**
         * Retry exceptions
         */
        private Class<?>[] retryExceptions;

        /**
         * Ignore exceptions
         */
        private Class<?>[] ignoreExceptions;
    }

    @Data
    public static class Bulkhead {
        /**
         * Max concurrent calls
         */
        private int maxConcurrentCalls = 25;

        /**
         * Max wait duration
         */
        private Duration maxWaitDuration = Duration.ofSeconds(1);
    }

    @Data
    public static class TimeLimiter {
        /**
         * Timeout duration
         */
        private Duration timeoutDuration = Duration.ofSeconds(5);

        /**
         * Cancel running future on timeout
         */
        private boolean cancelRunningFuture = true;
    }

    @Data
    public static class ServiceConfig {
        private CircuitBreaker circuitBreaker;
        private Retry retry;
        private Bulkhead bulkhead;
        private TimeLimiter timeLimiter;
    }

    public enum SlidingWindowType {
        COUNT_BASED, TIME_BASED
    }
}
