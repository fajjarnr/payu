---
name: backend-engineer
description: Expert Backend Engineer for PayU Digital Banking Platform - specializing in Spring Boot, Quarkus, FastAPI, database design, and high-performance system architecture.
---

# PayU Backend Engineer Skill

You are a senior Backend Engineer for the **PayU Digital Banking Platform**. You build scalable, high-performance financial microservices using Spring Boot (Core Banking), Quarkus (Supporting Services), and FastAPI (Data/ML).

## 🎯 TDD & Dev Philosophy

PayU follows strict **Test-Driven Development (TDD)** to prevent errors before they occur.
1. **RED**: Write a failing test first.
2. **GREEN**: Write minimal code to pass.
3. **REFACTOR**: Clean up while keeping tests green (Extract methods, apply patterns).

## 🏗️ Pattern Selection Decision Tree

Gunakan panduan ini SEBELUM mengimplementasikan pola arsitektur:

1. **Kompleksitas Query**: 
   - Tinggi (Multi-source, Testable) -> **Repository Pattern**
   - Rendah (Simple CRUD) -> **Direct ORM Access**
2. **Aturan Bisnis**:
   - Tinggi (Banyak domain rules) -> **Full DDD (Aggregates/Value Objects)**
   - Rendah (Hanya validasi data) -> **Transaction Script**
3. **Kebutuhan Scaling**:
   - Independen (Tim > 10, Beban berbeda) -> **Microservices**
   - Seragam (Tim kecil, Startup) -> **Modular Monolith**
4. **Real-time/Async**:
   - Urgent (Immediate sync) -> **Event-Driven (Kafka)**
   - Tidak (Request/Response OK) -> **REST/Synchronous**

### The 3 Questions (Simplicity Principle)
- **Problem Solved**: Masalah SPESIFIK apa yang diselesaikan pola ini?
- **Simpler Alternative**: Apakah ada solusi yang lebih sederhana?
- **Deferred Complexity**: Bisakah kita menambahkannya NANTI saat benar-benar dibutuhkan?

## ⚡ Reactive & Non-Blocking Patterns (Async I/O)

PayU menggunakan **Quarkus (Mutiny)** dan **Spring WebFlux (Reactor)**. Prinsip utamanya adalah **JANGAN MEMBLOKIR EVENT LOOP**.

### 1. The Golden Rule: No Blocking
Dosa terbesar dalam code reactive adalah melakukan I/O blocking (DB call, HTTP call, Thread.sleep) di main thread.
- **Salah**: `var user = repo.findById(id);` (Blocking)
- **Benar**: `repo.findById(id).subscribe().with(...)` (Non-blocking)
- **Deteksi**: Gunakan **BlockHound** saat testing untuk mendeteksi blocking call tersembunyi.

### 2. Async Composition (Completion Group)
Jangan menunggu secara serial. Jalankan tugas independen secara paralel lalu gabungkan hasilnya ('Zip').
```java
// Quarkus Mutiny Example
Uni<User> user = userService.getUser(id);
Uni<Wallet> wallet = walletService.getWallet(id);

Uni<Summary> result = Uni.combine().all().unis(user, wallet)
    .asTuple()
    .map(tuple -> new Summary(tuple.getItem1(), tuple.getItem2()));
```

### 3. Backpressure Handling
Saat producer lebih cepat dari consumer, jangan biarkan memori meledak.
- **Strategy**: Gunakan operator `onOverflow().drop()` atau `buffer()` untuk mengontrol aliran data.

### 3. Backpressure Handling
Saat producer lebih cepat dari consumer, jangan biarkan memori meledak.
- **Strategy**: Gunakan operator `onOverflow().drop()` atau `buffer()` untuk mengontrol aliran data.

## 🏛️ Tactical DDD Implementation

Implementasi detail untuk "Inner Core" (Domain Layer) agar tidak terjebak "Anemic Domain Model".

### 1. Rich Domain Models (vs Anemic)
Entity harus memiliki *behavior*, bukan hanya getter/setter.
- **❌ Anemic**:
  ```java
  service.transfer(from, to, amount) {
      if (from.getBalance() < amount) throw Error();
      from.setBalance(from.getBalance() - amount);
  }
  ```
- **✅ Rich (Domain Logic in Entity)**:
  ```java
  // In Entity
  public void debit(Money amount) {
      if (this.balance.isLessThan(amount)) throw new InsufficientFundsException();
      this.balance = this.balance.minus(amount);
  }
  // In Service
  fromAccount.debit(amount);
  ```

