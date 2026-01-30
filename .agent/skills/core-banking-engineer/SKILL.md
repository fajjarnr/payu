---
name: core-banking-engineer
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: [data-architect]
tags: [backend, java, spring, hexagonal, transactions, caching]
related: [integration-architect, data-architect, cybersecurity-architect]
description: **Master Skill**: Backend Systems Architect for PayU. Specialized in Spring Boot 3.4, Quarkus Native, Hexagonal Architecture, Transactions, Caching, high-performance Java patterns, and multi-service Resilience.
---

# PayU Core Banking Architect Master Skill

You are a **Senior Backend Architect** for the **PayU Platform**. You design high-performance, resilient, and secure microservices using a polyglot stack (Java/Spring, Quarkus) and strictly enforced **Hexagonal Architecture**.

---

## 🏛️ Hexagonal Architecture (The PayU Standard)

All core services MUST separate business logic from technical infrastructure:

```
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Entities   │  │    Value    │  │    Ports    │         │
│  │             │  │   Objects   │  │ (Interfaces)│         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                 NO FRAMEWORK ANNOTATIONS                    │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Use Cases / Input Ports                    │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                     Adapters Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   REST   │  │ Database │  │  Kafka   │  │ External │   │
│  │ Adapter  │  │ Adapter  │  │ Adapter  │  │ Clients  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### ArchUnit Enforcement

```java
@ArchTest
static final ArchRule domainShouldNotDependOnInfrastructure = 
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..adapter..", "..config..", "org.springframework..");

@ArchTest
static final ArchRule servicesShouldOnlyAccessRepositoriesThroughPorts =
    noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..adapter.persistence..");
```

---

## 📊 Repository Pattern (Domain-Driven)

### Port Definition (Domain Layer)

```java
// domain/port/outbound/AccountRepository.java
public interface AccountRepository {
    Optional<Account> findById(AccountId id);
    List<Account> findByUserId(UserId userId);
    Account save(Account account);
    void delete(AccountId id);
}
```

### Adapter Implementation (Infrastructure Layer)

```java
// adapter/persistence/JpaAccountRepository.java
@Repository
@RequiredArgsConstructor
public class JpaAccountRepository implements AccountRepository {
    
    private final AccountJpaRepository jpaRepository;
    private final AccountMapper mapper;
    
    @Override
    public Optional<Account> findById(AccountId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        AccountEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    // Query optimization - select only needed columns
    @Query("SELECT new AccountSummaryDto(a.id, a.name, a.balance) FROM AccountEntity a WHERE a.userId = :userId")
    List<AccountSummaryDto> findSummariesByUserId(@Param("userId") String userId);
}
```

---

## 🔄 Transaction Patterns

### Transactional Outbox (Atomicity DB + Kafka)

```java
// application/service/TransferService.java
@Service
@RequiredArgsConstructor
public class TransferService {
    
    private final AccountRepository accountRepository;
    private final OutboxRepository outboxRepository;
    
    @Transactional
    public Transfer executeTransfer(TransferCommand command) {
        // 1. Validate accounts
        Account source = accountRepository.findById(command.sourceId())
            .orElseThrow(() -> new AccountNotFoundException(command.sourceId()));
        Account target = accountRepository.findById(command.targetId())
            .orElseThrow(() -> new AccountNotFoundException(command.targetId()));
        
        // 2. Execute domain logic
        source.debit(command.amount());
        target.credit(command.amount());
        
        // 3. Persist changes
        accountRepository.save(source);
        accountRepository.save(target);
        
        // 4. Write to outbox (same transaction!)
        outboxRepository.save(OutboxEvent.builder()
            .aggregateType("Transfer")
            .aggregateId(UUID.randomUUID().toString())
            .eventType("TransferCompleted")
            .payload(objectMapper.writeValueAsString(command))
            .build());
        
        return Transfer.completed(command);
    }
}
```

### N+1 Prevention

```java
// ❌ BAD: N+1 queries
List<Account> accounts = accountRepository.findAll();
for (Account account : accounts) {
    account.setOwner(userRepository.findById(account.getUserId())); // N queries!
}

// ✅ GOOD: Batch fetch with JOIN FETCH
@Query("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.status = :status")
List<Account> findAllWithOwnerByStatus(@Param("status") AccountStatus status);

// ✅ GOOD: Batch fetch with IN clause
List<Account> accounts = accountRepository.findAll();
Set<UserId> userIds = accounts.stream().map(Account::getUserId).collect(toSet());
Map<UserId, User> userMap = userRepository.findByIdIn(userIds).stream()
    .collect(toMap(User::getId, Function.identity()));

accounts.forEach(a -> a.setOwner(userMap.get(a.getUserId())));
```

---

## 🗄️ Caching Strategies

### Multi-Layer Caching (L1 Caffeine + L2 Redis)

```java
// config/CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // L1: Caffeine (in-memory, fast)
        CaffeineCacheManager l1CacheManager = new CaffeineCacheManager();
        l1CacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES));
        
        // L2: Redis (distributed)
        RedisCacheManager l2CacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)))
            .build();
        
        return new CompositeCacheManager(l1CacheManager, l2CacheManager);
    }
}
```

### Cache-Aside Pattern

```java
// application/service/AccountCacheService.java
@Service
@RequiredArgsConstructor
public class AccountCacheService {
    
