---
name: payu-development
description: Skill untuk mengembangkan PayU Digital Banking Platform - mencakup microservices architecture (Spring Boot, Quarkus, FastAPI), event-driven patterns, PostgreSQL databases, dan integrasi dengan Red Hat OpenShift ecosystem.
---

# PayU Digital Banking Development Skill

Skill ini memberikan panduan komprehensif untuk pengembangan PayU Digital Banking Platform, sebuah platform digital banking modern dengan arsitektur microservices dan event-driven.

## When to Use This Skill

Gunakan skill ini ketika:

- Mengembangkan atau memodifikasi microservices di PayU
- Menambahkan fitur baru ke core banking services
- Implementasi event-driven patterns dengan Kafka
- Bekerja dengan database PostgreSQL dan caching
- Integrasi dengan external services (BI-FAST, QRIS, Dukcapil)
- Menerapkan security patterns untuk financial applications

---

## Project Overview

**PayU** adalah standalone digital banking platform dengan arsitektur microservices di atas **Red Hat OpenShift 4.20+**.

### Technology Stack

| Layer               | Technology                | Notes                      |
| ------------------- | ------------------------- | -------------------------- |
| Container Platform  | OpenShift 4.20+           | Kubernetes-compatible      |
| Core Banking        | Spring Boot 3.4 (Java 21) | Axon Framework for CQRS/ES |
| Supporting Services | Quarkus 3.x (Native)      | <50ms startup, <50MB RAM   |
| ML Services         | FastAPI (Python 3.12)     | PyTorch, scikit-learn      |
| Database            | PostgreSQL 16 (Crunchy)   | JSONB for documents        |
| Cache               | Data Grid (RESP mode)     | Redis-compatible API       |
| Event Streaming     | AMQ Streams (Kafka)       | Event sourcing, sagas      |
| Message Queue       | AMQ Broker (AMQP 1.0)     | Point-to-point messaging   |
| Identity            | Red Hat SSO (Keycloak)    | OAuth2/OIDC                |

---

## Microservices Architecture

### Service Decomposition

```
payu/backend/
├── account-service/      # Java Spring Boot - User accounts, eKYC, multi-pocket
├── auth-service/         # Java Spring Boot - Authentication, MFA, OAuth2
├── transaction-service/  # Java Spring Boot - Transfers, BI-FAST, QRIS
├── wallet-service/       # Java Spring Boot - Balance management, ledger
├── billing-service/      # Java Quarkus - Bill payments, top-ups
├── notification-service/ # Java Quarkus - Push, SMS, Email
├── gateway-service/      # Java Quarkus - API Gateway
├── kyc-service/          # Python FastAPI - OCR, liveness detection ML
└── analytics-service/    # Python FastAPI - User insights, ML
```

### Service Specifications

| Service              | Port | Technology      | Package                |
| -------------------- | ---- | --------------- | ---------------------- |
| account-service      | 8001 | Spring Boot 3.4 | `id.payu.account`      |
| auth-service         | 8002 | Spring Boot 3.4 | `id.payu.auth`         |
| transaction-service  | 8003 | Spring Boot 3.4 | `id.payu.transaction`  |
| wallet-service       | 8004 | Spring Boot 3.4 | `id.payu.wallet`       |
| billing-service      | 8005 | Quarkus 3.x     | `id.payu.billing`      |
| notification-service | 8006 | Quarkus 3.x     | `id.payu.notification` |
| gateway-service      | 8080 | Quarkus 3.x     | `id.payu.gateway`      |
| kyc-service          | 9001 | FastAPI         | `app`                  |
| analytics-service    | 9002 | FastAPI         | `app`                  |

---

## Development Guidelines

### Spring Boot Services (Core Banking)

**Structure:**

```
src/main/java/id/payu/<service>/
├── <ServiceName>Application.java
├── config/                  # Configuration classes
├── domain/
│   ├── entity/              # JPA entities
│   ├── event/               # Domain events
│   ├── repository/          # Repository interfaces
│   └── service/             # Domain services
├── application/
│   ├── command/             # CQRS commands
│   ├── query/               # CQRS queries
│   └── saga/                # Saga participants
├── infrastructure/
│   ├── persistence/         # JPA implementations
│   ├── messaging/           # Kafka producers/consumers
│   └── external/            # External service clients
└── api/
    ├── rest/                # REST controllers
    └── grpc/                # gRPC services (internal)
```

**Commands:**

```bash
cd backend/<service-name>
./mvnw spring-boot:run    # Development
./mvnw clean package      # Build
./mvnw test               # Run tests
```

**Dependencies:**

- Spring Data JPA + PostgreSQL
- Spring Data Redis (Data Grid RESP mode)
- Spring Kafka (AMQ Streams)
- Axon Framework (CQRS/Event Sourcing)