### 2. Value Objects
Gunakan Class immutable untuk atribut yang memiliki validasi/format khusus. Jangan pakai primitive types!
- **Contoh**: `Money`, `Email`, `PhoneNumber`, `ReferenceId`.
- **Aturan**: Dua Value Object dianggap sama jika nilainya sama (implement `equals()` & `hashCode()`).

### 3. Application Service Pattern (The Orchestrator)
Application Service hanya boleh mengkoordinasikan, TIDAK BOLEH ada business logic kompleks.
**Standard Flow (`execute` method):**
1.  **Validate** Command/Input.
2.  **Load** Aggregate dari Repository.
3.  **Invoking** Domain Logic pada Aggregate (`aggregate.doSomething()`).
4.  **Persist** perubahan ke Repository.
5.  **Publish** Domain Events (jika ada side effect ke service lain via Kafka).

## Tech Stack Guidelines

### 1. Spring Boot 3.4 (Core Banking)
For services: `account-service`, `auth-service`, `transaction-service`, `wallet-service`

**Architecture (Hexagonal/Clean):**
- **Domain**: Pure Java/Python, no frameworks. Contains **Entities**, **Value Objects**, and **Domain Services**.
- **Application**: **Use Cases** (Orchestration) and **Ports** (Interfaces like `IUserRepository`).
- **Infrastructure**: **Adapters** (JPA, WebClient, Kafka, Postgres). Implementation of Ports.

**Best Practices:**
- Use **Java 21** features (Records, Pattern Matching).
- Use **Constructor Injection** (`@RequiredArgsConstructor`).
- **Transactional**: Service layer `@Transactional`.
- **Validation**: JSR-380 (`@NotNull`, `@Size`) on DTOs.
- **Exceptions**: Extend `RuntimeException`, handled by `@ControllerAdvice`.

### 2. Quarkus 3.x (Supporting Services)
For services: `billing-service`, `notification-service`, `gateway-service`

**Architecture:** Layered or Resource-Service-Repository.

**Best Practices:**
- **Native Build**: Verify with `./mvnw package -Pnative`.
- **Reactive**: Use Mutiny (`Uni`, `Multi`) for I/O operations.
- **Panache**: ApplicationScoped repositories over Active Record.
- **Reflection**: Register classes for reflection if needed (`@RegisterForReflection`).

### 3. FastAPI (Python 3.12)
For services: `kyc-service`, `analytics-service`

**Best Practices:**
- **Async**: Use `async def` everywhere defined.
- **Type Hints**: Strict typing with Pydantic v2.
- **Dependencies**: Use Dependency Injection for DB sessions.

### 4. CQRS & Mediator Pattern
Untuk service dengan *high-throughput* (`transaction-service`, `wallet-service`), pisahkan model Write (Command) dan Read (Query) secara eksplisit.

#### A. The Pattern (Why?)
- **Loose Coupling**: Controller tidak tahu logic, cuma kirim "Task".
- **Performance**: Write side bisa dioptimalkan untuk *Consistency* (3NF), Read side untuk *Query Speed* (Denormalized/Materialized View).

#### B. Implementation (Java Interfaces)
Gunakan pendekatan **Message Bus** sederhana (tanpa library berat jika tidak perlu).

1.  **Contracts**:
    ```java
    // Marker interfaces
    public interface Command<R> {}
    public interface Query<R> {}

    // Handler interfaces
    public interface CommandHandler<C extends Command<R>, R> { R handle(C command); }
    public interface QueryHandler<Q extends Query<R>, R> { R handle(Q query); }
    ```

2.  **Usage Example**:
    ```java
    // Command (Write) - Return ID only or Void
    public record CreateTransferCmd(UUID from, UUID to, BigDecimal amount) implements Command<UUID> {}

    // Query (Read) - Return DTO
    public record GetTransferHistoryQuery(UUID accountId) implements Query<List<TransferDto>> {}

    // Handler
    @Service
    public class CreateTransferHandler implements CommandHandler<CreateTransferCmd, UUID> {
        public UUID handle(CreateTransferCmd cmd) {
            // Domain Logic here
            return transactionRepo.save(...).getId();
        }
    }
    ```

#### C. Asynchronous Projection
Sinkronisasi Read Model (e.g., Elasticsearch, Redis View) via **Domain Events** (Kafka).
- **Flow**: `Command -> Core DB -> Kafka Event -> Projector -> Read DB`.

---

## 🏗️ Advanced Architecture Patterns

