# PayU Cache Starter

Shared distributed caching module for PayU microservices with **stale-while-revalidate** pattern support.

## Features

- **Multi-layer Caching**: Redis (distributed) + Caffeine (local fallback)
- **Stale-While-Revalidate**: Serve stale data while asynchronously refreshing
- **Custom TTL per Cache**: Configure different TTLs for different data types
- **Cache Stampede Prevention**: Synchronized cache access for hot keys
- **Automatic Fallback**: Local cache when Redis is unavailable
- **Metrics Integration**: Built-in Micrometer metrics
- **Spring Boot Auto-Configuration**: Zero configuration setup

## Installation

Add the dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>id.payu</groupId>
    <artifactId>cache-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### Minimal Configuration

```yaml
payu:
  cache:
    enabled: true
    redis:
      host: localhost
      port: 6379
```

### Full Configuration

```yaml
payu:
  cache:
    enabled: true
    default-ttl: 5m
    redis:
      host: redis.payu.svc
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 5s
      command-timeout: 3s
      pool-size: 10
      ssl: false
      # For cluster mode
      cluster: false
      cluster-nodes: "redis-node1:6379,redis-node2:6379"
      # For sentinel mode
      sentinel-master: mymaster
    stale-while-revalidate:
      enabled: true
      soft-ttl-multiplier: 0.5
      refresh-thread-pool-size: 4
    caches:
      accounts:
        ttl: 10m
        stale-while-revalidate: true
      balances:
        ttl: 30s
        stale-while-revalidate: true
      transactions:
        ttl: 5m
    local-cache:
      enabled: true
      max-size: 1000
      ttl: 1m
```

## Usage

### 1. Using CacheService Directly

```java
@Service
@RequiredArgsConstructor
public class AccountService {

    private final CacheService cacheService;
    private final AccountRepository accountRepository;

    public Account getAccount(String accountId) {
        return cacheService.get(
            "account:" + accountId,
            Account.class,
            () -> accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId))
        );
    }

    public Account getAccountWithStaleCache(String accountId) {
        return cacheService.getWithStaleWhileRevalidate(
            "account:" + accountId,
            Account.class,
            () -> accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId)),
            Duration.ofSeconds(15),  // Soft TTL - serve stale data
            Duration.ofSeconds(30)   // Hard TTL - must refresh
        );
    }

    public void updateAccount(String accountId, AccountUpdateRequest request) {
        accountRepository.update(accountId, request);
        cacheService.invalidate("account:" + accountId);
    }
}
```

### 2. Using @CacheWithTTL Annotation

```java
@Service
public class AccountService {

    @CacheWithTTL(
        cacheName = "accounts",
        ttl = 10,
        timeUnit = TimeUnit.MINUTES
    )
    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @CacheWithTTL(
        cacheName = "accounts",
        key = "'account:' + #accountId",
        ttl = 10,
        timeUnit = TimeUnit.MINUTES
    )
    public Account getAccountWithCustomKey(String accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @CacheWithTTL(
        cacheName = "balances",
        ttl = 30,
        timeUnit = TimeUnit.SECONDS,
        softTtlMultiplier = 0.5,
        staleWhileRevalidate = true,
        sync = true
    )
    public Balance getBalance(String accountId) {
        return balanceRepository.findByAccountId(accountId);
    }

    @CacheInvalidate(cacheName = "accounts", key = "'account:' + #accountId")
    public void updateAccount(String accountId, AccountUpdateRequest request) {
        accountRepository.update(accountId, request);
    }

    @CacheInvalidate(cacheName = "accounts", allEntries = true)
    public void clearAllAccountsCache() {
        // Clear all cached accounts
    }
}
```

### 3. Manual Refresh for Stale Data

```java
@Service
public class BalanceService {

    @Autowired
    private CacheService cacheService;

    @Scheduled(fixedDelay = 60000) // Every minute
    public void refreshStaleBalances() {
        // Find keys that need refresh
        Set<String> keys = findStaleKeys("balances");

        for (String key : keys) {
            cacheService.getAndRefresh(
                key,
                Balance.class,
                () -> balanceRepository.findByAccountId(extractAccountId(key)),
                Duration.ofSeconds(15),
                Duration.ofSeconds(30)
            );
        }
    }
}
```

## Stale-While-Revalidate Pattern

The stale-while-revalidate pattern allows you to serve cached data even after it expires (stale), while asynchronously refreshing the cache in the background.

### How It Works

```
Time: 0s ---------- 15s ---------- 30s ---------- 45s
      Fresh          Stale          Expired        Fresh
      (serve)        (serve+async)  (refresh)      (serve)
```