### Quarkus Services (Supporting)

**Structure:**

```
src/main/java/id/payu/<service>/
├── config/
├── domain/
├── application/
├── infrastructure/
└── resource/                # JAX-RS resources
```

**Commands:**

```bash
cd backend/<service-name>
./mvnw quarkus:dev                              # Development (hot reload)
./mvnw package -Pnative                         # Native build
./mvnw package -Dquarkus.container-image.build=true  # Container build
```

**Dependencies:**

- Quarkus Hibernate ORM + PostgreSQL
- Quarkus Redis Client (Data Grid RESP mode)
- SmallRye Reactive Messaging (Kafka)

### Python FastAPI Services (ML/Data)

**Structure:**

```
app/
├── main.py              # FastAPI application
├── config/
├── models/              # Pydantic models
├── services/            # Business logic
├── ml/                  # ML models (PyTorch)
├── repositories/        # Database access
└── routers/             # API routes
```

**Commands:**

```bash
cd backend/<service-name>
pip install -r requirements.txt
uvicorn app.main:app --reload  # Development
pytest                         # Run tests
```

---

## TDD & Testing

### Testing Stack

| Tool                 | Version | Purpose                               |
| -------------------- | ------- | ------------------------------------- |
| JUnit 5              | Latest  | Test framework                        |
| Mockito              | Latest  | Mocking library                       |
| Testcontainers       | Latest  | Integration tests (PostgreSQL, Kafka) |
| ArchUnit             | 1.2.1   | Architecture rule enforcement         |
| JaCoCo               | 0.8.11  | Code coverage                         |
| Spring Security Test | Latest  | Security context testing              |

### Test Structure

```
src/test/java/id/payu/<service>/
├── service/           # Unit tests (Mockito)
├── controller/        # WebMvcTest
├── architecture/      # ArchUnit rules
└── integration/       # Testcontainers
```

### Test Commands

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
# Report at: target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=OnboardingServiceTest

# Run architecture tests only
mvn test -Dtest=ArchitectureTest
```

### TDD Workflow

1. **Red** - Write failing test first
2. **Green** - Write minimal code to pass
3. **Refactor** - Clean up while keeping tests green

### Example Test Patterns

**Unit Test (Service):**

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;

    @Test
    void shouldDoSomething() {
        given(repository.findById(any())).willReturn(Optional.of(entity));
        var result = service.process(id);
        assertThat(result).isNotNull();
    }
}
```

**Controller Test:**

```java
@WebMvcTest(MyController.class)
class MyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MyService service;

    @Test
    @WithMockUser
    void shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/resource"))
            .andExpect(status().isOk());
    }
}
```

**Architecture Test:**

```java
@Test
void shouldFollowLayeredArchitecture() {
    layeredArchitecture()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .check(importedClasses);
}
```

### Coverage Requirements

> [!IMPORTANT]
> These thresholds are enforced by JaCoCo during `mvn verify`.

| Coverage Type                 | Threshold | Description                       |
| ----------------------------- | --------- | --------------------------------- |
| **Line Coverage (Overall)**   | 80%       | Minimum lines covered             |
| **Branch Coverage (Overall)** | 70%       | Minimum decision branches covered |
| **Per-Class Line Coverage**   | 60%       | No single class below this        |
| **Critical Path (Payments)**  | 95%       | Payment/transaction flows         |

**Excluded from Coverage:**

- Configuration classes (`*Config`, `*Configuration`)
- Application main class (`*Application`)
- DTOs and Request/Response objects

### Event-Driven Testing Patterns

**Saga Compensation Tests:**

```java
@Test
void shouldCompensateWhenTransferFails() {
    // Given: Transfer initiated
    var transferId = initiateTransfer(100_000L);

    // When: Credit fails
    simulateCreditFailure(transferId);

    // Then: Balance should be released
    verify(walletService).releaseReservedBalance(transferId);
    assertThat(getTransferStatus(transferId)).isEqualTo(FAILED);
}
```

**Event Ordering & Idempotency Tests:**

```java
@Test
void shouldHandleDuplicateEvents() {
    var event = new TransferInitiatedEvent(txnId, amount);

    // Process same event twice
    eventHandler.handle(event);
    eventHandler.handle(event); // Duplicate

    // Should only process once
    verify(walletService, times(1)).reserveBalance(any());
}
```

**Dead Letter Queue Tests:**

```java
@Test
void shouldSendToDeadLetterQueueAfterMaxRetries() {
    var poisonEvent = createMalformedEvent();

    // Simulate 3 failed attempts
    for (int i = 0; i < 3; i++) {
        assertThrows(ProcessingException.class,
            () -> eventHandler.handle(poisonEvent));
    }

    // Should be in DLQ
    assertThat(dlqRepository.findByEventId(poisonEvent.getId())).isPresent();
}
```