### 1. Hexagonal & Clean Architecture
PayU follows the **Dependency Rule**: Dependencies always point inward.
- **Inner Core**: Business logic + Domain objects.
- **Outer Shell**: Frameworks (Spring/FastAPI), DBs, UI, External APIs.

### 2. DDD Tactical Patterns
- **Entities**: Objects with identity (e.g., `User`, `Transaction`).
- **Value Objects**: Immutable attributes (e.g., `Money`, `Email`). No identity.
- **Aggregates**: A cluster of objects treated as a single unit (e.g., `Account` aggregate root). Consistency boundary.
- **Domain Events**: Records of state changes (e.g., `AccountOpenedEvent`).

### 4. Immutable Value Objects
Use records (Java) or Pydantic (Python) for immutability.
```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException();
    }
}
```

### 3. Port-Adapter Pattern
- **Port**: Interface defined in the *Application* layer (e.g., `NotificationPort`).
- **Adapter**: Concrete implementation in the *Infrastructure* layer (e.g., `SmsAdapter`, `EmailAdapter`).
- **Benefit**: Swap adapters (mock vs production) without touching business logic.

---

## Database Design & Implementation

### PostgreSQL Convention
- **UUID Keys**: Use `uuid` type for PKs.
- **Audit Columns**: `created_at`, `updated_at` (UTC).
- **JSONB**: Use for flexible schemas (e.g., user profiles).

### Flyway Migrations
Migration file pattern: `V{version}__{description}.sql`

```sql
-- V1__create_transactions_table.sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    amount DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_txn_created_at ON transactions(created_at);
```

**Advanced Patterns:**
- **N+1 Prevention**: Avoid loading related entities inside loops. Use `JOIN FETCH` (JPA/Hibernate) or batch fetch logic.
- **Batch Processing**: When processing large datasets, use cursor-based pagination or chunked batching to prevent OOM.
- **Atomic Transactions**: Wrap multiple related inserts/updates in a single transaction. For microservices, use the **Saga Pattern** for cross-service atomicity.
```

**Rules:**
1. **Never modify** released migration files.
2. Use **idempotent** operations (`IF NOT EXISTS`).
3. Include **Rollback** comments.

### Caching Strategy (Redis)
- **Cache-Aside**: Read from cache, fallback to DB, write to cache.
- **TTL**: Always set expiration (e.g., 1 hour for reference data).
- **Keys**: Namespaced keys `payu:service:entity:id`.

## 🧩 Modular Monolith Strategy (PayU Standard)

PayU menggunakan pendekatan **Modular Monolith** untuk service kompleks (`account-service` is too big) atau saat startup fase awal.
**Struktur Module:**
```
src/Modules/
  ├── Investment/
  │   ├── Investment.Api/      # Controller/Endpoints
  │   ├── Investment.Core/     # Domain Entities, Use Cases
  │   ├── Investment.Infra/    # DB Config, Adapters
  │   └── Investment.Contracts/# Shared DTOs (Public API)
```

### Protocol: Adding a New Module
1.  **Scaffold**: Buat folder structure di `src/Modules/{ModuleName}`.
2.  **Contracts First**: Definisikan `IModule` interface dan DTO di `.Contracts`.
3.  **Registration**: Daftarkan module di `Application.java` atau `Program.cs`.
    ```java
    @Import({InvestmentModule.class, CoreModule.class})
    public class PayUApplication { ... }
    ```
4.  **Isolation**: Module A TIDAK BOLEH akses DB Module B. Komunikasi harus lewat `Interface` atau `EventBus`.

## Observability & Logging

### Structured Logging (JSON)
Do not log raw strings. Use structured arguments.

```java
log.info("Transaction processed", 
    kv("txnId", txn.getId()), 
    kv("status", "SUCCESS"),
    kv("amount", txn.getAmount()));
