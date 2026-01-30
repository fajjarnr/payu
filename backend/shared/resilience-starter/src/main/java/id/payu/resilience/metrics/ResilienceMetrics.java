package id.payu.resilience.metrics;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Counter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micrometer metrics collector for Resilience4j patterns.
 * Provides circuit breaker state metrics, transition alerts, and health indicators
 * optimized for financial services monitoring.
 *
 * <p>Metrics exposed:
 * <ul>
 *   <li>Circuit breaker state (CLOSED, OPEN, HALF_OPEN)</li>
 *   <li>Failure rate percentage</li>
 *   <li>Call counts (successful, failed, not permitted)</li>
 *   <li>State transition events</li>
 *   <li>Retry metrics (successful, failed, attempts)</li>
 * </ul>
 *
 * <p>Alert thresholds can be configured via properties:
 * <pre>
 * payu.resilience.metrics:
 *   failure-rate-threshold: 50.0
 *   slow-call-rate-threshold: 80.0
 *   state-change-alert-enabled: true
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilienceMetrics {

    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    // Alert state tracking to prevent duplicate alerts
    private final Map<String, CircuitBreaker.State> lastKnownStates = new ConcurrentHashMap<>();

    // Metric names
    public static final String METRIC_PREFIX = "payu.resilience";
    public static final String CIRCUIT_BREAKER_STATE = METRIC_PREFIX + ".circuitbreaker.state";
    public static final String CIRCUIT_BREAKER_FAILURE_RATE = METRIC_PREFIX + ".circuitbreaker.failure.rate";
    public static final String CIRCUIT_BREAKER_CALLS = METRIC_PREFIX + ".circuitbreaker.calls";
    public static final String CIRCUIT_BREAKER_TRANSITIONS = METRIC_PREFIX + ".circuitbreaker.transitions";
    public static final String RETRY_ATTEMPTS = METRIC_PREFIX + ".retry.attempts";
    public static final String RETRY_SUCCESS = METRIC_PREFIX + ".retry.success";

    // Alert thresholds
    private static final double DEFAULT_FAILURE_RATE_ALERT_THRESHOLD = 50.0;
    private static final double DEFAULT_SLOW_CALL_RATE_ALERT_THRESHOLD = 80.0;

    /**
     * Initialize metrics collection on startup.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing Resilience4j metrics collection");

        registerCircuitBreakerMetrics();
        registerRetryMetrics();
        registerEventListeners();

        log.info("Resilience4j metrics collection initialized successfully");
    }

    /**
     * Register circuit breaker state and metrics gauges.
     */
    private void registerCircuitBreakerMetrics() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String name = circuitBreaker.getName();
            Tags tags = Tags.of("circuit_breaker", name);

            // Circuit breaker state gauge (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
            Gauge.builder(CIRCUIT_BREAKER_STATE, circuitBreaker, cb -> {
                        switch (cb.getState()) {
                            case OPEN:
                                return 1.0;
                            case HALF_OPEN:
                                return 2.0;
                            case CLOSED:
                            default:
                                return 0.0;
                        }
                    })
                    .description("Circuit breaker state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)")
                    .tags(tags)
                    .register(meterRegistry);

            // Failure rate gauge
            Gauge.builder(CIRCUIT_BREAKER_FAILURE_RATE, circuitBreaker,
                        cb -> cb.getMetrics().getFailureRate())
                    .description("Circuit breaker failure rate percentage")
                    .tags(tags)
                    .register(meterRegistry);

            // Buffered calls count
            Gauge.builder(CIRCUIT_BREAKER_CALLS, circuitBreaker,
                        cb -> cb.getMetrics().getNumberOfBufferedCalls())
                    .description("Total number of buffered calls")
                    .tags(tags.and("type", "buffered"))
                    .register(meterRegistry);

            // Failed calls count
            Gauge.builder(CIRCUIT_BREAKER_CALLS, circuitBreaker,
                        cb -> cb.getMetrics().getNumberOfFailedCalls())
                    .description("Number of failed calls")
                    .tags(tags.and("type", "failed"))
                    .register(meterRegistry);

            // Successful calls count
            Gauge.builder(CIRCUIT_BREAKER_CALLS, circuitBreaker,
                        cb -> cb.getMetrics().getNumberOfSuccessfulCalls())
                    .description("Number of successful calls")
                    .tags(tags.and("type", "successful"))
                    .register(meterRegistry);

            // Not permitted calls count (when circuit is open)
            Gauge.builder(CIRCUIT_BREAKER_CALLS, circuitBreaker,
                        cb -> cb.getMetrics().getNumberOfNotPermittedCalls())
                    .description("Number of calls not permitted due to open circuit")
                    .tags(tags.and("type", "not_permitted"))
                    .register(meterRegistry);

            // Slow call rate
            Gauge.builder(CIRCUIT_BREAKER_FAILURE_RATE, circuitBreaker,
                        cb -> cb.getMetrics().getSlowCallRate())
                    .description("Slow call rate percentage")
                    .tags(tags.and("type", "slow"))
                    .register(meterRegistry);

            // Initialize last known state
            lastKnownStates.put(name, circuitBreaker.getState());

            log.debug("Registered metrics for circuit breaker: {}", name);
        });
    }

    /**
     * Register retry metrics.
     */
    private void registerRetryMetrics() {
        retryRegistry.getAllRetries().forEach(retry -> {
            String name = retry.getName();
            Tags tags = Tags.of("retry", name);

            // Successful retry count (without retry)
            Gauge.builder(RETRY_SUCCESS, retry,
                        r -> r.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
                    .description("Number of successful calls without retry")
                    .tags(tags.and("type", "without_retry"))
                    .register(meterRegistry);

            // Successful retry count (with retry)
            Gauge.builder(RETRY_SUCCESS, retry,
                        r -> r.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
                    .description("Number of successful calls with retry")
                    .tags(tags.and("type", "with_retry"))
                    .register(meterRegistry);

            // Failed retry count
            Gauge.builder(RETRY_ATTEMPTS, retry,
                        r -> r.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                    .description("Number of failed calls after retry attempts")
                    .tags(tags.and("type", "failed"))
                    .register(meterRegistry);

            log.debug("Registered metrics for retry: {}", name);
        });
    }

    /**
     * Register event listeners for state transitions and alerts.
     */
    private void registerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            circuitBreaker.getEventPublisher()
                    .onStateTransition(this::handleStateTransition)
                    .onFailureRateExceeded(this::handleFailureRateExceeded)
                    .onSlowCallRateExceeded(this::handleSlowCallRateExceeded)
                    .onCallNotPermitted(this::handleCallNotPermitted);
        });

        retryRegistry.getAllRetries().forEach(retry -> {
            retry.getEventPublisher()
                    .onRetry(event -> log.warn(
                            "Retry {} - Attempt {} for exception: {}",
                            event.getName(),
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable() != null ?
                                    event.getLastThrowable().getMessage() : "Unknown"))
                    .onError(event -> log.error(
                            "Retry {} exhausted after {} attempts",
                            event.getName(),
                            event.getNumberOfRetryAttempts()));
        });
    }

    /**
     * Handle circuit breaker state transition events.
     */
    private void handleStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        String name = event.getCircuitBreakerName();
        CircuitBreaker.State fromState = event.getStateTransition().getFromState();
        CircuitBreaker.State toState = event.getStateTransition().getToState();

        log.warn("Circuit breaker '{}' state transition: {} -> {}", name, fromState, toState);

        // Record transition counter
        Counter.builder(CIRCUIT_BREAKER_TRANSITIONS)
                .description("Circuit breaker state transition count")
                .tags(Tags.of(
                        "circuit_breaker", name,
                        "from_state", fromState.name(),
                        "to_state", toState.name()
                ))
                .register(meterRegistry)
                .increment();

        // Update last known state
        lastKnownStates.put(name, toState);

        // Alert on critical transitions
        if (toState == CircuitBreaker.State.OPEN) {
            publishAlert("CIRCUIT_BREAKER_OPEN",
                    String.format("Circuit breaker '%s' is now OPEN", name));
        } else if (fromState == CircuitBreaker.State.OPEN && toState == CircuitBreaker.State.HALF_OPEN) {
            publishAlert("CIRCUIT_BREAKER_HALF_OPEN",
                    String.format("Circuit breaker '%s' transitioning to HALF_OPEN", name));
        }
    }

    /**
     * Handle failure rate exceeded events.
     */
    private void handleFailureRateExceeded(io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnFailureRateExceededEvent event) {
        String name = event.getCircuitBreakerName();
        float failureRate = event.getFailureRate();

        log.warn("Circuit breaker '{}' failure rate exceeded: {}%", name, failureRate);

        if (failureRate >= DEFAULT_FAILURE_RATE_ALERT_THRESHOLD) {
            publishAlert("HIGH_FAILURE_RATE",
                    String.format("Circuit breaker '%s' has high failure rate: %.1f%%",
                            name, failureRate));
        }
    }

    /**
     * Handle slow call rate exceeded events.
     */
    private void handleSlowCallRateExceeded(io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSlowCallRateExceededEvent event) {
        String name = event.getCircuitBreakerName();
        float slowCallRate = event.getSlowCallRate();

        log.warn("Circuit breaker '{}' slow call rate exceeded: {}%", name, slowCallRate);

        if (slowCallRate >= DEFAULT_SLOW_CALL_RATE_ALERT_THRESHOLD) {
            publishAlert("HIGH_SLOW_CALL_RATE",
                    String.format("Circuit breaker '%s' has high slow call rate: %.1f%%",
                            name, slowCallRate));
        }
    }

    /**
     * Handle call not permitted events (circuit open).
     */
    private void handleCallNotPermitted(io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnCallNotPermittedEvent event) {
        String name = event.getCircuitBreakerName();
        log.warn("Call not permitted for circuit breaker '{}' - circuit is OPEN", name);
    }

    /**
     * Publish an alert to the monitoring system.
     * In production, this would integrate with Prometheus Alertmanager,
     * PagerDuty, or other alerting systems.
     *
     * @param alertType the type of alert
     * @param message   the alert message
     */
    private void publishAlert(String alertType, String message) {
        // Increment alert counter
        Counter.builder(METRIC_PREFIX + ".alerts")
                .description("Resilience alert counter")
                .tags(Tags.of("type", alertType))
                .register(meterRegistry)
                .increment();

        // Log the alert
        log.error("RESILIENCE ALERT [{}]: {}", alertType, message);

        // TODO: Integrate with external alerting systems
        // Example integrations:
        // - Prometheus Alertmanager
        // - PagerDuty
        // - Slack notifications
        // - Email alerts
    }

    /**
     * Get the current state of all circuit breakers.
     *
     * @return map of circuit breaker names to their states
     */
    public Map<String, String> getCircuitBreakerStates() {
        Map<String, String> states = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb ->
                states.put(cb.getName(), cb.getState().name()));
        return states;
    }

    /**
     * Check if any circuit breaker is currently open.
     *
     * @return true if any circuit breaker is open
     */
    public boolean hasOpenCircuitBreakers() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .anyMatch(cb -> cb.getState() == CircuitBreaker.State.OPEN);
    }

    /**
     * Get the number of circuit breakers in each state.
     *
     * @return map of state names to counts
     */
    public Map<String, Integer> getCircuitBreakerStateCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("CLOSED", 0);
        counts.put("OPEN", 0);
        counts.put("HALF_OPEN", 0);

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            counts.merge(cb.getState().name(), 1, Integer::sum);
        });

        return counts;
    }

    /**
     * Health indicator for circuit breakers.
     * Returns healthy if no circuit breakers are open.
     *
     * @return true if all circuit breakers are closed
     */
    public boolean isHealthy() {
        return !hasOpenCircuitBreakers();
    }
}