### Performance Testing Guidelines

**Tools:**

| Tool | Use Case |
|------|----------|
| Gatling | Load testing, stress testing |
| JMeter | API performance testing |
| k6 | Cloud-native load testing |

**Performance Thresholds:**

| Metric | Target | Critical |
|--------|--------|----------|
| P95 Latency | < 200ms | < 500ms |
| P99 Latency | < 500ms | < 1000ms |
| Error Rate | < 0.1% | < 1% |
| Throughput | > 500 req/s | > 100 req/s |

**Database Performance:**

```sql
-- Always run EXPLAIN ANALYZE for new queries
EXPLAIN ANALYZE
SELECT * FROM transactions
WHERE account_id = ? AND created_at > ?;

-- Target: < 10ms for indexed queries
-- Alert if: > 100ms
```

---

## Event-Driven Architecture

### Kafka Topic Naming Convention

```
payu.<domain>.<event-type>
```

**Topics:**

```
payu.
├── accounts.
│   ├── account-created
│   ├── account-updated
│   ├── pocket-created
│   └── pocket-balance-changed
├── transactions.
│   ├── transaction-initiated
│   ├── transaction-validated
│   ├── transaction-completed
│   └── transaction-failed
├── wallet.
│   ├── balance-reserved
│   ├── balance-committed
│   └── balance-released
├── notifications.
│   ├── notification-requested
│   └── notification-delivered
└── dlq.
    ├── transactions-dlq
    └── notifications-dlq
```

### Saga Pattern Example

Transfer Saga Flow:

1. `TransferInitiatedEvent` → Reserve balance
2. `BalanceReservedEvent` → Validate recipient
3. `RecipientValidatedEvent` → Commit transfer
4. `TransferCompletedEvent` → Send notification

Compensation:

- `BalanceReservationFailedEvent` → Fail transaction
- `CreditFailedEvent` → Release reserved balance

---

## Database Guidelines

### PostgreSQL Schemas

Each service has its own database:

- `payu_accounts` - Account service
- `payu_transactions` - Transaction service (+ Event Store)
- `payu_wallet` - Wallet service (Double-entry ledger)
- `payu_kyc` - KYC service (JSONB)
- `payu_notification` - Notification service
- `payu_analytics` - Analytics service (TimescaleDB)

### Entity Conventions

```java
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### Double-Entry Ledger (Wallet Service)

```sql
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL,  -- DEBIT, CREDIT
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    balance_after DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT positive_amount CHECK (amount > 0)
);

-- Rule: SUM(CREDIT) = SUM(DEBIT) per transaction
```

### Database Migration Strategy

PayU menggunakan **Flyway** untuk database migrations.

**Naming Convention:**

```
V{version}__{description}.sql
```

| Pattern  | Example                         | Description                   |
| -------- | ------------------------------- | ----------------------------- |
| `V1__`   | `V1__create_accounts_table.sql` | Initial schema                |
| `V2__`   | `V2__add_phone_index.sql`       | Schema changes                |
| `V1.1__` | `V1.1__add_status_column.sql`   | Minor updates                 |
| `R__`    | `R__refresh_views.sql`          | Repeatable (views, functions) |

**Migration Structure:**

```
src/main/resources/db/migration/
├── V1__create_accounts_table.sql
├── V2__create_pockets_table.sql
├── V3__add_kyc_status_column.sql
└── R__create_account_summary_view.sql
```

**Migration Best Practices:**

- ✅ Always add `IF NOT EXISTS` for safety
- ✅ Include rollback comments in migration files
- ✅ Test migrations on empty and populated databases
- ✅ Never modify released migrations
- ❌ Never use `DROP` without backup plan

**Example Migration:**

```sql
-- V3__add_kyc_status_column.sql
-- Description: Add KYC status column for compliance tracking
-- Rollback: ALTER TABLE accounts DROP COLUMN kyc_status;

ALTER TABLE accounts
ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(20) DEFAULT 'PENDING';

