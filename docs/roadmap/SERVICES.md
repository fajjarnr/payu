# PayU Backend Services Status

> **Last Updated**: August 23, 2026
> **Total Services**: 28 (23 microservices + 5 simulators) + 1 frontend (web-app)
> **OpenShift Status**: 🟢 **5 environment hidup penuh** (v1.18.0, snapshot 2026-08-23): `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, dan `payu` (prod, profil lab) — masing-masing menjalankan 25 microservices + 5 simulators + web-app + CNPG PostgreSQL + Kafka + DataGrid + Artemis + Keycloak, semua pod Running Ready, 0 ERROR log. Tekton: 31/31 per-service pipeline hijau di dev; promotion chain dev→sit→uat→preprod→prod terbukti hijau (pilot account-service).
>
> **Sumber kebenaran status kerja**: [`TODOS.md`](./TODOS.md) — file ini ringkasan arsitektur; angka test terverifikasi per 2026-08-11.

---

## Quick Test Status (Last Known/Verified 2026-08-11)

| Status                  | Count     | Notes                         |
| ----------------------- | --------- | ----------------------------- |
| 🟢 **api-commons**       | 178/178   | incl. RateLimitAspect (fail-closed, per-account key) |
| 🟢 **security-starter**  | 56/56     | incl. BlindIndexService (HMAC rotation) |
| 🟢 **account-service**   | 128/128*  | *2 errors pre-existing di VaultConfigurationTest (context JPA broken, INTEGRATION-CTX) |
| 🟢 **auth-service**      | 82/82     | login contract 401/423/429/503, revoke, replay rejection |
| 🟢 **transaction-service**| 142/142   | Money scale 4 (PROD-047) |
| 🟢 **backoffice-service**| 131/131   | BlindIndexService via security-starter |
| 🔴 **Integration tests** | account context broken pre-existing (INTEGRATION-CTX); lihat TODOS |

## Major Updates (March 2026)

- ✅ **Phase 7-12 Complete**: 648 historical bugs fixed across all layers.
- ✅ **Gateway Gaps Closed**: Webhooks, Multi-tenancy, Idempotency, and Escrow implemented.
- ✅ **E2E Coverage**: Added 113 new Playwright tests for 10 critical flows.

---

## 🚨 Platform Readiness Matrix (Audit Status)

> Dirangkum dari audit status semua skills. Lihat masing-masing `SKILL.md` untuk detail.

| Domain | Status | P0 Blockers |
|:-------|:-------|:------------|
| **Core Banking (Hexagonal)** | ✅ Active | Shared starters (outbox, security, cache, resilience) dipakai services inti |
| **Event Architecture** | ✅ Live | Outbox + CloudEvents + DLQ live (PARTNER-PROD-004); topik `.v1` versioned |
| **Security (Zero-Trust)** | ⚠️ Partial | mTLS belum menyeluruh; DB RLS defense-in-depth belum (ACCOUNT-003-RLS); PKCE/MFA (LOGIN-003) |
| **Testing** | ⚠️ Partial | Account integration context broken pre-existing (INTEGRATION-CTX); 6/8 core service integration gap (TEST-GAP) |
| **Container Hardening** | ✅ UBI9 non-root | Semua workload UBI9, UID 1001, drop ALL caps |
| **Data Governance (UU PDP)** | ⚠️ Partial | PII ter-enkripsi at-rest + blind index; audit-log masking live; klasifikasi menyeluruh belum |
| **API Contracts** | ⚠️ Partial | No contract testing (Pact) in CI |
| **Performance Testing** | 🔴 Empty | Load test scaffold empty, no Gatling simulations |

**Services Requiring Immediate Test Coverage:**

| Service | Unit Tests | Integration Tests | Priority |
|:--------|:-----------|:-----------------|:---------|
| `outbox-starter` | 🔴 ZERO | 🔴 ZERO | P0 |
| `saga-starter` | 🔴 ZERO | 🔴 ZERO | P0 |
| `lending-service` | ⚠️ Unit only | 🔴 ZERO | P0 |
| `fx-service` | ⚠️ Unit only | 🔴 ZERO | P0 |
| `cms-service` | ✅ Full (3 new) | 🔴 ZERO | P1 ✅ (READY-001 closed) |
| `ab-testing-service` | ⚠️ Minimal | 🔴 ZERO | P1 |
| `statement-service` | 🔴 Minimal | 🔴 ZERO | P1 |

---

## Services Overview

### ✅ Feature-Complete Services (Implementation)

| Service                     | Language    | Framework         | Status     | Notes                                                       |
| --------------------------- | ----------- | ----------------- | ---------- | ----------------------------------------------------------- |
| **account-service**         | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Hexagonal architecture, eKYC integration, tests (Sec Patch 1.7.6) |
| **auth-service**            | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Keycloak integration, JWT auth, lockout mechanism, tests    |
| **wallet-service**          | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Hexagonal architecture, ledger, virtual cards, Kafka events |
| **transaction-service**     | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Hexagonal architecture, BI-FAST integration, Kafka events   |
| **billing-service**         | Java 21     | Quarkus 3.32.3    | ✅ Complete | Bill payments, wallet integration, Kafka events, tests      |
| **notification-service**    | Java 21     | Quarkus 3.32.3    | ✅ Complete | Multi-channel notifications, Kafka consumer, tests          |
| **gateway-service**         | Java 21     | Quarkus 3.32.3    | ✅ Complete | API routing, rate limiting, circuit breaker, tracing        |
| **kyc-service**             | Python 3.12 | FastAPI 0.115.0   | ✅ Complete | OCR, liveness, face matching, tests                         |
| **analytics-service**       | Python 3.12 | FastAPI 0.115.0   | ✅ Complete | TimescaleDB, ML recommendations, tests                      |
| **bi-fast-simulator**       | Java 21     | Quarkus 3.32.3    | ✅ Complete | BI-FAST API, latency/failure simulation                     |
| **dukcapil-simulator**      | Java 21     | Quarkus 3.32.3    | ✅ Complete | NIK verification, face matching simulation                  |
| **qris-simulator**          | Java 21     | Quarkus 3.32.3    | ✅ Complete | QR generation, payment simulation                           |
| **compliance-service**      | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Regulatory audits, PCI-DSS                                  |
| **support-service**         | Java 21     | Quarkus 3.32.3    | ✅ Complete | Support training mgmt                                       |
| **investment-service**      | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Deposits, Funds, Gold                                       |
| **lending-service**         | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Loans, PayLater                                             |
| **promotion-service**       | Java 21     | Quarkus 3.32.3    | ✅ Complete | Rewards, Cashback                                           |
| **backoffice-service**      | Java 21     | Quarkus 3.32.3    | ✅ Complete | Admin, Fraud Ops                                            |
| **partner-service**         | Java 21     | Quarkus 3.32.3    | ✅ Complete | SNAP BI Basic                                               |
| **dispute-service**         | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Refund & Dispute Management                                 |
| **integration-service**     | Java 21     | Spring Boot 3.4.13 | ✅ Complete | External Swift/ISO20022 Adapter                             |
| **biller-simulator**        | Java 21     | Quarkus 3.32.3    | ✅ Complete | PLN, PDAM, Telco simulation                                 |
| **va-simulator**            | Java 21     | Quarkus 3.32.3    | ✅ Complete | Virtual Account simulation                                  |
| **cms-service**             | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Content Management, Banners                                 |
| **fx-service**              | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Foreign Exchange Rates                                      |
| **product-catalog-service** | Java 21     | Spring Boot 3.4.13 | ✅ Complete | Banking products & fees catalog                             |
| **api-portal-service**      | Java 21     | Quarkus 3.32.3    | ✅ Complete | Centralized OpenAPI Docs & Sandbox                          |
| **statement-service**       | Java 21     | Spring Boot 3.4.13 | ✅ Complete | E-Statement PDF Generation                                  |

---

## Service Details

### Core Banking Services (Spring Boot)

#### 1. account-service (Port 8001)

- ✅ User Management (User, Account, Profile)
- ✅ PII encrypted at rest (AES-GCM) + blind index HMAC lookup (ACCOUNT-001)
- ✅ Registration response & outbox payload PII-minimized (ACCOUNT-004)
- ✅ eKYC Integration with Dukcapil Simulator
- ✅ Tenant dari JWT claim; Hibernate tenant filter di-enforce (ACCOUNT-003)
- ✅ Kafka events via outbox (user-created, user-updated, kyc-completed)
- ✅ Hexagonal Architecture
- ✅ Unit tests 128 (OnboardingControllerTest, BeneficiaryControllerAuthorizationTest, BudgetOwnershipTest, TenantFilterTest, RequestDtoMaskingTest, ArchitectureTest)

#### 2. auth-service (Port 8002)

- ✅ Keycloak Admin Client Integration
- ✅ Login proxy + account lockout (5 attempts / 15 min) + risk evaluation
- ✅ Rate limiting 10/min per IP/account, fail-closed (LOGIN-004)
- ✅ Deterministic error contract: 200 / 401 invalid / 423 locked / 429 / 503 IdP down (LOGIN-005)
- ✅ Logout revoke via Keycloak end_session + refresh replay ditolak (LOGIN-002)
- ✅ OAuth2 Resource Server
- ⚠️ Password grant masih dipakai (LOGIN-003 PKCE/MFA open)
- ✅ Unit tests 82

#### 3. wallet-service (Port 8004)

- ✅ Domain Layer: Wallet, WalletTransaction, Card
- ✅ Ports: WalletUseCase, CardUseCase (input), Persistence (output)
- ✅ Balance Management: get, reserve, commit, release, credit
- ✅ Virtual Debit Card: Create, list, freeze/unfreeze
- ✅ Flyway Migrations (3: wallet tables, cards, ledger)
- ✅ Kafka Events: wallet.created, balance.changed, etc.
- ✅ Unit tests (WalletServiceTest, ControllerTest, ArchitectureTest)

#### 4. transaction-service (Port 8003)

- ✅ Hexagonal Architecture
- ✅ Money scale 4 HALF_EVEN — DECIMAL(19,4) parity (PROD-047)
- ✅ Integration with wallet-service for balance operations
- ✅ Resilience4j Circuit Breaker
- ✅ Kafka Events: transactions.initiated, validated, completed, failed
- ✅ Unit tests 142

#### 5. compliance-service (Port 8087)

- ✅ Regulatory Compliance & Audit
- ✅ Kafka integration
- ✅ PostgreSQL database
- ✅ Spring Boot 3.4.1

### Supporting Services (Quarkus)

#### 6. billing-service (Port 8005)

- ✅ Bill Payments for PLN, PDAM, Pulsa, BPJS
- ✅ REST API: /billers, /payments
- ✅ Wallet Integration for balance debit
- ✅ Kafka Events for payment notifications
- ✅ Hibernate Panache ORM
- ✅ Unit tests (PaymentServiceTest)
- ✅ Architecture Tests
- ✅ Integration Tests (Testcontainers)

#### 7. notification-service (Port 8006)

- ✅ Multi-channel: Email, SMS, Push, In-App
- ✅ REST API: /notifications
- ✅ Kafka Consumer for wallet, transaction, payment events
- ✅ Quarkus Mailer for emails
- ✅ Sender Abstraction: EmailSender, SmsSender, PushSender
- ✅ Unit tests (NotificationServiceTest)
- ✅ Architecture Tests
- ✅ Integration Tests (Testcontainers)

#### 8. gateway-service (Port 8080)

- ✅ API Gateway for all backend services
- ✅ Request Routing to simulators and core services
- ✅ Distributed Rate Limiting with Redis
- ✅ Circuit Breaker with fault tolerance
- ✅ Correlation ID for distributed tracing
- ✅ OIDC/JWT Authentication (Red Hat SSO)
- ✅ Health, Status, Version endpoints
- ✅ Prometheus Metrics + OpenTelemetry Tracing
- ✅ Unit tests (ArchitectureTest, CorrelationIdFilterTest, HealthResourceTest)

#### 9. support-service (Port 8014)

- ✅ Support team training management
- ✅ Hibernate Panache ORM
- ✅ Quarkus 3.17

#### 10. dispute-service (Port 8098)

- ✅ **Refund Management**: Full and partial refunds
- ✅ **Refund State Machine**: PENDING -> PROCESSING -> COMPLETED/FAILED
- ✅ **Dispute Lifecycle**: OPEN -> INVESTIGATING -> RESOLVED/ESCALATED/REJECTED
- ✅ **Evidence Management**: File attachments with URL storage
- ✅ **Hexagonal Architecture**: Domain, Application, Adapter layers
- ✅ **Unit Tests**: RefundTest, DisputeTest (domain), RefundServiceTest, DisputeServiceTest
- ✅ **Integration Tests**: Testcontainers with PostgreSQL

#### 11. fx-service (Port 8096)

- ✅ Currency exchange rates & conversion
- ✅ Daily rate updates via scheduled tasks
- ✅ Support for major currencies (IDR, USD, EUR, SGD)
- ✅ REST API: `/fx-api/rates`, `/fx-api/convert`
- ✅ Spring Boot 3.4.1 + PostgreSQL

#### 12. statement-service (Port 8015)

- ✅ PDF e-Statement generation
- ✅ Integration with wallet and transaction services
- ✅ Storage integration with RustFS (S3 compatible)
- ✅ Async generation via Kafka events
- ✅ REST API: `/api/v1/statements`

#### 13. integration-service (Port 8101)

- ✅ External Swift/ISO20022 Adapter
- ✅ Transaction mapping and validation
- ✅ External system connectivity management
- ✅ Spring Boot 3.4.1

#### 14. product-catalog-service (Port 8100)

- ✅ Banking products & fees catalog
- ✅ Product lifecycle management
- ✅ Dynamic fee calculation rules
- ✅ Spring Boot 3.4.1

#### 15. cms-service (Port 8095)

- ✅ Content Management for Mobile/Web
- ✅ Dynamic Banners and Promos
- ✅ In-app notification content
- ✅ Spring Boot 3.4.1
- ✅ `cms-service:1.8.12` — `@Cacheable` round-trip fixed (TypedJsonRedisSerializer, 3 characterization tests green, E2E proven in `payu-dev`)

#### 16. api-portal-service (Port 8021)

- ✅ Centralized OpenAPI Docs & Sandbox
- ✅ Partner developer portal
- ✅ API documentation aggregation
- ✅ Quarkus 3.17

### ML/Data Services (Python)

#### 17. kyc-service (Port 8007)

- ✅ Full eKYC implementation
- ✅ **OCR Service**: PaddleOCR for Indonesian KTP scanning
- ✅ **Liveness Detection**: Computer vision-based anti-spoofing
- ✅ **Face Matching**: Cosine similarity KTP vs selfie
- ✅ **Dukcapil Integration**: Real-time NIK verification
- ✅ PostgreSQL with asyncpg + SQLAlchemy 2.0
- ✅ Kafka Producer for KYC events
- ✅ E2E Tests (complete workflow)
- ✅ Unit Tests (OCR, Liveness, Face, Dukcapil)
- ✅ UBI9 Dockerfile (multi-stage)
- ✅ Monitoring: Prometheus + OpenTelemetry + structured logs
- ⚠️ Local test setup may require additional Python native libs

#### 18. analytics-service (Port 8008)

- ✅ Time-series analytics with TimescaleDB
- ✅ Kafka Consumer for real-time event consumption
- ✅ Hypertables: transactions, wallet balances, user activities
- ✅ User Metrics: total transactions, amount, average, account age
- ✅ Spending Insights: trends, categories, MoM change
- ✅ Cash Flow Analysis: income vs expenses
- ✅ ML Recommendations: savings, budget alerts, trends
- ✅ E2E Tests (complete user journey)
- ✅ Unit Tests (recommendation engine, analytics service)
- ✅ UBI9 Dockerfile (multi-stage)
- ✅ Monitoring: Prometheus + OpenTelemetry + structured logs
- ⚠️ Local test setup may require additional Python native libs

### External Service Simulators (Quarkus)

#### 19. bi-fast-simulator (Port 8090)

- ✅ Account Inquiry endpoint
- ✅ Fund Transfer endpoint
- ✅ Status Check endpoint
- ✅ Configurable Latency (50-500ms)
- ✅ Configurable Failure Rate (5% default)
- ✅ Test Bank Accounts (BCA, BRI, MANDIRI, BNI, etc.)
- ✅ Blocked/Timeout Scenarios
- ✅ Health Checks & Prometheus Metrics
- ✅ OpenTelemetry Tracing

#### 20. dukcapil-simulator (Port 8091)

- ✅ NIK Verification endpoint
- ✅ Face Matching endpoint
- ✅ Citizen Data Retrieval
- ✅ Configurable Latency (100-800ms)
- ✅ Configurable Failure Rate (3% default)
- ✅ Simulated Face Match Scores (threshold 75%)
- ✅ Liveness Detection Simulation
- ✅ Test Citizens (VALID, BLOCKED, INVALID, DECEASED)
- ✅ Verification Audit Logging
- ✅ Health Checks & Prometheus Metrics

#### 21. qris-simulator (Port 8092)

- ✅ QR Code Generation endpoint
- ✅ Payment Simulation endpoint
- ✅ Status Check endpoint
- ✅ Real QR Code Generation (ZXing library)
- ✅ QRIS-Compliant QR Content Format
- ✅ Configurable Latency (50-300ms)
- ✅ Configurable Failure Rate (2% default)
- ✅ QR Expiry Handling (5 minutes default)
- ✅ Test Merchants (Food & Beverage, Electronics, Health, etc.)
- ✅ Health Checks & Prometheus Metrics

---

## Testing Infrastructure

### Test Commands Quick Reference

```bash
# Spring Boot Services
cd backend/<service> && mvn test                    # Run all tests
cd backend/<service> && mvn test -Dtest=*Test       # Unit tests only
cd backend/<service> && mvn test jacoco:report      # With coverage

