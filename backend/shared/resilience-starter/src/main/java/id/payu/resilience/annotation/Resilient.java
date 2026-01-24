package id.payu.resilience.annotation;

import java.lang.annotation.*;

/**
 * Combined annotation for resilience patterns
 * Combines Circuit Breaker, Retry, and Bulkhead
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resilient {

    /**
     * Circuit breaker name
     */
    String circuitBreaker() default "";

    /**
     * Retry name
     */
    String retry() default "";

    /**
     * Bulkhead name
     */
    String bulkhead() default "";

    /**
     * Time limiter name
     */
    String timeLimiter() default "";

    /**
     * Fallback method name
     */
    String fallbackMethod() default "";
}
