# Resilience Patterns Reference

## Circuit Breaker Implementation

### Configuration (resilience-starter)

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 100
        failureRateThreshold: 50
        slowCallRateThreshold: 80
        slowCallDurationThreshold: 2s
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 10
        minimumNumberOfCalls: 20
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.HttpServerErrorException
        ignoreExceptions:
          - id.payu.shared.exception.BusinessException
          
      # Stricter config for critical services (BI-FAST, Payment Gateway)
      critical:
        failureRateThreshold: 30
        slowCallDurationThreshold: 1s
        waitDurationInOpenState: 60s
        
    instances:
      bifast-service:
        baseConfig: critical
      partner-api:
        baseConfig: default
        failureRateThreshold: 40
```

### Usage Pattern

```java
@Service
@Slf4j
public class TransferService {
    
    private final BiFastClient biFastClient;
    private final FallbackService fallbackService;
    
    @CircuitBreaker(name = "bifast-service", fallbackMethod = "transferFallback")
    @Retry(name = "bifast-service", fallbackMethod = "transferFallback")
    @TimeLimiter(name = "bifast-service")
    public CompletableFuture<TransferResult> executeTransfer(TransferRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Executing BI-FAST transfer: {}", request.getIdempotencyKey());
            return biFastClient.initiateTransfer(request);
        });
    }
    
    /**
     * Fallback: Queue for retry or offer alternative channel
     */
    private CompletableFuture<TransferResult> transferFallback(
            TransferRequest request, 
            Throwable throwable) {
        
        log.warn("BI-FAST circuit open, activating fallback for: {}", 
            request.getIdempotencyKey(), throwable);
        
        // Option 1: Queue for retry
        if (isRetryable(throwable)) {
            retryQueue.enqueue(request);
            return CompletableFuture.completedFuture(
                TransferResult.pending("Queued for retry - BI-FAST temporarily unavailable")
            );
        }
        
        // Option 2: Suggest alternative channel
        return CompletableFuture.completedFuture(
            TransferResult.fallback("Please use QRIS or manual transfer")
        );
    }
}
```

### Circuit Breaker States

```
     ┌─────────────────────────────────────────────────────┐
     │                  CIRCUIT BREAKER STATES             │
     └─────────────────────────────────────────────────────┘
     
         ┌──────────┐    failure rate     ┌──────────┐
         │  CLOSED  │ ─────> 50% ─────────►│   OPEN   │
         │ (Normal) │                      │(Blocking)│
         └────┬─────┘                      └────┬─────┘
              │                                  │
              │                                  │ wait 30s
              │                                  ▼
              │                           ┌──────────┐
              │                           │HALF_OPEN │
              │                           │ (Testing)│
              │                           └────┬─────┘
              │                                │
              │       success rate > 50%       │ failure
              │◄───────────────────────────────┤
              │                                │
              │                                ▼
              │                           ┌──────────┐
              └───────────────────────────│   OPEN   │
                                          └──────────┘
```

---

## Retry Strategy

### Configuration

```yaml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
        ignoreExceptions:
          - id.payu.shared.exception.BusinessException
          - id.payu.shared.exception.ValidationException
          
      # Idempotent operations only
      idempotent:
        maxAttempts: 5
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        maxWaitDuration: 30s
```

### Idempotency Pattern

```java
@Service
public class IdempotentTransferService {
    
    private final RedisTemplate<String, String> redis;
    private final TransferRepository repository;
    
    @Transactional
    public TransferResult executeTransfer(String idempotencyKey, TransferRequest request) {
        // 1. Check if already processed
        String existing = redis.opsForValue().get("idem:" + idempotencyKey);
        if (existing != null) {
            return repository.findByIdempotencyKey(idempotencyKey)
                .map(TransferResult::fromEntity)
                .orElseThrow(() -> new IllegalStateException("Inconsistent state"));
        }
        
        // 2. Acquire lock
        Boolean locked = redis.opsForValue()
            .setIfAbsent("lock:" + idempotencyKey, "1", Duration.ofMinutes(5));
        
        if (!Boolean.TRUE.equals(locked)) {
            throw new ConcurrentModificationException("Transfer in progress");
        }
        
        try {
            // 3. Execute transfer
            TransferResult result = doTransfer(request);
            
            // 4. Store idempotency key
            redis.opsForValue().set(
                "idem:" + idempotencyKey, 
                result.getTransactionId(),
                Duration.ofDays(7)
            );
            
            return result;
            
        } finally {
            redis.delete("lock:" + idempotencyKey);
        }
    }
}
```

---

## Bulkhead Pattern

### Thread Pool Isolation

```yaml
resilience4j:
  bulkhead:
    configs:
      default:
        maxConcurrentCalls: 25
        maxWaitDuration: 0ms  # Fail fast
        
    instances:
      bifast-calls:
        maxConcurrentCalls: 50
        maxWaitDuration: 100ms
        
      partner-api:
        maxConcurrentCalls: 20
        maxWaitDuration: 50ms

  thread-pool-bulkhead:
    configs:
      default:
        maxThreadPoolSize: 10
        coreThreadPoolSize: 5
        queueCapacity: 100
        
    instances:
      heavy-processing:
        maxThreadPoolSize: 20
        coreThreadPoolSize: 10
        queueCapacity: 200
