package id.payu.resilience.aspect;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring and logging resilience events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilienceAspect {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    /**
     * Monitor all circuit breaker events
     */
    public void registerCircuitBreakerEventPublisher() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            var eventPublisher = circuitBreaker.getEventPublisher();

            eventPublisher.onEvent(event -> {
                log.debug("Circuit breaker {} event: {}", circuitBreaker.getName(), event.getEventType());
            });

            eventPublisher.onSuccess(event -> {
                log.debug("Circuit breaker {} call succeeded", circuitBreaker.getName());
            });

            eventPublisher.onError(event -> {
                log.error("Circuit breaker {} recorded error: {}", circuitBreaker.getName(),
                        event.getThrowable() != null ? event.getThrowable().getMessage() : "Unknown error");
            });

            eventPublisher.onStateTransition(event -> {
                log.info("Circuit breaker {} state transition: {} -> {}",
                        circuitBreaker.getName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState());
            });

            eventPublisher.onCallNotPermitted(event -> {
                log.warn("Circuit breaker {} is OPEN - call not permitted", circuitBreaker.getName());
                publishAlert("CIRCUIT_BREAKER_OPEN", circuitBreaker.getName());
            });

            eventPublisher.onFailureRateExceeded(event -> {
                log.warn("Circuit breaker {} failure rate exceeded: {}%",
                        circuitBreaker.getName(), event.getFailureRate());
            });

            eventPublisher.onSlowCallRateExceeded(event -> {
                log.warn("Circuit breaker {} slow call rate exceeded: {}%",
                        circuitBreaker.getName(), event.getSlowCallRate());
            });
        });
    }

    /**
     * Monitor all retry events
     */
    public void registerRetryEventPublisher() {
        retryRegistry.getAllRetries().forEach(retry -> {
            var eventPublisher = retry.getEventPublisher();

            eventPublisher.onEvent(event -> {
                log.debug("Retry {} event: {}", retry.getName(), event.getEventType());
            });

            eventPublisher.onRetry(event -> {
                log.warn("Retry {} attempt {} for exception: {}", retry.getName(),
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "Unknown");
            });

            eventPublisher.onError(event -> {
                log.error("Retry {} failed after {} attempts", retry.getName(),
                        event.getNumberOfRetryAttempts());
            });

            eventPublisher.onSuccess(event -> {
                log.info("Retry {} succeeded after {} attempts", retry.getName(),
                        event.getNumberOfRetryAttempts());
            });
        });
    }

    /**
     * Monitor all bulkhead events
     */
    public void registerBulkheadEventPublisher() {
        bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
            var eventPublisher = bulkhead.getEventPublisher();

            eventPublisher.onEvent(event -> {
                log.debug("Bulkhead {} event: {}", bulkhead.getName(), event.getEventType());
            });

            eventPublisher.onCallPermitted(event -> {
                log.debug("Bulkhead {} permitted call", bulkhead.getName());
            });

            eventPublisher.onCallRejected(event -> {
                log.warn("Bulkhead {} rejected call - bulkhead full", bulkhead.getName());
                publishAlert("BULKHEAD_FULL", bulkhead.getName());
            });

            eventPublisher.onCallFinished(event -> {
                log.debug("Bulkhead {} call finished", bulkhead.getName());
            });
        });
    }

    /**
     * Monitor all time limiter events
     */
    public void registerTimeLimiterEventPublisher() {
        timeLimiterRegistry.getAllTimeLimiters().forEach(timeLimiter -> {
            var eventPublisher = timeLimiter.getEventPublisher();

            eventPublisher.onEvent(event -> {
                log.debug("Time limiter {} event: {}", timeLimiter.getName(), event.getEventType());
            });

            eventPublisher.onTimeout(event -> {
                log.warn("Time limiter {} timed out", timeLimiter.getName());
                publishAlert("TIMEOUT", timeLimiter.getName());
            });

            eventPublisher.onError(event -> {
                log.error("Time limiter {} error: {}", timeLimiter.getName(),
                        event.getThrowable() != null ? event.getThrowable().getMessage() : "Unknown error");
            });

            eventPublisher.onSuccess(event -> {
                log.debug("Time limiter {} succeeded", timeLimiter.getName());
            });
        });
    }

    /**
     * Publish alert for monitoring system
     * In production, this would integrate with your alerting system (Prometheus, Grafana, PagerDuty, etc.)
     */
    private void publishAlert(String alertType, String componentName) {
        // TODO: Integrate with alerting system
        log.warn("ALERT [{}]: Component {} requires attention", alertType, componentName);

        // Example: Send to Prometheus metrics, or call alerting API
        // meterRegistry.counter("resilience.alerts", "type", alertType, "component", componentName).increment();
    }

    /**
     * Initialize event publishers after construction
     */
    @PostConstruct
    public void init() {
        registerCircuitBreakerEventPublisher();
        registerRetryEventPublisher();
        registerBulkheadEventPublisher();
        registerTimeLimiterEventPublisher();
    }
}
