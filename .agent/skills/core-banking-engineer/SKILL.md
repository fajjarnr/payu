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

## 📚 Reference Implementation Patterns
For detailed patterns and historical context on PayU backend engineering, see:
- [Backend & JPA Patterns](./references/BACKEND_PATTERNS.md)

# PayU Core Banking Architect Master Skill

---

## 📦 Build System & Dependency Management

All PayU microservices MUST inherit from the consolidated parent POM to ensure consistency in library versions, security configurations, and compiler settings (including Lombok/Annotation processing).

### 1. Parent POM Standard
NEVER use `spring-boot-starter-parent` directly in a microservice. Always use:
```xml
<parent>
    <groupId>id.payu</groupId>
    <artifactId>payu-backend-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

### 2. Lombok & Code Generation
Always use Lombok annotations for boilerplate. If compilation fails with "cannot find symbol", verify that the service is properly linked to the parent POM and that the `maven-compiler-plugin` hasn't been overridden without the `lombok` annotation processor.

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

## 🚨 P19 Audit Status — Current Platform Reality (Feb 2026)

> **CRITICAL**: Read `.agent/context/P19-AUDIT-STATUS.md` for full details.  
> **Production Readiness: 48/100** — NOT ready for deployment.

### Service Compliance Matrix (What You MUST Know)

| Service | Hex Arch | Shared Starters | Action Required |
|:--------|:---------|:-----------------|:----------------|
| wallet-service | ✅ Full | ✅ All 4 | Integrate `outbox-starter` (R-002) |
| transaction-service | ✅ Full | ✅ All 4 | Integrate `outbox-starter` + `saga-starter` (R-002) |
| account-service | ✅ Full | ✅ All 4 | — |
| auth-service | ✅ Full | ✅ All 4 | — |
| kyc-service | ✅ Full | ✅ All 4 | — |
| lending-service | ⚠️ Partial | ✅ | Write integration tests (R-004), complete hex refactor |
| fx-service | ⚠️ Partial | ✅ | Write integration tests (R-004), complete hex refactor |
| cms-service | 🔴 Flat | 🔴 ZERO starters | Add ALL starters (R-006), refactor to hexagonal |
| ab-testing-service | 🔴 Flat | 🔴 Only api-commons | Add security+resilience+cache (R-006) |
| statement-service | 🔴 Thin | 🔴 ZERO starters | Add ALL starters (R-006), write tests |
| support-service | 🔴 Flat | ✅ | Refactor to hexagonal (R-008) |
| promotion-service | 🔴 Flat | ✅ | Refactor to hexagonal (R-008) |
| backoffice-service | 🔴 Flat | ✅ | Refactor to hexagonal (R-008) |

### P0 Blockers Relevant to This Skill

1. **P0-ARCH-001**: `outbox-starter` and `saga-starter` exist in `backend/shared/` but **ZERO services use them**. When implementing any financial transaction, you MUST integrate `outbox-starter` for atomic event publishing. See `docs/guides/LESSONS.md` § "Transactional Outbox Pattern Integration".

2. **P0-TEST-001**: `lending-service` and `fx-service` have **ZERO integration tests**. Any new feature in these services MUST include Testcontainers integration tests.

### Hexagonal Refactoring Checklist (for flat-package services)

When asked to work on cms, ab-testing, statement, support, promotion, or backoffice services:

1. Create `domain/model/`, `domain/port/in/`, `domain/port/out/` packages
2. Move entities to domain (remove `@Entity` from domain — keep in adapter)
3. Create `application/` package with use case interfaces
4. Create `infrastructure/persistence/` adapter implementing domain ports
5. Create `interfaces/rest/` with DTOs separate from domain models
6. Add `ArchitectureTest.java` using `archunit-starter`
7. Add `security-starter`, `resilience-starter`, `cache-starter` dependencies
8. Run `mvn test` to verify ArchUnit rules pass

---
*Last Updated: February 2026 (P19 Audit)*

## 🧠 Lessons Learned (Session Log)

### L-008: Hexagonal Port Naming — `InputPort` vs `OutputPort`

**Date**: February 26, 2026 | **Severity**: Low | **Domain**: Architecture

Always use `InputPort` suffix for use cases (driver) and `OutputPort` suffix for infrastructure interfaces (driven).
**Rule**: Standardized naming reduces mental overhead during navigation.

### L-009: Shared Starters Exception — Avoid Circular Dependencies

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Development

`security-starter` must NOT depend on any other starter. Keep shared libraries "leaf" modules whenever possible.
**Rule**: Zero-dependency starters are the most resilient.

### L-011: .gitignore `out/` vs Hexagonal `port/out/`

**Date**: March 14, 2026 | **Severity**: Critical | **Domain**: Platform / Architecture

A root `.gitignore` entry `out/` matched all `port/out/` directories in the Hexagonal Architecture. 
**Fix**: Use `/out/` (root-only) instead of `out/` (recursive).

### L-024: Quality Gate Enforcement — ArchUnit

**Date**: March 17, 2026 | **Severity**: Medium | **Domain**: QA / Architecture

Never skip `ArchitectureTest.java`. It’s the only way to prevent "Big Ball of Mud" in a 22-service system.
**Rule**: Every PR that adds a service MUST include ArchUnit rules.

### L-025: Annotation Processor Fallback — Explicit over Magic

**Date**: March 18, 2026 | **Severity**: High | **Domain**: Development

If Lombok/Annotation processing fails in a specific CI runner/IDE, don't waste 4 hours debugging. 
**Fallback**: Implement standard Getters/Setters/Constructors manually to unblock the build.
