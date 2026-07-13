package id.payu.gateway.application.service;

import id.payu.gateway.config.GatewayConfig;
import id.payu.gateway.domain.State;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Per-service circuit breaker management.
 * <p>
 * Implements the circuit breaker pattern with three states:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failures exceeded threshold, requests fail fast with 503
 * - HALF_OPEN: After delay, allows limited requests to test recovery
 */
@ApplicationScoped
public class CircuitBreakerService {

    @Inject
    GatewayConfig config;

    private final ConcurrentHashMap<String, ServiceCircuitBreaker> breakers = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        Log.infof("CircuitBreakerService initialized (enabled=%s, failureRatio=%.2f, delay=%s, successThreshold=%d)",
                config.circuitBreaker().enabled(),
                config.circuitBreaker().failureRatio(),
                config.circuitBreaker().delay(),
                config.circuitBreaker().successThreshold());
    }

    /**
     * Execute a proxy call with circuit breaker protection.
     *
     * @param serviceName   the backend service name
     * @param action        the actual proxy call supplier
     * @param fallbackStatus the HTTP status code for the fallback response (typically 503)
     * @return Uni<jakarta.ws.rs.core.Response> with circuit breaker protection
     */
    public Uni<jakarta.ws.rs.core.Response> execute(String serviceName,
                                                      Supplier<Uni<jakarta.ws.rs.core.Response>> action) {
        if (!config.circuitBreaker().enabled()) {
            return action.get();
        }

        ServiceCircuitBreaker cb = getOrCreate(serviceName);

        // If circuit is OPEN, check if delay has elapsed
        if (cb.getState() == State.OPEN) {
            if (cb.shouldAttemptReset()) {
                cb.transitionTo(State.HALF_OPEN);
                Log.infof("Circuit breaker for '%s' transitioning to HALF_OPEN", serviceName);
            } else {
                long retryAfterSeconds = cb.getRetryAfterSeconds();
                Log.warnf("Circuit breaker for '%s' is OPEN — failing fast (retry-after: %ds)", serviceName, retryAfterSeconds);
                return Uni.createFrom().item(
                        jakarta.ws.rs.core.Response.status(503)
                                .header("Retry-After", String.valueOf(retryAfterSeconds))
                                .entity("{\"error\":\"CIRCUIT_OPEN\",\"message\":\"Service " +
                                        serviceName + " is temporarily unavailable. Retry after " +
                                        retryAfterSeconds + " seconds.\",\"status\":503,\"retryAfterSeconds\":" +
                                        retryAfterSeconds + "}")
                                .type("application/json")
                                .build()
                );
            }
        }

        return action.get()
                .onItem().invoke(response -> {
                    if (response.getStatus() >= 500) {
                        recordFailure(cb, serviceName);
                    } else {
                        recordSuccess(cb, serviceName);
                    }
                })
                .onFailure().recoverWithItem(throwable -> {
                    Log.errorf("Circuit breaker caught failure for '%s': %s", serviceName, throwable.getMessage());
                    recordFailure(cb, serviceName);
                    return jakarta.ws.rs.core.Response.status(503)
                            .entity("{\"error\":\"SERVICE_UNAVAILABLE\",\"message\":\"" +
                                    escapeJson(throwable.getMessage()) + "\",\"status\":503}")
                            .type("application/json")
                            .build();
                });
    }

    /**
     * Get the circuit breaker state for all services.
     */
    public Map<String, CircuitBreakerInfo> getCircuitStates() {
        return Collections.unmodifiableMap(
                breakers.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().toInfo()
                        ))
        );
    }

    /**
     * Get the circuit breaker state for a specific service.
     */
    public CircuitBreakerInfo getCircuitState(String serviceName) {
        ServiceCircuitBreaker cb = breakers.get(serviceName);
        if (cb == null) {
            return new CircuitBreakerInfo(State.CLOSED, 0, 0, 0, null, null, 0);
        }
        return cb.toInfo();
    }

    /**
     * Reset circuit breaker for a specific service (admin operation).
     */
    public void reset(String serviceName) {
        ServiceCircuitBreaker cb = breakers.get(serviceName);
        if (cb != null) {
            cb.reset();
            Log.infof("Circuit breaker for '%s' manually reset to CLOSED", serviceName);
        }
    }

    private ServiceCircuitBreaker getOrCreate(String serviceName) {
        return breakers.computeIfAbsent(serviceName,
                name -> new ServiceCircuitBreaker(
                        name,
                        config.circuitBreaker().failureRatio(),
                        config.circuitBreaker().delay(),
                        config.circuitBreaker().successThreshold()
                ));
    }

    private void recordFailure(ServiceCircuitBreaker cb, String serviceName) {
        cb.recordFailure();
        if (cb.getState() == State.HALF_OPEN) {
            cb.transitionTo(State.OPEN);
            Log.warnf("Circuit breaker for '%s' re-opened (failure in HALF_OPEN)", serviceName);
        } else if (cb.getState() == State.CLOSED && cb.isFailureThresholdExceeded()) {
            cb.transitionTo(State.OPEN);
            Log.warnf("Circuit breaker for '%s' OPENED (failure ratio exceeded: %d failures / %d total)",
                    serviceName, cb.failureCount.get(), cb.totalCount.get());
        }
    }

    private void recordSuccess(ServiceCircuitBreaker cb, String serviceName) {
        cb.recordSuccess();
        if (cb.getState() == State.HALF_OPEN && cb.isSuccessThresholdMet()) {
            cb.transitionTo(State.CLOSED);
            Log.infof("Circuit breaker for '%s' CLOSED (recovery confirmed)", serviceName);
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "Unknown error";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // ==================== Inner Classes ====================

    /**
     * DTO for circuit breaker state info (exposed via health endpoint).
     */
    public record CircuitBreakerInfo(
            State state,
            int failureCount,
            int successCount,
            int totalCount,
            Instant lastFailureTime,
            Instant openedAt,
            long retryAfterSeconds
    ) {}

    /**
     * Per-service circuit breaker state machine.
     */
    static class ServiceCircuitBreaker {
        private final String serviceName;
        private final double failureRatio;
        private final Duration delay;
        private final int successThreshold;
        private static final int VOLUME_THRESHOLD = 10; // minimum requests before checking ratio

        final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger totalCount = new AtomicInteger(0);
        final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
        volatile Instant lastFailureTime = null;
        volatile Instant openedAt = null;

        ServiceCircuitBreaker(String serviceName, double failureRatio, Duration delay, int successThreshold) {
            this.serviceName = serviceName;
            this.failureRatio = failureRatio;
            this.delay = delay;
            this.successThreshold = successThreshold;
        }

        State getState() {
            return state.get();
        }

        void transitionTo(State newState) {
            State old = state.getAndSet(newState);
            if (newState == State.OPEN) {
                openedAt = Instant.now();
            } else if (newState == State.CLOSED) {
                reset();
            } else if (newState == State.HALF_OPEN) {
                halfOpenSuccessCount.set(0);
            }
        }

        void recordFailure() {
            failureCount.incrementAndGet();
            totalCount.incrementAndGet();
            lastFailureTime = Instant.now();
        }

        void recordSuccess() {
            successCount.incrementAndGet();
            totalCount.incrementAndGet();
            if (state.get() == State.HALF_OPEN) {
                halfOpenSuccessCount.incrementAndGet();
            }
        }

        boolean shouldAttemptReset() {
            return openedAt != null &&
                    Instant.now().isAfter(openedAt.plus(delay));
        }

        /**
         * Calculate remaining seconds until the circuit breaker may attempt reset.
         * Used for the Retry-After HTTP header.
         */
        long getRetryAfterSeconds() {
            if (openedAt == null) {
                return delay.getSeconds();
            }
            Instant resetAt = openedAt.plus(delay);
            long remaining = java.time.Duration.between(Instant.now(), resetAt).getSeconds();
            return Math.max(1, remaining); // at least 1 second
        }

        boolean isFailureThresholdExceeded() {
            int total = totalCount.get();
            if (total < VOLUME_THRESHOLD) {
                return false;
            }
            return (double) failureCount.get() / total >= failureRatio;
        }

        boolean isSuccessThresholdMet() {
            return halfOpenSuccessCount.get() >= successThreshold;
        }

        void reset() {
            state.set(State.CLOSED);
            failureCount.set(0);
            successCount.set(0);
            totalCount.set(0);
            halfOpenSuccessCount.set(0);
            openedAt = null;
        }

        CircuitBreakerInfo toInfo() {
            return new CircuitBreakerInfo(
                    state.get(),
                    failureCount.get(),
                    successCount.get(),
                    totalCount.get(),
                    lastFailureTime,
                    openedAt,
                    state.get() == State.OPEN ? getRetryAfterSeconds() : 0
            );
        }
    }
}
