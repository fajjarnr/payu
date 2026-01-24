package id.payu.resilience.aspect;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring and logging resilience events
 */
@Slf4j
@Aspect
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
            EventPublisher<CircuitBreakerEvent> eventPublisher = circuitBreaker.getEventPublisher();

            // In Resilience4j 2.x, use onEvent() to handle all event types
            eventPublisher.onEvent(event -> {
                if (event.getEventType() == CircuitBreakerEvent.Type.CALL_NOT_PERMITTED) {
                    log.warn("Circuit breaker {} is OPEN - call not permitted", event.getCircuitBreakerName());
                    publishAlert("CIRCUIT_BREAKER_OPEN", event.getCircuitBreakerName());
                } else if (event.getEventType() == CircuitBreakerEvent.Type.ERROR) {
                    log.error("Circuit breaker {} recorded error: {}", event.getCircuitBreakerName(),
                            event.getThrowable() != null ? event.getThrowable().getMessage() : "Unknown error");
                } else if (event.getEventType() == CircuitBreakerEvent.Type.STATE_TRANSITION) {
                    log.info("Circuit breaker {} state transition", event.getCircuitBreakerName());
                }
            });

            eventPublisher.onSuccessRateExceeded(event -> {
                log.info("Circuit breaker {} success rate exceeded: {}%",
                        event.getCircuitBreakerName(), event.getSuccessRate());
            });

            eventPublisher.onFailureRateExceeded(event -> {
                log.warn("Circuit breaker {} failure rate exceeded: {}%",
                        event.getCircuitBreakerName(), event.getFailureRate());
            });
        });
    }

    /**
     * Monitor all retry events
     */
    public void registerRetryEventPublisher() {
        retryRegistry.getAllRetries().forEach(retry -> {
            EventPublisher<io.github.resilience4j.retry.event.RetryEvent> eventPublisher = retry.getEventPublisher();

            eventPublisher.onRetry(event -> {
                log.warn("Retry {} attempt {} for exception: {}", event.getName(),
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage());
            });

            eventPublisher.onError(event -> {
                log.error("Retry {} failed after {} attempts", event.getName(),
                        event.getNumberOfRetryAttempts());
            });

            eventPublisher.onSuccess(event -> {
                log.info("Retry {} succeeded after {} attempts", event.getName(),
                        event.getNumberOfRetryAttempts());
            });
        });
    }

    /**
     * Monitor all bulkhead events
     */
    public void registerBulkheadEventPublisher() {
        bulkheadRegistry.getAllBulkheads().forEach(bulkhead -> {
            EventPublisher<io.github.resilience4j.bulkhead.event.BulkheadEvent> eventPublisher = bulkhead.getEventPublisher();

            eventPublisher.onCallPermitted(event -> {
                log.debug("Bulkhead {} permitted call", event.getBulkheadName());
            });

            eventPublisher.onCallRejected(event -> {
                log.warn("Bulkhead {} rejected call - bulkhead full", event.getBulkheadName());
                publishAlert("BULKHEAD_FULL", event.getBulkheadName());
            });
        });
    }

    /**
     * Monitor all time limiter events
     */
    public void registerTimeLimiterEventPublisher() {
        timeLimiterRegistry.getAllTimeLimiters().forEach(timeLimiter -> {
            EventPublisher<io.github.resilience4j.timelimiter.event.TimeLimiterEvent> eventPublisher = timeLimiter.getEventPublisher();

            eventPublisher.onTimeout(event -> {
                log.warn("Time limiter {} timed out after {}", event.getTimeLimiterName(),
                        event.getTimeoutDuration());
            });

            eventPublisher.onError(event -> {
                log.error("Time limiter {} error: {}", event.getTimeLimiterName(),
                        event.getThrowable().getMessage());
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