CREATE INDEX IF NOT EXISTS idx_accounts_kyc_status
ON accounts (kyc_status);
```

### Backup & Recovery Strategy

**Backup Configuration:**

| Type          | Frequency     | Retention | RTO     | RPO      |
| ------------- | ------------- | --------- | ------- | -------- |
| Full Backup   | Daily 02:00   | 30 days   | 4 hours | 24 hours |
| Incremental   | Every 6 hours | 7 days    | 1 hour  | 6 hours  |
| WAL Archive   | Continuous    | 7 days    | 15 min  | 5 min    |
| Point-in-Time | On-demand     | 7 days    | 30 min  | Custom   |

**Disaster Recovery:**

- **RTO (Recovery Time Objective)**: 4 hours max
- **RPO (Recovery Point Objective)**: 5 minutes max (WAL shipping)
- **Backup Location**: Cross-region S3/Object Storage

### Indexing Guidelines

**When to Create Index:**

- Columns in `WHERE` clauses used frequently
- Columns in `JOIN` conditions
- Columns in `ORDER BY` with large result sets
- Foreign key columns

**Index Naming Convention:**

```
idx_{table}_{column(s)}
```

**Common Indexes:**

```sql
-- Single column index
CREATE INDEX idx_accounts_phone ON accounts (phone_number);

-- Composite index (order matters!)
CREATE INDEX idx_transactions_account_date
ON transactions (account_id, created_at DESC);

-- Partial index (for specific conditions)
CREATE INDEX idx_accounts_active
ON accounts (status) WHERE status = 'ACTIVE';

-- GIN index for JSONB
CREATE INDEX idx_profiles_data_gin
ON profiles USING GIN (profile_data);
```

**Query Performance Guidelines:**

```sql
-- Always run EXPLAIN ANALYZE for new queries
EXPLAIN ANALYZE
SELECT * FROM transactions
WHERE account_id = 'uuid' AND created_at > '2026-01-01';

-- Target execution times:
-- • Simple queries: < 10ms
-- • Complex joins: < 100ms
-- • Reports/analytics: < 1000ms
```

**Anti-Patterns to Avoid:**

- ❌ `SELECT *` - Always specify columns
- ❌ `LIKE '%value'` - Leading wildcard prevents index use
- ❌ Functions on indexed columns in WHERE
- ❌ Missing indexes on foreign keys
- ❌ Too many indexes (slows writes)

---

## Observability & Monitoring 📊

### Logging Standards

**Format:** JSON structured logging

```java
// Logback configuration (logback-spring.xml)
@Slf4j
@Service
public class TransferService {
    public TransferResult transfer(TransferRequest request) {
        log.info("Transfer initiated",
            kv("transferId", request.getId()),
            kv("amount", request.getAmount()),
            kv("sourceAccount", maskAccount(request.getSource())));
        // ...
    }
}
```

**Log Structure (JSON):**

```json
{
  "timestamp": "2026-01-20T00:45:00.000Z",
  "level": "INFO",
  "logger": "id.payu.transaction.TransferService",
  "message": "Transfer initiated",
  "context": {
    "correlationId": "uuid-correlation",
    "traceId": "abc123",
    "spanId": "def456",
    "service": "transaction-service",
    "version": "1.0.0"
  },
  "data": {
    "transferId": "txn-uuid",
    "amount": 100000,
    "sourceAccount": "****7890"
  }
}
```

**Log Levels:**

| Level | Environment | Use Case                                 |
| ----- | ----------- | ---------------------------------------- |
| ERROR | All         | Exceptions, failures requiring attention |
| WARN  | All         | Degraded performance, recoverable issues |
| INFO  | All         | Business events, milestones              |
| DEBUG | Dev/SIT     | Detailed flow tracing                    |
| TRACE | Dev only    | Very detailed debugging                  |

**What to Log:**

- ✅ Request received/completed with correlation ID
- ✅ Business events (transfer initiated, completed, failed)
- ✅ External service calls (duration, status)
- ✅ Authentication events (login, logout, failures)
- ❌ PII (mask NIK, phone, email)
- ❌ Secrets (passwords, tokens, API keys)
- ❌ Full request/response bodies in production

### Distributed Tracing

**OpenTelemetry Configuration:**

```yaml
# application.yml
management:
  tracing:
    sampling:
      probability: 1.0 # 100% in dev, 10% in prod
  otlp:
    tracing:
      endpoint: http://jaeger-collector:4318/v1/traces
```

**Trace Context Propagation:**

```java
@RestController
public class TransferController {

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("X-Correlation-ID") String correlationId,
            @RequestBody TransferRequest request) {

        // Correlation ID is automatically propagated via Micrometer
        MDC.put("correlationId", correlationId);

        return ResponseEntity.ok(transferService.process(request));
    }
}
```

**Span Naming Convention:**

```
{service}.{operation}
```

Examples:

- `transaction-service.transfer.initiate`
- `wallet-service.balance.reserve`
- `bifast-client.transfer.execute`

### SLI/SLO Definitions

**Service Level Indicators (SLIs):**

| SLI          | Metric                               | Target    |
| ------------ | ------------------------------------ | --------- |
| Availability | Successful requests / Total requests | 99.9%     |
| Latency      | P95 response time                    | < 200ms   |
| Error Rate   | 5xx errors / Total requests          | < 0.1%    |
| Throughput   | Requests per second                  | > 500 rps |

**Service Level Objectives (SLOs):**

| Service     | Availability | P95 Latency | Error Rate |
| ----------- | ------------ | ----------- | ---------- |
| Gateway     | 99.95%       | 100ms       | 0.05%      |
| Account     | 99.9%        | 200ms       | 0.1%       |
| Transaction | 99.9%        | 300ms       | 0.1%       |
| Wallet      | 99.9%        | 150ms       | 0.1%       |
| Billing     | 99.5%        | 500ms       | 0.5%       |

**Error Budget:**

```
Error Budget = 100% - SLO
Monthly Error Budget = Error Budget × Minutes in Month

