package id.payu.cache.service;

import id.payu.cache.properties.CacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private DistributedCacheService distributedCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        distributedCacheService = new DistributedCacheService(redisTemplate, new CacheProperties());
    }

    @Test
    void shouldReadCacheEntryWithSerializerMetadata() {
        String key = "wallet:account:acc-1";
        Instant now = Instant.now();

        Map<String, Object> cachedWallet = new LinkedHashMap<>();
        cachedWallet.put("@class", SamplePayload.class.getName());
        cachedWallet.put("id", "wallet-1");
        cachedWallet.put("accountId", "acc-1");
        cachedWallet.put("balance", new BigDecimal("125000.50"));

        Map<String, Object> cacheEntry = new LinkedHashMap<>();
        cacheEntry.put("@class", "id.payu.cache.model.CacheEntry");
        cacheEntry.put("value", cachedWallet);
        cacheEntry.put("createdAt", now);
        cacheEntry.put("softTtl", now.plusSeconds(30));
        cacheEntry.put("hardTtl", now.plusSeconds(60));
        cacheEntry.put("version", 0);

        when(valueOperations.get(key)).thenReturn(cacheEntry);

        SamplePayload result = distributedCacheService.get(key, SamplePayload.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("wallet-1");
        assertThat(result.getAccountId()).isEqualTo("acc-1");
        assertThat(result.getBalance()).isEqualByComparingTo("125000.50");
    }

    @Test
    void shouldReadDirectValueMapWithSerializerMetadata() {
        String key = "wallet:id:wallet-2";

        Map<String, Object> cachedWallet = new LinkedHashMap<>();
        cachedWallet.put("@class", SamplePayload.class.getName());
        cachedWallet.put("id", "wallet-2");
        cachedWallet.put("accountId", "acc-2");
        cachedWallet.put("balance", new BigDecimal("42000.00"));

        when(valueOperations.get(key)).thenReturn(cachedWallet);

        SamplePayload result = distributedCacheService.get(key, SamplePayload.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("wallet-2");
        assertThat(result.getAccountId()).isEqualTo("acc-2");
        assertThat(result.getBalance()).isEqualByComparingTo("42000.00");
    }

    @Test
    void shouldUnwrapLegacyValueOnlyWrapper() {
        String key = "wallet:legacy:wallet-3";

        Map<String, Object> wrappedWallet = new LinkedHashMap<>();
        wrappedWallet.put("id", "wallet-3");
        wrappedWallet.put("accountId", "acc-3");
        wrappedWallet.put("balance", new BigDecimal("9000.00"));

        Map<String, Object> legacyWrapper = new LinkedHashMap<>();
        legacyWrapper.put("@class", SamplePayload.class.getName());
        legacyWrapper.put("value", wrappedWallet);

        when(valueOperations.get(key)).thenReturn(legacyWrapper);

        SamplePayload result = distributedCacheService.get(key, SamplePayload.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("wallet-3");
        assertThat(result.getAccountId()).isEqualTo("acc-3");
        assertThat(result.getBalance()).isEqualByComparingTo("9000.00");
    }

    public static class SamplePayload {

        private String id;
        private String accountId;
        private BigDecimal balance;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }
}