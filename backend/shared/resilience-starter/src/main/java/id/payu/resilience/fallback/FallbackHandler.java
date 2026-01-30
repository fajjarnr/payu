package id.payu.resilience.fallback;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Resilience4j patterns.
 * Provides standardized fallback responses for resilience-related exceptions.
 *
 * <p>This handler converts Resilience4j exceptions into user-friendly HTTP responses
 * with appropriate status codes and retry guidance.
 *
 * @see io.github.resilience4j.circuitbreaker.CallNotPermittedException
 * @see io.github.resilience4j.ratelimiter.RequestNotPermitted
 * @see io.github.resilience4j.bulkhead.BulkheadFullException
 */
@Slf4j
@RestControllerAdvice
public class FallbackHandler {

    /**
     * Handle Circuit Breaker Open Exception.
     * Returns HTTP 503 Service Unavailable with retry-after guidance.
     *
     * @param ex the CallNotPermittedException
     * @return error response map with retry information
     */
    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker is open: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_UNAVAILABLE");
        response.put("errorCode", "RES_001");
        response.put("message", "Service is temporarily unavailable. Please try again later.");
        response.put("retryAfter", "30s");
        response.put("circuitBreakerName", ex.getCausingCircuitBreakerName());

        return response;
    }

    /**
     * Handle Rate Limit Exceeded Exception.
     * Returns HTTP 429 Too Many Requests.
     *
     * @param ex the RequestNotPermitted
     * @return error response map
     */
    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleRateLimitExceeded(RequestNotPermitted ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "TOO_MANY_REQUESTS");
        response.put("errorCode", "RES_002");
        response.put("message", "Too many requests. Please slow down and try again.");

        return response;
    }

    /**
     * Handle Bulkhead Full Exception.
     * Returns HTTP 503 Service Unavailable when concurrent call limit is reached.
     *
     * @param ex the BulkheadFullException
     * @return error response map
     */
    @ExceptionHandler(io.github.resilience4j.bulkhead.BulkheadFullException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleBulkheadFull(io.github.resilience4j.bulkhead.BulkheadFullException ex) {
        log.warn("Bulkhead is full: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_BUSY");
        response.put("errorCode", "RES_003");
        response.put("message", "Service is busy. Please try again later.");

        return response;
    }

    /**
     * Handle Timeout Exception.
     * Returns HTTP 408 Request Timeout.
     *
     * @param ex the TimeoutException
     * @return error response map
     */
    @ExceptionHandler({java.util.concurrent.TimeoutException.class, java.util.concurrent.TimeoutException.class})
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public Map<String, Object> handleTimeout(Exception ex) {
        log.warn("Request timeout: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "REQUEST_TIMEOUT");
        response.put("errorCode", "RES_004");
        response.put("message", "Request processing timed out. Please try again.");

        return response;
    }

    /**
     * Handle generic resilience exceptions.
     * Returns HTTP 503 for resilience-related failures.
     *
     * @param ex the exception
     * @return error response map
     */
    @ExceptionHandler({io.github.resilience4j.retry.MaxRetriesExceededException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleResilienceException(Exception ex) {
        log.error("Resilience pattern triggered: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "RETRY_EXHAUSTED");
        response.put("errorCode", "RES_005");
        response.put("message", "Service is temporarily unavailable after multiple retry attempts.");
        response.put("retryAfter", "60s");

        return response;
    }

    /**
     * Interface for implementing custom fallback strategies.
     * Implementations should provide alternative responses when the primary
     * service call fails due to resilience patterns.
     *
     * @param <T> the return type of the fallback
     */
    @FunctionalInterface
    public interface FallbackStrategy<T> {
        /**
         * Execute the fallback logic.
         *
         * @param exception the exception that caused the fallback
         * @return the fallback response
         */
        T execute(Exception exception);
    }

    /**
     * Get a standardized error response for circuit breaker open state.
     *
     * @param serviceName the name of the service
     * @return error response map
     */
    public static Map<String, Object> getCircuitBreakerOpenResponse(String serviceName) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_UNAVAILABLE");
        response.put("errorCode", "RES_001");
        response.put("message", "Service '" + serviceName + "' is temporarily unavailable.");
        response.put("retryAfter", "30s");
        return response;
    }

    /**
     * Get a standardized error response for bulkhead full.
     *
     * @param serviceName the name of the service
     * @return error response map
     */
    public static Map<String, Object> getBulkheadFullResponse(String serviceName) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_BUSY");
        response.put("errorCode", "RES_003");
        response.put("message", "Service '" + serviceName + "' is currently at capacity.");
        response.put("retryAfter", "5s");
        return response;
    }
}