1. **Fresh (0-15s)**: Cache is fresh, serve immediately
2. **Stale (15-30s)**: Cache is stale, serve immediately and trigger async refresh
3. **Expired (30s+)**: Cache is expired, block and refresh

### Configuration

```java
@CacheWithTTL(
    cacheName = "balances",
    ttl = 30,              // Hard TTL (seconds)
    timeUnit = TimeUnit.SECONDS,
    softTtlMultiplier = 0.5, // Soft TTL = 30 * 0.5 = 15 seconds
    staleWhileRevalidate = true
)
```

## Metrics

The cache starter exposes the following metrics:

| Metric | Description |
|--------|-------------|
| `cache.distributed.hits` | Cache hits from Redis |
| `cache.distributed.misses` | Cache misses from Redis |
| `cache.distributed.stale` | Stale entries served |
| `cache.distributed.errors` | Redis errors |
| `cache.distributed.get` | Time spent getting from cache |
| `cache.distributed.put` | Time spent putting to cache |
| `cache.local.fallback` | Fallbacks to local cache |
| `cache.local.writes` | Writes to local cache |
| `cache.aspect.hit` | Annotation-based cache hits |
| `cache.aspect.miss` | Annotation-based cache misses |
| `cache.aspect.refresh` | Async refresh operations |

## Testing

```java
@SpringBootTest
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Test
    void testGetAndPut() {
        String key = "test:key";
        String value = "test-value";

        cacheService.put(key, value, Duration.ofMinutes(5));
        String retrieved = cacheService.get(key, String.class);

        assertEquals(value, retrieved);
    }

    @Test
    void testStaleWhileRevalidate() {
        String key = "test:swr";

        // Put with soft TTL 1s, hard TTL 2s
        cacheService.put(key, "value", Duration.ofSeconds(1), Duration.ofSeconds(2));

        // Should be fresh immediately
        Object result = cacheService.getWithStaleWhileRevalidate(
            key,
            String.class,
            () -> "fresh-value",
            Duration.ofSeconds(1),
            Duration.ofSeconds(2)
        );
        assertEquals("value", result);

        // Wait for stale
        Thread.sleep(1500);

        // Should return stale value
        result = cacheService.getWithStaleWhileRevalidate(
            key,
            String.class,
            () -> "fresh-value",
            Duration.ofSeconds(1),
            Duration.ofSeconds(2)
        );
        assertEquals("value", result); // Stale value served
    }
}
```

## Production Considerations

### 1. Redis Cluster Configuration

For production, use Redis Cluster for high availability:

```yaml
payu:
  cache:
    redis:
      cluster: true
      cluster-nodes: "redis-node1.payu.svc:6379,redis-node2.payu.svc:6379,redis-node3.payu.svc:6379"
      password: ${REDIS_PASSWORD}
```

### 2. Sentinel Configuration

For automatic failover:

```yaml
payu:
  cache:
    redis:
      sentinel-master: payu-master
      cluster-nodes: "sentinel1.payu.svc:26379,sentinel2.payu.svc:26379,sentinel3.payu.svc:26379"
      password: ${REDIS_PASSWORD}
```

### 3. Connection Pooling

Adjust pool size based on traffic:

```yaml
payu:
  cache:
    redis:
      pool-size: 50  # Increase for high-traffic services
```

### 4. TTL Strategy

Choose appropriate TTLs based on data characteristics:

| Data Type | Recommended TTL | SWR |
|-----------|----------------|-----|
| User Profile | 10-30 minutes | Yes |
| Account Balance | 30-60 seconds | Yes |
| Transaction List | 5-10 minutes | No |
| Reference Data | 1-24 hours | Yes |
| Session Data | 15-30 minutes | No |

## Troubleshooting

### Cache Not Working

1. Check if caching is enabled: `payu.cache.enabled=true`
2. Verify Redis connectivity: `redis-cli -h <host> -p <port> ping`
3. Check logs for errors: `grep "cache" logs/application.log`

### High Memory Usage

1. Reduce local cache size: `payu.cache.local-cache.max-size`
2. Reduce TTLs for frequently accessed data
3. Enable cache eviction policies

### Stale Data Issues

1. Adjust soft TTL multiplier: `payu.cache.stale-while-revalidate.soft-ttl-multiplier`
2. Increase async refresh thread pool: `payu.cache.stale-while-revalidate.refresh-thread-pool-size`
3. Implement manual refresh for critical data

## License

Copyright (c) 2026 PayU. All rights reserved.
