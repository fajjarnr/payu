package id.payu.cache.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Annotation for cache invalidation.
 *
 * <p>Usage example:</p>
 * <pre>
 * // Invalidate single cache entry by key
 * {@literal @}CacheInvalidate(cacheName = "accounts", key = "'account:' + #accountId")
 * public void updateAccount(String accountId, AccountUpdateRequest request) {
 *     accountRepository.update(accountId, request);
 * }
 *
 * // Invalidate all entries in a cache
 * {@literal @}CacheInvalidate(cacheName = "accounts", allEntries = true)
 * public void clearAllAccounts() {
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInvalidate {

    /**
     * Name of the cache to invalidate.
     */
    String cacheName();

    /**
     * Spring Expression Language (SpEL) for cache key.
     * Required unless allEntries is true.
     */
    String key() default "";

    /**
     * Whether to invalidate all entries in the cache.
     */
    boolean allEntries() default false;

    /**
     * Alias for cacheName.
     */
    @AliasFor("cacheName")
    String value() default "";
}
