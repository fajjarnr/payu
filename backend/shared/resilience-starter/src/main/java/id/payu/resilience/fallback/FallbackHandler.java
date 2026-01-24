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
 * Global exception handler for Resilience4j patterns
 */
@Slf4j
@RestControllerAdvice
public class FallbackHandler {

    /**
     * Handle Circuit Breaker Open Exception
     */
    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker is open: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_UNAVAILABLE");
        response.put("message", "Service is temporarily unavailable. Please try again later.");
        response.put("retryAfter", "30s");

        return response;
    }

    /**
     * Handle Rate Limit Exceeded Exception
     */
    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleRateLimitExceeded(RequestNotPermitted ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "TOO_MANY_REQUESTS");
        response.put("message", "Too many requests. Please slow down and try again.");

        return response;
    }

    /**
     * Handle Bulkhead Full Exception
     */
    @ExceptionHandler(io.github.resilience4j.bulkhead.BulkheadFullException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> handleBulkheadFull(io.github.resilience4j.bulkhead.BulkheadFullException ex) {
        log.warn("Bulkhead is full: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_BUSY");
        response.put("message", "Service is busy. Please try again later.");

        return response;
    }

    /**
     * Handle Timeout Exception
     */
    @ExceptionHandler({java.util.concurrent.TimeoutException.class, java.util.concurrent.TimeoutException.class})
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public Map<String, Object> handleTimeout(Exception ex) {
        log.warn("Request timeout: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("error", "REQUEST_TIMEOUT");
        response.put("message", "Request processing timed out. Please try again.");

        return response;
    }
}
