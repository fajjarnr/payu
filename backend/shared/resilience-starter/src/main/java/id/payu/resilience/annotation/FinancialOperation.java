package id.payu.resilience.annotation;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

import java.lang.annotation.*;

/**
 * Combined annotation for financial operations that applies multiple resilience patterns.
 * This annotation combines Circuit Breaker, Bulkhead, Retry, and Time Limiter
 * specifically optimized for financial service operations.
 *
 * <p>Usage example:
 * <pre>
 * @FinancialOperation(name = "transfer", fallbackMethod = "transferFallback")
 * public TransferResponse transfer(TransferRequest request) {
 *     // implementation
 * }
 *
 * private TransferResponse transferFallback(TransferRequest request, Exception ex) {
 *     // fallback implementation
 * }
 * </pre>
 *
 * <p>Default configurations applied:
 * <ul>
 *   <li>Circuit Breaker: slidingWindowSize=100, failureRateThreshold=50%, waitDurationInOpenState=10s</li>
 *   <li>Bulkhead: maxConcurrentCalls=20, maxWaitDuration=500ms</li>
 *   <li>Retry: maxAttempts=3, waitDuration=1s, exponentialBackoffMultiplier=2</li>
 *   <li>Time Limiter: timeoutDuration=5s</li>
 * </ul>
 *
 * <p>Business exceptions are automatically ignored from circuit breaker calculations
 * as they represent expected business rule violations, not system failures.
 *
 * @see CircuitBreaker
 * @see Bulkhead
 * @see Retry
 * @see TimeLimiter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
// BUG-BE-093: Resilience4j annotations do NOT support Spring property placeholders.
// Use hardcoded literal names that match configured instances in ResilienceAutoConfiguration.
@CircuitBreaker(name = "financial")
@Bulkhead(name = "financial")
@Retry(name = "financial")
@TimeLimiter(name = "financial")
public @interface FinancialOperation {

    /**
     * The name used to identify this financial operation.
     * This name is used as a prefix for all resilience patterns.
     * If not specified, the method name will be used.
     *
     * @return the operation name
     */
    String name() default "";

    /**
     * The name of the circuit breaker configuration to use.
     * Defaults to "default" which uses the financial services optimized settings.
     *
     * @return the circuit breaker name
     */
    String circuitBreaker() default "default";

    /**
     * The name of the bulkhead configuration to use.
     * Defaults to "default" which allows 20 concurrent calls.
     *
     * @return the bulkhead name
     */
    String bulkhead() default "default";

    /**
     * The name of the retry configuration to use.
     * Defaults to "default" which retries 3 times with exponential backoff.
     *
     * @return the retry name
     */
    String retry() default "default";

    /**
     * The name of the time limiter configuration to use.
     * Defaults to "default" which times out after 5 seconds.
     *
     * @return the time limiter name
     */
    String timeLimiter() default "default";

    /**
     * The method name to use as a fallback when any resilience pattern fails.
     * The fallback method must have the same parameters as the annotated method
     * plus an additional Exception parameter at the end.
     *
     * <p>Example signature:
     * <pre>
     * ReturnType fallbackMethod(ParamType param, Exception ex)
     * </pre>
     *
     * @return the fallback method name
     */
    String fallbackMethod() default "";

    /**
     * Whether this operation is idempotent and can be safely retried.
     * If true, uses more aggressive retry settings.
     * If false, uses minimal retry to avoid duplicate operations.
     *
     * @return true if the operation is idempotent
     */
    boolean idempotent() default false;

    /**
     * Whether this is a critical financial operation that requires stricter resilience.
     * Critical operations have lower failure thresholds and faster circuit breaker response.
     *
     * @return true if this is a critical operation
     */
    boolean critical() default false;
}