```

### Isolation Strategy

```
┌────────────────────────────────────────────────────────────┐
│                    THREAD POOL ISOLATION                    │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Main Thread Pool (Tomcat)                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  200 threads for HTTP requests                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│      ┌───────────────────┼───────────────────┐             │
│      ▼                   ▼                   ▼             │
│  ┌────────┐         ┌────────┐         ┌────────┐         │
│  │BI-FAST │         │Partner │         │ Heavy  │         │
│  │ Pool   │         │  API   │         │Process │         │
│  │(50 thr)│         │(20 thr)│         │(20 thr)│         │
│  └────────┘         └────────┘         └────────┘         │
│                                                            │
│  ✅ If BI-FAST is slow, Partner API still works!          │
└────────────────────────────────────────────────────────────┘
```

---

## Rate Limiting

### Configuration

```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0ms
        
    instances:
      # Public API endpoints
      public-api:
        limitForPeriod: 1000
        limitRefreshPeriod: 1s
        
      # Per-user rate limit
      user-api:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        
      # Critical operations
      transfer-api:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
```

### Per-User Rate Limiting

```java
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    
    private final RateLimiterRegistry registry;
    
    @PostMapping
    public ResponseEntity<TransferResult> createTransfer(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody TransferRequest request) {
        
        // Get or create per-user rate limiter
        RateLimiter limiter = registry.rateLimiter(
            "user-" + userId,
            RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build()
        );
        
        return RateLimiter.decorateSupplier(limiter, () -> {
            return ResponseEntity.ok(transferService.execute(request));
        }).get();
    }
}
```

---

## Timeout Management

### Tiered Timeouts

| Operation Type | Connect Timeout | Read Timeout | Total Timeout |
|:---------------|:----------------|:-------------|:--------------|
| Internal Service | 500ms | 2s | 3s |
| BI-FAST | 1s | 5s | 10s |
| Partner API | 2s | 10s | 15s |
| Report Generation | 5s | 60s | 120s |

### Configuration

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient internalServiceClient() {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                    .responseTimeout(Duration.ofSeconds(2))
            ))
            .build();
    }
    
    @Bean
    public WebClient bifastClient() {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                    .responseTimeout(Duration.ofSeconds(5))
            ))
            .build();
    }
}
```

---

## Graceful Degradation Patterns

### Feature Flags for Degradation

```java
@Service
public class BalanceService {
    
    private final FeatureFlagService featureFlags;
    private final WalletClient walletClient;
    private final CacheService cache;
    
    public BalanceResponse getBalance(String accountId) {
        // Full service mode
        if (featureFlags.isEnabled("balance-realtime")) {
            try {
                return walletClient.getRealtimeBalance(accountId);
            } catch (Exception e) {
                log.warn("Realtime balance failed, falling back to cache", e);
            }
        }
        
        // Degraded mode: cached balance
        if (featureFlags.isEnabled("balance-cached")) {
            Optional<BalanceResponse> cached = cache.get("balance:" + accountId);
            if (cached.isPresent()) {
                return cached.get().withStaleIndicator(true);
            }
        }
        
        // Minimal mode: last known balance from DB
        return balanceRepository.findLastKnown(accountId)
            .map(b -> BalanceResponse.stale(b, "Data may be outdated"))
            .orElse(BalanceResponse.unavailable());
    }
}
```

### Degradation Matrix

| Service State | Balance | Transfer | History | Notifications |
|:--------------|:--------|:---------|:--------|:--------------|
| **Healthy** | Real-time | Full | Full | Push + SMS |
| **Degraded** | Cached (5m) | Essential only | Last 7 days | Push only |
| **Critical** | Last known | Disabled | Disabled | SMS critical only |
| **Maintenance** | Static message | Disabled | Disabled | Maintenance banner |