Example (99.9% SLO):
- Error Budget: 0.1%
- Monthly: 0.001 × 43,200 = 43.2 minutes downtime allowed
```

### Metrics (Micrometer/Prometheus)

**Business Metrics:**

```java
@Component
public class TransactionMetrics {
    private final Counter transferCounter;
    private final Timer transferTimer;

    public TransactionMetrics(MeterRegistry registry) {
        this.transferCounter = Counter.builder("payu.transfers.total")
            .tag("type", "bifast")
            .tag("status", "success")
            .description("Total transfers processed")
            .register(registry);

        this.transferTimer = Timer.builder("payu.transfers.duration")
            .description("Transfer processing time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
}
```

**Standard Metrics:**

| Metric                  | Type    | Labels              | Description             |
| ----------------------- | ------- | ------------------- | ----------------------- |
| `http_server_requests`  | Timer   | method, uri, status | Request latency         |
| `jvm_memory_used`       | Gauge   | area, id            | JVM memory usage        |
| `db_pool_active`        | Gauge   | pool                | Active DB connections   |
| `payu.transfers.total`  | Counter | type, status        | Business: transfers     |
| `payu.accounts.created` | Counter | channel             | Business: registrations |

### Alerting Rules

**Critical Alerts (PagerDuty):**

| Alert               | Condition                   | Severity |
| ------------------- | --------------------------- | -------- |
| Service Down        | availability < 99% for 5min | P1       |
| High Error Rate     | error_rate > 5% for 5min    | P1       |
| Latency Spike       | P95 > 2s for 10min          | P2       |
| Database Connection | pool_active > 80% for 5min  | P2       |

**Warning Alerts (Slack):**

| Alert         | Condition                   | Severity |
| ------------- | --------------------------- | -------- |
| SLO Burn Rate | consuming > 2x error budget | P3       |
| Memory High   | jvm_memory > 80% for 15min  | P3       |
| Queue Backlog | kafka_lag > 1000 for 10min  | P3       |

**Prometheus Alert Rule Example:**

```yaml
groups:
  - name: payu-slo
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_total{status=~"5.."}[5m])) 
          / sum(rate(http_server_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: 'High error rate detected'
          description: 'Error rate is {{ $value | humanizePercentage }}'
```

---

## Security Requirements

### Authentication Flow

1. User login with phone + PIN
2. Challenge (OTP/Biometric)
3. Access Token + Refresh Token (JWT)
4. Token validation via Keycloak

### Security Layers

| Layer       | Implementation                                 |
| ----------- | ---------------------------------------------- |
| Perimeter   | WAF, DDoS protection                           |
| Network     | VPC, mTLS (Istio)                              |
| Application | OAuth2/OIDC, JWT (15min expiry)                |
| Data        | AES-256 encryption, Field-level PII encryption |

### Transaction Security

- 6-digit Transaction PIN (3 attempts before lock)
- Device binding (max 2 devices)
- Fraud detection ML model
- Idempotency keys (UUID-based)

---

## API Design Standards

### REST Endpoint Conventions

```
/v1/{resource}           # Collection
/v1/{resource}/{id}      # Single resource
/v1/{resource}/{id}/{sub-resource}
```

### Response Format

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "uuid",
    "timestamp": "ISO-8601"
  }
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Saldo tidak mencukupi",
    "details": {}
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "ISO-8601"
  }
}
```

---

## Error Handling Taxonomy 🚨

> [!IMPORTANT]
> All services MUST use consistent error codes from this taxonomy.

### Error Code Structure

```
[DOMAIN]_[CATEGORY]_[SPECIFIC]
```

Example: `TXN_VALIDATION_INSUFFICIENT_BALANCE`

### Error Code Domains

| Domain Prefix | Range     | Description                         |
| ------------- | --------- | ----------------------------------- |
| `AUTH_xxx`    | 4000-4999 | Authentication/Authorization errors |
| `ACCT_xxx`    | 5000-5999 | Account management errors           |
| `TXN_xxx`     | 6000-6999 | Transaction errors                  |
| `INTG_xxx`    | 7000-7999 | External integration errors         |
| `SYS_xxx`     | 9000-9999 | System/Infrastructure errors        |

### Authentication Errors (AUTH)

| Code                       | HTTP | Description                           |
| -------------------------- | ---- | ------------------------------------- |
| `AUTH_INVALID_CREDENTIALS` | 401  | Wrong username/password               |
| `AUTH_TOKEN_EXPIRED`       | 401  | JWT token has expired                 |
| `AUTH_TOKEN_INVALID`       | 401  | JWT token is malformed                |
| `AUTH_MFA_REQUIRED`        | 403  | Multi-factor authentication needed    |
| `AUTH_ACCOUNT_LOCKED`      | 403  | Account locked due to failed attempts |
| `AUTH_INSUFFICIENT_SCOPE`  | 403  | Token lacks required permissions      |

### Account Errors (ACCT)

| Code                  | HTTP | Description                   |
| --------------------- | ---- | ----------------------------- |
| `ACCT_NOT_FOUND`      | 404  | Account does not exist        |
| `ACCT_ALREADY_EXISTS` | 409  | Duplicate account creation    |
| `ACCT_INACTIVE`       | 403  | Account is suspended/inactive |
| `ACCT_KYC_PENDING`    | 403  | KYC verification required     |
| `ACCT_LIMIT_EXCEEDED` | 400  | Account limit reached         |

### Transaction Errors (TXN)

| Code                       | HTTP | Description                         |
| -------------------------- | ---- | ----------------------------------- |
| `TXN_INSUFFICIENT_BALANCE` | 400  | Not enough funds                    |
| `TXN_LIMIT_EXCEEDED`       | 400  | Daily/monthly limit exceeded        |
| `TXN_DUPLICATE`            | 409  | Duplicate transaction (idempotency) |
| `TXN_INVALID_AMOUNT`       | 400  | Amount is zero/negative             |
| `TXN_RECIPIENT_NOT_FOUND`  | 404  | Destination account not found       |
| `TXN_PROCESSING_FAILED`    | 500  | Internal processing error           |
| `TXN_TIMEOUT`              | 504  | Transaction timed out               |

### Integration Errors (INTG)

| Code                      | HTTP | Description                        |
| ------------------------- | ---- | ---------------------------------- |
| `INTG_BIFAST_UNAVAILABLE` | 503  | BI-FAST service down               |
| `INTG_QRIS_TIMEOUT`       | 504  | QRIS gateway timeout               |
| `INTG_DUKCAPIL_ERROR`     | 502  | Dukcapil verification failed       |
| `INTG_BANK_REJECTED`      | 400  | External bank rejected transaction |
| `INTG_CIRCUIT_OPEN`       | 503  | Circuit breaker is open            |

### System Errors (SYS)

| Code                 | HTTP | Description                     |
| -------------------- | ---- | ------------------------------- |
| `SYS_DATABASE_ERROR` | 500  | Database connection/query error |
| `SYS_CACHE_ERROR`    | 500  | Redis/cache failure             |
| `SYS_KAFKA_ERROR`    | 500  | Message broker failure          |
| `SYS_INTERNAL_ERROR` | 500  | Unexpected internal error       |

---

## Resilience Patterns 🛡️

### Retry Pattern

```java
@Retry(name = "bifast", fallbackMethod = "bifastFallback")
public TransferResult sendToBifast(TransferRequest request) {
    return bifastClient.transfer(request);
}
```

**Configuration (application.yml):**

```yaml
resilience4j:
  retry:
    instances:
      bifast:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - id.payu.transaction.exception.BusinessException
```

### Circuit Breaker Pattern

```java
@CircuitBreaker(name = "externalBank", fallbackMethod = "bankFallback")
public BankResponse callExternalBank(BankRequest request) {
    return bankClient.process(request);
}
```

**Configuration:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      externalBank:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallRateThreshold: 80
        slowCallDurationThreshold: 2s
```

### Bulkhead Pattern

```java
@Bulkhead(name = "qris", type = Bulkhead.Type.THREADPOOL)
public QRISResponse processQRIS(QRISRequest request) {
    return qrisGateway.process(request);
}
```

**Configuration:**

```yaml
resilience4j:
  bulkhead:
    instances:
      qris:
        maxConcurrentCalls: 25
        maxWaitDuration: 100ms
  thread-pool-bulkhead:
    instances:
      qris:
        maxThreadPoolSize: 10
        coreThreadPoolSize: 5
        queueCapacity: 50
```

### Combined Resilience Example

```java
@CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
@Retry(name = "payment")
@Bulkhead(name = "payment")
@TimeLimiter(name = "payment")
public PaymentResult processPayment(PaymentRequest request) {
    return paymentGateway.process(request);
}

private PaymentResult paymentFallback(PaymentRequest request, Exception ex) {
    log.error("Payment failed, triggering fallback", ex);
    return PaymentResult.pending(request.getId(), "Processing delayed");
}
```

---

## Code Style and Conventions

### Naming Conventions

| Type                | Convention  | Example                |
| ------------------- | ----------- | ---------------------- |
| Variables/Functions | camelCase   | `accountBalance`       |
| Classes             | PascalCase  | `AccountService`       |
| Files               | kebab-case  | `account-service.java` |
| Packages            | lowercase   | `id.payu.account`      |
| Constants           | UPPER_SNAKE | `MAX_RETRY_COUNT`      |

### Git Commit Format

Use conventional commits:

```
feat: add multi-pocket support for accounts
fix: resolve balance calculation error
docs: update API documentation
refactor: extract validation logic
test: add unit tests for transfer saga
```

### Changelog Policy

Update `CHANGELOG.md` under `[Unreleased]` section for significant changes.

---

## Context7 Integration

Query up-to-date documentation using Context7 MCP:

| Library     | Context7 ID                    |
| ----------- | ------------------------------ |
| Spring Boot | `/spring-projects/spring-boot` |
| Quarkus     | `/quarkusio/quarkus`           |
| FastAPI     | `/tiangolo/fastapi`            |
| Kafka       | `/apache/kafka`                |
| PostgreSQL  | `/postgres/postgres`           |

---

## External Service Simulators 🧪

PayU menyediakan simulators untuk external service integrations yang memungkinkan development dan testing tanpa koneksi ke production systems.

### Available Simulators

| Simulator            | Port | Technology     | Purpose                         |
| -------------------- | ---- | -------------- | ------------------------------- |
| `bifast-simulator`   | 8091 | Quarkus Native | BI-FAST interbank transfers     |
| `dukcapil-simulator` | 8092 | Quarkus Native | NIK verification, face matching |
| `qris-simulator`     | 8093 | Quarkus Native | QR code payments                |

### Simulator Structure

```
backend/simulators/
├── bifast-simulator/
│   ├── src/main/java/id/payu/simulator/bifast/
│   │   ├── resource/         # REST endpoints
│   │   ├── model/            # Request/Response DTOs
│   │   ├── service/          # Business logic
│   │   └── config/           # Simulation config
│   └── src/main/resources/
│       ├── application.yml   # Config (latency, failure rate)
│       └── test-data/        # Mock data (accounts, responses)
├── dukcapil-simulator/
└── qris-simulator/
```

### Running Simulators

```bash
# Run individual simulator
cd backend/simulators/bifast-simulator
./mvnw quarkus:dev

# Run all via Docker Compose
docker-compose up -d bifast-simulator dukcapil-simulator qris-simulator
```

### Simulator Configuration

Setiap simulator dapat dikonfigurasi via environment variables:

```yaml
# application.yml
simulator:
  latency:
    min-ms: 50 # Minimum response latency
    max-ms: 500 # Maximum response latency
  failure:
    rate: 0.05 # 5% random failure rate
    timeout-rate: 0.02 # 2% timeout rate
  scenarios:
    enabled: true # Enable special test scenarios
```

### BI-FAST Simulator

**Endpoints:**

| Method | Path                     | Description     |
| ------ | ------------------------ | --------------- |
| POST   | `/api/v1/inquiry`        | Account inquiry |
| POST   | `/api/v1/transfer`       | Fund transfer   |
| GET    | `/api/v1/status/{refId}` | Transfer status |

**Test Accounts:**

| Account Number | Bank | Behavior           |
| -------------- | ---- | ------------------ |
| `1234567890`   | BCA  | Success            |
| `9876543210`   | BRI  | Success            |
| `1111111111`   | -    | ACCOUNT_NOT_FOUND  |
| `2222222222`   | -    | BLOCKED_ACCOUNT    |
| `3333333333`   | -    | Timeout (5s delay) |

**Example Request:**

```json
POST /api/v1/transfer
{
  "sourceAccount": "1234567890",
  "destinationAccount": "9876543210",
  "destinationBank": "BRI",
  "amount": 100000,
  "currency": "IDR",
  "reference": "TXN-123456"
}
```

### Dukcapil Simulator

**Endpoints:**

| Method | Path                  | Description      |
| ------ | --------------------- | ---------------- |
| POST   | `/api/v1/verify`      | NIK verification |
| POST   | `/api/v1/match-photo` | Face matching    |
| GET    | `/api/v1/nik/{nik}`   | Get citizen data |

**Test NIKs:**

| NIK                | Status    | Face Match Score |
| ------------------ | --------- | ---------------- |
| `3171234567890001` | VALID     | 95%              |
| `3171234567890002` | VALID     | 75% (threshold)  |
| `3171234567890003` | BLOCKED   | -                |
| `3171234567890004` | DECEASED  | -                |
| `0000000000000000` | NOT_FOUND | -                |

### QRIS Simulator

**Endpoints:**

| Method | Path                    | Description      |
| ------ | ----------------------- | ---------------- |
| POST   | `/api/v1/generate`      | Generate QR code |
| POST   | `/api/v1/pay`           | Process payment  |
| GET    | `/api/v1/status/{qrId}` | Payment status   |

**Test Merchants:**

| Merchant ID   | Name            | Category        |
| ------------- | --------------- | --------------- |
| `MERCHANT001` | Warung Kopi     | Food & Beverage |
| `MERCHANT002` | Toko Elektronik | Electronics     |
| `MERCHANT003` | Apotek Sehat    | Health          |

### Testing with Simulators

#### Unit Test with Mock

```java
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    @Mock
    private BifastClient bifastClient;

    @Test
    void shouldTransferSuccessfully() {
        // Given
        given(bifastClient.transfer(any()))
            .willReturn(new BifastResponse("SUCCESS", "REF-123"));

        // When
        var result = transferService.transfer(request);

        // Then
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }
}
```

#### Integration Test with Testcontainers

```java
@Testcontainers
@SpringBootTest
class BifastIntegrationTest {

    @Container
    static GenericContainer<?> bifastSimulator = new GenericContainer<>(
        DockerImageName.parse("payu/bifast-simulator:latest"))
        .withExposedPorts(8091);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("bifast.base-url",
            () -> "http://localhost:" + bifastSimulator.getMappedPort(8091));
    }

    @Test
    void shouldCallBifastSimulator() {
        // Integration test with real simulator
    }
}
```

#### Contract Testing with PACT

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "bifast-service")
class BifastContractTest {

    @Pact(consumer = "transaction-service")
    public RequestResponsePact transferPact(PactDslWithProvider builder) {
        return builder
            .given("account exists")
            .uponReceiving("a transfer request")
            .path("/api/v1/transfer")
            .method("POST")
            .body(new PactDslJsonBody()
                .stringType("sourceAccount", "1234567890")
                .numberType("amount", 100000))
            .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody()
                .stringValue("status", "SUCCESS"))
            .toPact();
    }
}
```

