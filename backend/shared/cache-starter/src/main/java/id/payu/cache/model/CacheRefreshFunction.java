package id.payu.cache.model;

/**
 * Functional interface for cache refresh operations.
 * Used by the stale-while-revalidate pattern.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@literal @Bean}
 * public CacheRefreshFunction accountRefreshFunction() {
 *     return key -> accountService.refreshAccount((String) key);
 * }
 * </pre>
 */
@FunctionalInterface
public interface CacheRefreshFunction<T> {

    /**
     * Refresh the cached value for the given key.
     *
     * @throws Exception if refresh fails
     */
    T refresh(Object key) throws Exception;
}