    private final AccountRepository accountRepository;
    private final RedisTemplate<String, Account> redisTemplate;
    
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    public Account findById(AccountId id) {
        String cacheKey = "account:" + id.value();
        
        // Try cache first
        Account cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Cache miss - fetch from DB
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        
        // Update cache
        redisTemplate.opsForValue().set(cacheKey, account, CACHE_TTL);
        
        return account;
    }
    
    public void invalidateCache(AccountId id) {
        redisTemplate.delete("account:" + id.value());
    }
}
```

---

## ☕ Spring Boot 3.4 Patterns

### Idempotency (Prevent Double-Spending)

```java
// adapter/rest/TransferController.java
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {
    
    private final TransferService transferService;
    private final IdempotencyService idempotencyService;
    
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        
        // Check idempotency
        Optional<TransferResponse> cached = idempotencyService.get(idempotencyKey);
        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }
        
        // Execute transfer
        Transfer transfer = transferService.executeTransfer(request.toCommand());
        TransferResponse response = TransferResponse.from(transfer);
        
        // Store result for idempotency
        idempotencyService.store(idempotencyKey, response, Duration.ofHours(24));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Resilience (Resilience4j)

```java
// adapter/client/FxServiceClient.java
@Service
@RequiredArgsConstructor
public class FxServiceClient {
    
    private final RestTemplate restTemplate;
    
    @CircuitBreaker(name = "fxService", fallbackMethod = "getCachedRate")
    @Bulkhead(name = "fxService", type = Bulkhead.Type.THREADPOOL)
    @Retry(name = "fxService")
    public ExchangeRate getRate(String from, String to) {
        return restTemplate.getForObject(
            "/api/v1/rates?from={from}&to={to}",
            ExchangeRate.class,
            from, to
        );
    }
    
    private ExchangeRate getCachedRate(String from, String to, Exception ex) {
        log.warn("FX service unavailable, using cached rate", ex);
        return cachedRateRepository.findLatest(from, to)
            .orElseThrow(() -> new ServiceUnavailableException("FX rate unavailable"));
    }
}
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      fxService:
        registerHealthIndicator: true
        slidingWindowSize: 100            # Count-based to reduce jitter
        slidingWindowType: COUNT_BASED
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 10
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s       # Fast recovery
        failureRateThreshold: 50           # Fail if >50% errors
        slowCallRateThreshold: 50
        slowCallDurationThreshold: 2000ms
        recordExceptions:
            - java.net.SocketTimeoutException
            - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
            - id.payu.core.exception.BusinessException # Don't trip on logic errors
  
  bulkhead:
    instances:
      fxService:
        maxConcurrentCalls: 20
        maxWaitDuration: 500ms
  
  retry:
    instances:
      fxService:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
```

---

## ⚛️ Quarkus Native (High-Velocity Services)

For lightweight tasks (Gateway, Notifications, Billing), use Quarkus for sub-second startup:

```java
// QuarkusBillingProcessor.java
@ApplicationScoped
public class BillingProcessor {
    
    @Inject
    BillingRepository billingRepository;
    
    @Incoming("billing-process")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    @Transactional
    public CompletionStage<Void> process(BillingEvent event) {
        return billingRepository.process(event)
            .thenAccept(result -> Log.infof("Processed billing: %s", event.getId()));
    }
}
```

---

## 🛡️ Financial Integrity & Security

```java
// domain/value/Money.java
@Value
@RequiredArgsConstructor(staticName = "of")
public class Money {
    BigDecimal amount;
    Currency currency;
    
    // NEVER use double/float for currency
    public Money add(Money other) {
        validateSameCurrency(other);
        return Money.of(
            this.amount.add(other.amount).setScale(2, RoundingMode.HALF_EVEN),
            this.currency
        );
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount)
            .setScale(2, RoundingMode.HALF_EVEN);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }
        return Money.of(result, this.currency);
    }
}
```

---

## 📊 Structured Logging

```java
// adapter/logging/StructuredLogger.java
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLoggingAspect {
    
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = MDC.get("requestId");
        String method = joinPoint.getSignature().getName();
        
        log.info("Request started",
            StructuredArguments.kv("requestId", requestId),
            StructuredArguments.kv("method", method),
            StructuredArguments.kv("args", joinPoint.getArgs()));
        
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("Request completed",
                StructuredArguments.kv("requestId", requestId),
                StructuredArguments.kv("durationMs", System.currentTimeMillis() - start));
            return result;
        } catch (Exception e) {
            log.error("Request failed",
                StructuredArguments.kv("requestId", requestId),
                StructuredArguments.kv("error", e.getMessage()));
            throw e;
        }
    }
}
```

---

## 🔍 Quality & Reliability Checklist

- [ ] **Hexagonal**: Is the domain layer framework-free?
- [ ] **Transactions**: Are related DB operations in `@Transactional`?
- [ ] **Outbox**: Is Kafka publishing atomic with DB writes?
- [ ] **N+1 Prevention**: Are queries optimized with JOIN FETCH or batch fetch?
- [ ] **Caching**: Is frequently accessed data cached with proper TTL?
- [ ] **Idempotency**: Do financial endpoints support `Idempotency-Key`?
- [ ] **Resilience**: Are external calls wrapped with Circuit Breaker?
- [ ] **BigDecimal**: Is all currency math using BigDecimal with HALF_EVEN?
- [ ] **Test Coverage**: 100% logic coverage with JUnit 5 & Mockito?
- [ ] **Integration**: Are external interactions tested with Testcontainers?
- [ ] **Observability**: Is OpenTelemetry tracing active?

---
*Last Updated: January 2026*
