---
name: backend-patterns
description: Backend architecture patterns, API design, database optimization, and server-side best practices for Node.js, Express, and Next.js API routes.
---

# Backend Development Patterns

Backend architecture patterns and best practices for scalable, enterprise-grade server-side applications on the **PayU Digital Banking Platform**.

## 🏗️ Hexagonal Architecture (Ports & Adapters)

PayU core services MUST follow Hexagonal Architecture to decouple business logic from infrastructure.

### 1. Domain Layer (Core)
Contains Entities, Value Objects, and Domain Services. It has no dependencies on external frameworks or databases.

```java
// domain/model/Account.java (Entity)
public class Account {
    private AccountId id;
    private Money balance;
    
    public void credit(Money amount) {
        this.balance = this.balance.add(amount);
    }
}
```

### 2. Ports (Interfaces)
Define the contracts for interaction.
- **Inbound Ports**: Interfaces for use cases (called by Controllers).
- **Outbound Ports**: Interfaces for infrastructure (called by Services to access DB/External APIs).

```java
// domain/port/in/TransferUseCase.java
public interface TransferUseCase {
    void execute(TransferCommand command);
}

// domain/port/out/AccountRepositoryPort.java
public interface AccountRepositoryPort {
    Optional<Account> findById(AccountId id);
    void save(Account account);
}
```

### 3. Adapters (Infrastructure)
Implement the ports to connect with the outside world.
- **Inbound Adapters**: REST Controllers, Kafka Consumers.
- **Outbound Adapters**: JPA Repositories, Redis Clients, External API Clients.

---

## ☕ Java & Spring Boot 3.4 Patterns

### 1. DTO-First Strategy
Define Request/Response schemas before implementation.

```java
// adapter/web/dto/TransferRequest.java
public record TransferRequest(
    @NotNull @UUID String sourceId,
    @NotNull @UUID String targetId,
    @Positive BigDecimal amount,
    @NotBlank String idempotencyKey
) {}
```

### 2. Service Implementation
```java
@Service
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {
    private final AccountRepositoryPort repository;
    private final EventEmitterPort eventEmitter;

    @Transactional
    public void execute(TransferCommand command) {
        var source = repository.findById(command.sourceId()).orElseThrow();
        var target = repository.findById(command.targetId()).orElseThrow();
        
        source.debit(command.amount());
        target.credit(command.amount());
        
        repository.save(source);
        repository.save(target);
        
        eventEmitter.emit(new TransferCompletedEvent(command.id()));
    }
}
```

---

## 🐍 Python & FastAPI Patterns (KYC/Analytics)

### 1. Pydantic Models for Validation
```python
class OCRRequest(BaseModel):
    image_url: HttpUrl
    document_type: str = Field(pattern="^(KTP|NPWP|SIM)$")
```

### 2. Async Repository & Service
```python
class KYCService:
    def __init__(self, repo: KYCRepository):
        self.repo = repo

    async def process_ocr(self, request: OCRRequest):
        # Async I/O for better concurrency
        result = await self.repo.extract_data(request.image_url)
        return result
```

---

## 🔄 Common Backend Patterns

### 1. Idempotency Key Pattern
Essential for payment systems to prevent double spending.
- Store `idempotency_key` in Redis/DB with TTL.
- Check before processing; return cached response if key exists.

### 2. Saga Pattern (Distributed Transactions)
- **Orchestration**: A central service manages the workflow steps and compensations.
- **Choreography**: Services communicate via events to trigger subsequent steps.

### 3. CQRS (Command Query Responsibility Segregation)
Separate read and write operations for high-scale performance.
- **Command**: Writes to a normalized DB (Postgres).
- **Query**: Reads from a denormalized view or specialized DB (NoSQL/Elasticsearch).

### 4. Outbox Pattern
Ensures atomicity between DB updates and event publishing.
1. Save entity AND event to the same DB in one transaction.
2. A separate relay process reads events and publishes them to Kafka.

---

## ⚡ Performance Optimization

### 1. Caching (Multi-layer)
- **L1 (In-Memory)**: Caffeine (Java) or `lru_cache` (Python) for static/frequent data.
- **L2 (Distributed)**: Redis for shared state across microservices.

### 2. Batch Processing
Use `vectorized` operations in data services (ML) and `batch inserts` in high-volume transaction logging.

### 3. Connection Pooling
Always use connection pools (HikariCP for Spring, `asyncpg` pool for Python) to avoid overhead.

---

## 🛡️ Reliability & Resilience

- **Circuit Breaker**: Fail fast when downstream is down (Resilience4j).
- **Bulkhead**: Isolate failure to a specific thread pool or service area.
- **Rate Limiting**: Protect the system from spike loads (Token Bucket/Leaky Bucket).
- **Graceful Degradation**: Provide a static or cached fallback response.

---

## 🔍 Checklist for Backend Design
- [ ] Does it follow Hexagonal Architecture?
- [ ] Are DTOs used for API contracts (no Entitiy leakage)?
- [ ] Is idempotency implemented for all mutations?
- [ ] Are external calls protected by Circuit Breakers?
- [ ] Is PII data masked in logs?
- [ ] Is there an audit trail for financial actions?

---
*Last Updated: January 2026*
