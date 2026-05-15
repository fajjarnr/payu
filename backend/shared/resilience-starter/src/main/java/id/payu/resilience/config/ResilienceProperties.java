package id.payu.resilience.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration properties for Resilience4j patterns
 */
@Data
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
     * Rate limiter configuration
     */
    private RateLimiter rateLimiter = new RateLimiter();

    /**
     * Service-specific configurations
     */
    private Map<String, ServiceConfig> services;

    @Data
    public static class CircuitBreaker {
        /**
         * Failure rate threshold in percentage
         * Default: 50% as per PayU financial services standard
         */
        private float failureRateThreshold = 50f;

        /**
         * Wait duration in open state
         * Default: 10s as per PayU financial services standard
         */
        private Duration waitDurationInOpenState = Duration.ofSeconds(10);

        /**
         * Permitted number of calls in half-open state
         */
        private int permittedNumberOfCallsInHalfOpenState = 5;

        /**
         * Sliding window size
         * Default: 100 as per PayU financial services standard
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

        /**
         * Slow call rate threshold in percentage
         */
        private float slowCallRateThreshold = 80f;

        /**
         * Slow call duration threshold
         */
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);

        /**
         * Exception class names to ignore from circuit breaker calculations
         * Business exceptions should be ignored as they represent expected failures
         * Default: BusinessException, ValidationException, ResourceNotFoundException
         */
        private String[] ignoreExceptionClassNames = new String[]{
                "id.payu.api.common.exception.BusinessException",
                "id.payu.api.common.exception.ValidationException",
                "id.payu.api.common.exception.ResourceNotFoundException"
        };
    }

    @Data
    public static class Retry {
        /**
         * Max retry attempts
         * Default: 3 as per PayU financial services standard
         */
        private int maxAttempts = 3;

        /**
         * Wait duration between retries
         * Default: 1s as per PayU financial services standard
         */
        private Duration waitDuration = Duration.ofSeconds(1);

        /**
         * Enable exponential backoff
         * Default: true as per PayU financial services standard
         */
        private boolean enableExponentialBackoff = true;

        /**
         * Exponential backoff multiplier
         * Default: 2 as per PayU financial services standard
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * Randomize wait duration
         */
        private boolean randomizeWait = false;

        /**
         * Retry exception class names - IO and network exceptions should be retried
         */
        private String[] retryExceptionClassNames = new String[]{
                "java.io.IOException",
                "java.net.SocketTimeoutException",
                "java.net.ConnectException"
        };

        /**
         * Ignore exception class names - Business exceptions should not be retried
         */
        private String[] ignoreExceptionClassNames = new String[]{
                "id.payu.api.common.exception.BusinessException",
                "id.payu.api.common.exception.ValidationException",
                "id.payu.api.common.exception.ResourceNotFoundException",
                "id.payu.api.common.exception.ConflictException"
        };
    }

    @Data
    public static class Bulkhead {
        /**
         * Max concurrent calls
         * Default: 20 as per PayU financial services standard
         */
        private int maxConcurrentCalls = 20;

        /**
         * Max wait duration for semaphore acquisition
         * Default: 500ms as per PayU financial services standard
         */
        private Duration maxWaitDuration = Duration.ofMillis(500);

        /**
         * Max thread pool size for thread pool bulkhead
         */
        private int maxThreadPoolSize = 20;

        /**
         * Core thread pool size for thread pool bulkhead
         */
        private int coreThreadPoolSize = 10;

        /**
         * Queue capacity for thread pool bulkhead
         */
        private int queueCapacity = 50;
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
    public static class RateLimiter {
        /**
         * Limit for period - number of calls allowed per refresh period
         * Default: 100 calls per second
         */
        private int limitForPeriod = 100;

        /**
         * Limit refresh period
         * Default: 1 second
         */
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);

        /**
         * Timeout duration for acquiring permission
         * Default: 500ms
         */
        private Duration timeoutDuration = Duration.ofMillis(500);
    }

    @Data
    public static class ServiceConfig {
        private CircuitBreaker circuitBreaker;
        private Retry retry;
        private Bulkhead bulkhead;
        private TimeLimiter timeLimiter;
        private RateLimiter rateLimiter;
    }
}
