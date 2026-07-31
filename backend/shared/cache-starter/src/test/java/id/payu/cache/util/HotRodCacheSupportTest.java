package id.payu.cache.util;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HotRodCacheSupportTest {

    @Test
    void shouldStartManagerWhenNotStarted() {
        RemoteCacheManager manager = mock(RemoteCacheManager.class);
        when(manager.isStarted()).thenReturn(false);

        HotRodCacheSupport.ensureStarted(manager);

        verify(manager).start();
    }

    @Test
    void shouldNotStartAlreadyStartedManager() {
        RemoteCacheManager manager = mock(RemoteCacheManager.class);
        when(manager.isStarted()).thenReturn(true);

        HotRodCacheSupport.ensureStarted(manager);

        verify(manager, never()).start();
    }

    @Test
    void shouldTolerateNullManager() {
        assertThatCode(() -> HotRodCacheSupport.ensureStarted(null))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldStartManagerAndReturnConfiguredCache() {
        RemoteCacheManager manager = mock(RemoteCacheManager.class);
        when(manager.isStarted()).thenReturn(false);
        Configuration configuration = mock(Configuration.class);
        RemoteCacheConfiguration cacheConfiguration = mock(RemoteCacheConfiguration.class);
        when(manager.getConfiguration()).thenReturn(configuration);
        when(configuration.remoteCaches())
                .thenReturn(Collections.singletonMap("payu", cacheConfiguration));

        HotRodCacheSupport.cache(manager);

        verify(manager).start();
        verify(manager).getCache("payu");
    }
}
