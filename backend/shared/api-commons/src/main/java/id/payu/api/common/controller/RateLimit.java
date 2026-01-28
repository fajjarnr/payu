package id.payu.api.common.controller;

import id.payu.api.common.constant.ApiConstants;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to controller methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum number of requests allowed in the time window.
     */
    @AliasFor("value")
    int value() default ApiConstants.DEFAULT_RATE_LIMIT_PER_MINUTE;

    /**
     * Alias for value.
     */
    @AliasFor("value")
    int requests() default ApiConstants.DEFAULT_RATE_LIMIT_PER_MINUTE;

    /**
     * Time window in seconds.
     */
    int windowSeconds() default ApiConstants.DEFAULT_RATE_LIMIT_WINDOW_SECONDS;

    /**
     * Key prefix for Redis storage.
     */
    String keyPrefix() default "default";
}
