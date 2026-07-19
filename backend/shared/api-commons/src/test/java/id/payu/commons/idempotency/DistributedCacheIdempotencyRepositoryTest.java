package id.payu.commons.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.payu.cache.service.DistributedAtomicCache;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistributedCacheIdempotencyRepositoryTest {

    @Test
    void shouldAtomicallySaveAnIdempotencyEntryWhenKeyIsAbsent() {
        DistributedAtomicCache cache = mock(DistributedAtomicCache.class);
        when(cache.putStringIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(60)))).thenReturn(true);
        IdempotencyProperties properties = new IdempotencyProperties();
        properties.getRedis().setKeyPrefix("idempotency");
        DistributedCacheIdempotencyRepository repository = new DistributedCacheIdempotencyRepository(
                cache, new ObjectMapper().findAndRegisterModules(), properties);

        String key = "550e8400-e29b-41d4-a716-446655440000";
        repository.saveIfAbsent(IdempotencyKey.of(key),
                IdempotencyEntry.inProgress(key, "fingerprint"), 60);

        verify(cache).putStringIfAbsent(
                eq("idempotency:" + key), anyString(), eq(Duration.ofSeconds(60)));
    }
}
