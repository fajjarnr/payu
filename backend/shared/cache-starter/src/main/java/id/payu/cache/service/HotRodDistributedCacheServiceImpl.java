package id.payu.cache.service;

import id.payu.cache.properties.CacheProperties;
import org.infinispan.client.hotrod.RemoteCacheManager;

/**
 * Native Infinispan 16 Hot Rod implementation of the shared cache contract.
 */
public final class HotRodDistributedCacheServiceImpl extends DistributedCacheService {

    @SuppressWarnings("unchecked")
    public HotRodDistributedCacheServiceImpl(
            RemoteCacheManager remoteCacheManager,
            CacheProperties properties) {
        super(() -> {
            if (!remoteCacheManager.isStarted()) {
                remoteCacheManager.start();
            }
            return remoteCacheManager.getCache(properties.getHotrod().getCacheName());
        }, properties);
    }
}
