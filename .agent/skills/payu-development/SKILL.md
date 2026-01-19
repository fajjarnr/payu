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

| Layer | Technology | Notes |
|-------|------------|-------|
| Container Platform | OpenShift 4.20+ | Kubernetes-compatible |
| Core Banking | Spring Boot 3.4 (Java 21) | Axon Framework for CQRS/ES |
| Supporting Services | Quarkus 3.x (Native) | <50ms startup, <50MB RAM |
| ML Services | FastAPI (Python 3.12) | PyTorch, scikit-learn |
| Database | PostgreSQL 16 (Crunchy) | JSONB for documents |
| Cache | Data Grid (RESP mode) | Redis-compatible API |
| Event Streaming | AMQ Streams (Kafka) | Event sourcing, sagas |
| Message Queue | AMQ Broker (AMQP 1.0) | Point-to-point messaging |
| Identity | Red Hat SSO (Keycloak) | OAuth2/OIDC |

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

| Service | Port | Technology | Package |
|---------|------|------------|---------|
| account-service | 8001 | Spring Boot 3.4 | `id.payu.account` |
| auth-service | 8002 | Spring Boot 3.4 | `id.payu.auth` |
| transaction-service | 8003 | Spring Boot 3.4 | `id.payu.transaction` |
| wallet-service | 8004 | Spring Boot 3.4 | `id.payu.wallet` |
| billing-service | 8005 | Quarkus 3.x | `id.payu.billing` |
| notification-service | 8006 | Quarkus 3.x | `id.payu.notification` |
| gateway-service | 8080 | Quarkus 3.x | `id.payu.gateway` |
| kyc-service | 9001 | FastAPI | `app` |
| analytics-service | 9002 | FastAPI | `app` |

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

| Tool | Version | Purpose |
|------|---------|---------|
| JUnit 5 | Latest | Test framework |
| Mockito | Latest | Mocking library |
| Testcontainers | Latest | Integration tests (PostgreSQL, Kafka) |
| ArchUnit | 1.2.1 | Architecture rule enforcement |
| JaCoCo | 0.8.11 | Code coverage |
| Spring Security Test | Latest | Security context testing |

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

---

## Security Requirements

### Authentication Flow

1. User login with phone + PIN
2. Challenge (OTP/Biometric)
3. Access Token + Refresh Token (JWT)
4. Token validation via Keycloak

### Security Layers

| Layer | Implementation |
|-------|---------------|
| Perimeter | WAF, DDoS protection |
| Network | VPC, mTLS (Istio) |
| Application | OAuth2/OIDC, JWT (15min expiry) |
| Data | AES-256 encryption, Field-level PII encryption |

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
  "data": { },
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
    "details": { }
  },
  "meta": {
    "requestId": "uuid",
    "timestamp": "ISO-8601"
  }
}
```

---

## Code Style and Conventions

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Variables/Functions | camelCase | `accountBalance` |
| Classes | PascalCase | `AccountService` |
| Files | kebab-case | `account-service.java` |
| Packages | lowercase | `id.payu.account` |
| Constants | UPPER_SNAKE | `MAX_RETRY_COUNT` |

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

| Library | Context7 ID |
|---------|-------------|
| Spring Boot | `/spring-projects/spring-boot` |
| Quarkus | `/quarkusio/quarkus` |
| FastAPI | `/tiangolo/fastapi` |
| Kafka | `/apache/kafka` |
| PostgreSQL | `/postgres/postgres` |

---

## Related Documents

| Document | Path | Description |
|----------|------|-------------|
| Architecture | `ARCHITECTURE.md` | Technical architecture details |
| PRD | `PRD.md` | Product requirements |
| Changelog | `CHANGELOG.md` | Version history |
| Guidelines | `GEMINI.md` | AI assistant guidelines |

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