# Quarkus Services  
cd backend/<service> && ./mvnw test                 # Run all tests
cd backend/<service> && ./mvnw test -Dtest=*Test    # Unit tests only

# Python Services
cd backend/<service> && pytest -v                   # Run all tests
cd backend/<service> && pytest --cov=src            # With coverage

# Full Suite (when working)
./scripts/run-all-tests.sh
./scripts/test-single-service.sh <service-name>
```

### Test Categories

| Category           | Framework                  | Scope                        |
| ------------------ | -------------------------- | ---------------------------- |
| Unit Tests         | JUnit 5 / Mockito / pytest | Isolated business logic      |
| Architecture Tests | ArchUnit                   | Layer dependency enforcement |
| Controller Tests   | @WebMvcTest / @QuarkusTest | REST API endpoints           |
| Integration Tests  | Testcontainers             | PostgreSQL, Kafka, Keycloak  |
| E2E Tests          | Full workflow              | Complete user journeys       |

### Known Issues & Fixes

| Issue                  | Affected Services                          | Fix                             |
| ---------------------- | ------------------------------------------ | ------------------------------- |
| Testcontainers Docker  | billing, notification, partner, backoffice | Start Docker daemon             |
| EncryptionService Bean | All using security-starter                 | Add @ConditionalOnProperty      |
| Resilience4j 2.x API   | All using resilience-starter               | Update to new builder API       |
| Python shared libs     | kyc, analytics                             | Inline or create shared package |
| Gateway env config     | gateway-service                            | Mock Redis/Keycloak in tests    |

### E2E Tests

- ✅ KYC Service E2E: Complete workflow (start → KTP → selfie → verified)
- ✅ Analytics Service E2E: Complete user journey with analytics
- ✅ podman-compose.yml: Unified dev/test environment

### Unit Tests (terverifikasi 2026-08-11)

- ✅ api-commons: 178 (RateLimitAspect fail-closed, idempotency, money)
- ✅ security-starter: 56 (EncryptionService rotation, BlindIndexService, masking, audit)
- ✅ Account Service: 128 (service, controller, authorization, tenant, masking)
- ✅ Auth Service: 82 (login contract, revoke, refresh, rate-limit)
- ✅ Transaction Service: 142 (Money scale 4, transfer flows)
- ✅ Backoffice Service: 131 (PII encryption, blind index, backfill)
- ✅ Wallet Service, Billing, Notification, Gateway: green pada run terakhir (lihat CI)
- ⚠️ Account integration context broken pre-existing (INTEGRATION-CTX di TODOS)

---

## Summary

**Status**: 28 module ✅ IMPLEMENTED; money-flow live di `payu-dev`; production deployment belum (ACCOUNT-007/CB-006).

- Core Banking P0 security: ACCOUNT-001..004, LOGIN-002/004/005, PROD-047 CLOSED (2026-08-11)
- SNAP-BI partner gates PARTNER-PROD-001..006 LIVE (sandbox)
- **Lihat [`TODOS.md`](./TODOS.md)** untuk backlog aktif & gate — jangan gunakan bagian file ini sebagai sumber status kerja.
