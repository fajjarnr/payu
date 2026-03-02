package id.payu.shared.restclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for PayU REST Client.
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * payu:
 *   rest-client:
 *     connect-timeout: 5000
 *     read-timeout: 30000
 *     max-retries: 3
 *     retry-backoff: 1000
 *     services:
 *       bi-fast:
 *         base-url: https://api.bi-fast.id
 *         connect-timeout: 3000
 *         read-timeout: 10000
 * </pre>
 */
@ConfigurationProperties(prefix = "payu.rest-client")
public class RestClientProperties {

    /**
     * Default connection timeout in milliseconds.
     */
    private int connectTimeout = 5000;

    /**
     * Default read timeout in milliseconds.
     */
    private int readTimeout = 30000;

    /**
     * Maximum number of retry attempts for retryable failures.
     */
    private int maxRetries = 3;

    /**
     * Initial backoff duration in milliseconds between retries.
     * Exponential backoff multiplier of 2.0 is applied.
     */
    private long retryBackoff = 1000;

    /**
     * Circuit breaker failure rate threshold percentage.
     * When failure rate exceeds this, the circuit opens.
     */
    private float circuitBreakerFailureRateThreshold = 50f;

    /**
     * Duration the circuit breaker stays open before transitioning to half-open.
     */
    private Duration circuitBreakerWaitDuration = Duration.ofSeconds(10);

    /**
     * Sliding window size for the circuit breaker.
     */
    private int circuitBreakerSlidingWindowSize = 20;

    /**
     * Minimum number of calls before circuit breaker calculates failure rate.
     */
    private int circuitBreakerMinimumCalls = 5;

    /**
     * Per-service configuration overrides.
     */
    private Map<String, ServiceClientProperties> services = new HashMap<>();

    // --- Getters and Setters ---

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(long retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public float getCircuitBreakerFailureRateThreshold() {
        return circuitBreakerFailureRateThreshold;
    }

    public void setCircuitBreakerFailureRateThreshold(float circuitBreakerFailureRateThreshold) {
        this.circuitBreakerFailureRateThreshold = circuitBreakerFailureRateThreshold;
    }

    public Duration getCircuitBreakerWaitDuration() {
        return circuitBreakerWaitDuration;
    }

    public void setCircuitBreakerWaitDuration(Duration circuitBreakerWaitDuration) {
        this.circuitBreakerWaitDuration = circuitBreakerWaitDuration;
    }

    public int getCircuitBreakerSlidingWindowSize() {
        return circuitBreakerSlidingWindowSize;
    }

    public void setCircuitBreakerSlidingWindowSize(int circuitBreakerSlidingWindowSize) {
        this.circuitBreakerSlidingWindowSize = circuitBreakerSlidingWindowSize;
    }

    public int getCircuitBreakerMinimumCalls() {
        return circuitBreakerMinimumCalls;
    }

    public void setCircuitBreakerMinimumCalls(int circuitBreakerMinimumCalls) {
        this.circuitBreakerMinimumCalls = circuitBreakerMinimumCalls;
    }

    public Map<String, ServiceClientProperties> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceClientProperties> services) {
        this.services = services;
    }

    /**
     * Per-service REST client configuration.
     */
    public static class ServiceClientProperties {
        private String baseUrl;
        private int connectTimeout = -1; // -1 means use global default
        private int readTimeout = -1;
        private int maxRetries = -1;
        private long retryBackoff = -1;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(long retryBackoff) {
            this.retryBackoff = retryBackoff;
        }
    }
}
