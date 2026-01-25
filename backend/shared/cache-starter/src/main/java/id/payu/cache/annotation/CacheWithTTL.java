package id.payu.cache.annotation;

import id.payu.cache.model.CacheRefreshFunction;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for caching method return values with custom TTL and
 * stale-while-revalidate support.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Custom TTL per method or cache</li>
 *   <li>Stale-while-revalidate pattern for improved performance</li>
 *   <li>Automatic key generation using SpEL</li>
   <li>Conditional caching based on result</li>
 *   <li>Cache invalidation support</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * // Basic usage
 * {@literal @}CacheWithTTL(cacheName = "accounts", ttl = 10, timeUnit = TimeUnit.MINUTES)
 * public Account getAccount(String accountId) {
 *     return accountRepository.findById(accountId);
 * }
 *
 * // With custom key
 * {@literal @}CacheWithTTL(
 *     cacheName = "accounts",
 *     key = "'account:' + #accountId",
 *     ttl = 10,
 *     timeUnit = TimeUnit.MINUTES
 * )
 * public Account getAccount(String accountId) {
 *     return accountRepository.findById(accountId);
 * }
 *
 * // With stale-while-revalidate
 * {@literal @}CacheWithTTL(
 *     cacheName = "balances",
 *     ttl = 30,
 *     timeUnit = TimeUnit.SECONDS,
 *     softTtlMultiplier = 0.5,
 *     refresh = "@accountService#refreshBalance"
 * )
 * public Balance getBalance(String accountId) {
 *     return balanceRepository.findByAccountId(accountId);
 * }
 *
 * // Conditional caching
 * {@literal @}CacheWithTTL(
 *     cacheName = "accounts",
 *     ttl = 10,
 *     timeUnit = TimeUnit.MINUTES,
 *     condition = "#accountId.length() > 5"
 * )
 * public Account getAccount(String accountId) {
 *     return accountRepository.findById(accountId);
 * }
 * </pre>
 *
 * @see CacheRefreshFunction
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheWithTTL {

    /**
     * Name of the cache to use.
     * @deprecated Use {@link #value()} instead.
     */
    @AliasFor("value")
    String cacheName() default "";

    /**
     * Name of the cache to use - primary attribute.
     * This is the default attribute used when specifying just the cache name.
     */
    String value() default "";

    /**
     * Spring Expression Language (SpEL) for custom cache key generation.
     * Default: generates key from method parameters.
     */
    String key() default "";

    /**
     * Time-to-live for cache entries.
     */
    long ttl();

    /**
     * Time unit for TTL.
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * Enable stale-while-revalidate pattern.
     * When enabled, serves stale data while asynchronously refreshing.
     */
    boolean staleWhileRevalidate() default false;

    /**
     * Multiplier for soft TTL (soft TTL = hard TTL * multiplier).
     * Default 0.5 means soft TTL is 50% of hard TTL.
     */
    double softTtlMultiplier() default 0.5;

    /**
     * Bean name of the refresh function for stale-while-revalidate.
     * Format: "beanName#methodName" or fully qualified method reference.
     */
    String refresh() default "";

    /**
     * SpEL expression for conditional caching.
     * Cache is only applied if condition evaluates to true.
     */
    String condition() default "";

    /**
     * SpEL expression for conditional caching based on result.
     * Cache is only applied if result evaluates to true.
     */
    String unless() default "";

    /**
     * Whether to sync cache access (prevent cache stampede).
     */
    boolean sync() default true;
}
