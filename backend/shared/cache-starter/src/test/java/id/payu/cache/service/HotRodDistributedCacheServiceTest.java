package id.payu.cache.service;

import id.payu.cache.properties.CacheProperties;
import java.time.Duration;
import java.util.Map;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.MetadataValue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HotRodDistributedCacheServiceTest {

    @Test
    void shouldStoreJsonStringAndReadTypedValue() {
        RemoteCache<String, Object> remoteCache = mock(RemoteCache.class);
        DistributedCacheService cache = new DistributedCacheService(() -> remoteCache, new CacheProperties());
        Map<String, String> value = Map.of("partnerId", "partner-1");

        cache.put("partner:1", value, Duration.ofMinutes(1));

        ArgumentCaptor<Object> stored = ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(remoteCache).put(
                org.mockito.ArgumentMatchers.eq("partner:1"),
                stored.capture(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
        assertThat(stored.getValue()).isInstanceOf(String.class);

        when(remoteCache.get(anyString())).thenReturn(stored.getValue());

        assertThat(cache.get("partner:1", Map.class)).isEqualTo(value);
    }

    @Test
    void shouldIncrementWithVersionedCompareAndSet() {
        RemoteCache<String, Object> remoteCache = mock(RemoteCache.class);
        MetadataValue<Object> metadata = mock(MetadataValue.class);
        when(metadata.getValue()).thenReturn("4");
        when(metadata.getVersion()).thenReturn(7L);
        when(remoteCache.getWithMetadata("rate:1")).thenReturn(metadata);
        when(remoteCache.replaceWithVersion("rate:1", "5", 7L)).thenReturn(true);
        DistributedCacheService cache = new DistributedCacheService(() -> remoteCache, new CacheProperties());

        assertThat(cache.increment("rate:1", Duration.ofMinutes(1))).isEqualTo(5L);
    }
}
