package id.payu.cache.util;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import java.util.Set;

/**
 * Shared helper for callers that use the Hot Rod client directly (health
 * indicators) so the lazily-started {@link RemoteCacheManager} is started on
 * demand. Mirrors the lazy-start contract of
 * {@code HotRodDistributedCacheServiceImpl#getCache()} (ARCH-007).
 */
public final class HotRodCacheSupport {

    private HotRodCacheSupport() {
    }

    public static void ensureStarted(RemoteCacheManager manager) {
        if (manager != null && !manager.isStarted()) {
            manager.start();
        }
    }

    /**
     * Ensures the manager is started and returns the single remote cache
     * configured by the starter (e.g. {@code payu}). Falls back to the default
     * cache only when no named remote cache is configured.
     */
    public static RemoteCache<String, String> cache(RemoteCacheManager manager) {
        ensureStarted(manager);
        Set<String> cacheNames = manager.getConfiguration().remoteCaches().keySet();
        if (cacheNames.size() == 1) {
            return manager.getCache(cacheNames.iterator().next());
        }
        return manager.getCache();
    }
}