### Failure Scenario Testing

```java
@Test
void shouldHandleBifastTimeout() {
    // Use test account that triggers timeout
    var request = TransferRequest.builder()
        .destinationAccount("3333333333")  // Timeout account
        .amount(100000L)
        .build();

    assertThrows(TimeoutException.class,
        () -> bifastClient.transfer(request));
}

@Test
void shouldHandleAccountNotFound() {
    var request = TransferRequest.builder()
        .destinationAccount("1111111111")  // Not found account
        .amount(100000L)
        .build();

    var response = bifastClient.transfer(request);
    assertThat(response.getErrorCode()).isEqualTo("ACCOUNT_NOT_FOUND");
}
```

### Adding New Simulator

1. Create new Quarkus project:

   ```bash
   cd backend/simulators
   mvn io.quarkus:quarkus-maven-plugin:create \
     -DprojectGroupId=id.payu.simulator \
     -DprojectArtifactId=newservice-simulator
   ```

2. Implement endpoints following patterns above

3. Add to docker-compose.yml:

   ```yaml
   newservice-simulator:
     build: ./backend/simulators/newservice-simulator
     ports:
       - '8094:8094'
   ```

4. Document test data and scenarios

---

## Related Documents

| Document     | Path              | Description                    |
| ------------ | ----------------- | ------------------------------ |
| Architecture | `ARCHITECTURE.md` | Technical architecture details |
| PRD          | `PRD.md`          | Product requirements           |
| Changelog    | `CHANGELOG.md`    | Version history                |
| Guidelines   | `GEMINI.md`       | AI assistant guidelines        |

---

## Constraints

> [!CAUTION]
> **Do NOT** skip these critical rules:

1. **Never** commit sensitive data (API keys, passwords, PII)
2. **Always** use parameterized queries to prevent SQL injection
3. **Never** expose internal service endpoints to public
4. **Always** validate and sanitize all user inputs
5. **Never** bypass authentication/authorization checks
6. **Always** use HTTPS/TLS for all external communications
7. **Never** log sensitive information (passwords, tokens, PII)
8. **Always** update CHANGELOG.md for significant changes
9. **Never** use deprecated security algorithms
10. **Always** implement idempotency for financial operations