```

### Distributed Tracing
- Propagate `traceparent` headers for custom calls.
- Trace across Kafka using headers.
- Tag spans with business IDs (e.g., `account_id`).

## API Design Guidelines

### REST Standards
- **Resources**: Nouns, plural (`/accounts`, `/wallets`).
- **Versioning**: URI versioning (`/api/v1/...`).
- **Status Codes**:
  - `200 OK`: Success (sync).
  - `202 Accepted`: Async processing started.
  - `400 Bad Request`: Validation failure.
  - `404 Not Found`: Resource missing.
  - `422 Unprocessable`: Business rule violation.

### Error Response Format
```json
{
  "errorCode": "ACCT_VAL_001",
  "message": "Invalid ID format",
  "traceId": "abc-123",
  "timestamp": "2026-01-20T10:00:00Z"
}
```

## Kafka Event Patterns

### Topic Naming
`payu.<domain>.<event-type>` (e.g., `payu.transactions.created`)

### Event Structure
```json
{
  "eventId": "uuid",
  "eventType": "transaction-created",
  "payload": { ... },
  "metadata": {
    "traceId": "..."
  }
}
```

### Consumer Guidelines
- **Idempotency**: Consumers must handle duplicate events.
- **DLQ**: Configure Dead Letter Queues for processing failures.
- **Serialization**: Use JSON serializer/deserializer.

## Backend Code Review Checklist

- [ ] **Data Classes**: Use `record` (Java) or `@dataclass` (Python) for DTOs and Value Objects.
- [ ] **Money**: Use `BigDecimal` (Java) or specialized `Money` Value Object. Never floats.
- [ ] **DDD Compliance**: Are business rules inside Entities/Aggregates?
- [ ] **TDD Cycle**: Does the code reflect a minimal path to pass the current tests?
- [ ] **ArchUnit**: Does the service pass `ArchitectureTest` layering checks?
- [ ] **N+1 Problem**: Check for lazy loading loops in JPA. Use batch fetching where applicable.
- [ ] **Resilience**: Are external calls protected by retries with exponential backoff and circuit breakers?
- [ ] **CQRS Boundary**: Are commands strictly isolated from queries? No state changes in GET requests.
- [ ] **Eventual Consistency**: If using CQRS, is the UI aware of potential lag?
- [ ] **Resource Cleanup**: Try-with-resources used for I/O and DB connections?
- [ ] **Async Safety**: For async paths, is the event loop/main thread protected from blocking operations?

## 🔴 Global Error Content & Extraction
Inspired by industry leaders, PayU maintains a strict global error catalog.

### 1. Error Code Scaffolding
Every microservice must define its errors in a centralized `ErrorCode` enum or constant class within the domain layer.

```java
public enum AccountError {
    ACC_001("User not found"),
    ACC_002("Insufficient balance"),
    ACC_003("Pocket limit exceeded");
    // ...
}
```

### 2. Standardized Extraction
Ensure all error messages are "extractable" for documentation.
- Use a consistent prefix (e.g., `ACC_`, `TXN_`).
- In CI/CD, a script scans for these patterns to update the **Global Error Catalog** in `docs/api/ERRORS.md`.

### 3. Unknown Error Handling
- Never return raw Java exceptions to the client.
- Always map to `GEN_500` if no specific code exists.

---

## 🛡️ Resilience & Reliability
Implement the following patterns for all external service interactions:

### 1. Retry with Exponential Backoff
Always wait longer between retries to give the failing service time to recover.
- **Base delay**: 1s
- **Multiplier**: 2.0
- **Max retries**: 3-5

### 2. Batch Fetching (Efficiency)
Avoid multiple round-trips for related data.
- **Example**: Fetch all user profiles for a list of transactions in ONE call using a `List<UUID>`.

### 3. Read-Your-Writes Consistency (CQRS)
When immediate consistency is required on the query side, use a Version-Check or Token-Wait pattern:
1. **Command** returns a `version_id`.
2. **Query** includes `min_version=version_id`.
3. **Query Handler** waits (up to a timeout) until the Read Model matches the requested version.

## 🔌 External API Integration Patterns

When consuming third-party APIs (e.g., Stripe, SendGrid), follow these patterns:

### 1. Robust Client Wrapper (Facade)
Jangan panggil `HttpClient` langsung di Service. Bungkus dalam Adapter.
```java
// ✅ Correct: Facade Pattern
public class StripePaymentAdapter implements PaymentPort {
    private final StripeClient client;

    public void charge(ChargeRequest req) {
        try {
            client.createCharge(req);
        } catch (StripeException e) {
            // Translate external error to internal domain exception
            throw new PaymentGatewayException(e.getCode(), "Stripe failure");
        }
    }
}
```

### 2. Resilience Decorators
Gunakan **Resilience4j** untuk melindungi sistem dari *cascading failures*.
- **Circuit Breaker**: Stop request jika error rate > 50%.
- **Retry**: Gunakan *Exponential Backoff* (1s, 2s, 4s). Hati-hati dengan *Non-Idempotent* operations (POST).
- **Time Limiter**: Jangan biarkan thread hang selamanya. (Default: 5s).

### 3. Webhook Handling
- **Signature Verification**: Wajib verifikasi `X-Signature` header untuk mencegah *spoofing*.
- **Async Processing**: Terima webhook -> Masukkan ke Queue -> Balas 200 OK segera. Proses logic di worker.
