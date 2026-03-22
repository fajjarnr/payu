# PayU Backend Services Status

> **Last Updated**: March 22, 2026
> **Total Services**: 28 (23 microservices + 5 simulators)  
> **Lab Status**: 🟢 Production Ready (Phase 1–12 Complete) | ⚠️ Post-Audit Remediation In Progress (56 Open Bugs)
>
> **Note**: All 703 E2E tests (544 Playwright + 159 Pytest) are 🟢 PASSING in local environment.

---

## Quick Test Status (Last Known/Verified)

| Status                  | Count     | Notes                         |
| ----------------------- | --------- | ----------------------------- |
| 🟢 **E2E (Playwright)**  | 544/544   | 100% Pass per Mar 17          |
| 🟢 **E2E (Pytest)**      | 159/159   | 100% Pass per Mar 17          |
| 🟢 **Unit Tests**        | ~700+     | All core services passing     |
| 🟢 **Build Success**     | 38/38     | All Maven modules SUCCESS     |
| 🔴 **Open Bugs**         | 56        | Deep Audit 2026-03-21 items   |

## Major Updates (March 2026)

- ✅ **Phase 7-12 Complete**: 648 historical bugs fixed across all layers.
- ✅ **Gateway Gaps Closed**: Webhooks, Multi-tenancy, Idempotency, and Escrow implemented.
- ✅ **E2E Coverage**: Added 113 new Playwright tests for 10 critical flows.

---

## Services Overview

### ✅ Feature-Complete Services (Implementation)

| Service                     | Language    | Framework         | Status     | Notes                                                       |
| --------------------------- | ----------- | ----------------- | ---------- | ----------------------------------------------------------- |
| **account-service**         | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Hexagonal architecture, eKYC integration, tests             |
| **auth-service**            | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Keycloak integration, JWT auth, lockout mechanism, tests    |
| **wallet-service**          | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Hexagonal architecture, ledger, virtual cards, Kafka events |
| **transaction-service**     | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Hexagonal architecture, BI-FAST integration, Kafka events   |
| **billing-service**         | Java 21     | Quarkus 3.17      | ✅ Complete | Bill payments, wallet integration, Kafka events, tests      |
| **notification-service**    | Java 21     | Quarkus 3.17      | ✅ Complete | Multi-channel notifications, Kafka consumer, tests          |
| **gateway-service**         | Java 21     | Quarkus 3.17      | ✅ Complete | API routing, rate limiting, circuit breaker, tracing        |
| **kyc-service**             | Python 3.12 | FastAPI 0.115.0   | ✅ Complete | OCR, liveness, face matching, tests                         |
| **analytics-service**       | Python 3.12 | FastAPI 0.115.0   | ✅ Complete | TimescaleDB, ML recommendations, tests                      |
| **bi-fast-simulator**       | Java 21     | Quarkus 3.17      | ✅ Complete | BI-FAST API, latency/failure simulation                     |
| **dukcapil-simulator**      | Java 21     | Quarkus 3.17      | ✅ Complete | NIK verification, face matching simulation                  |
| **qris-simulator**          | Java 21     | Quarkus 3.17      | ✅ Complete | QR generation, payment simulation                           |
| **compliance-service**      | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Regulatory audits, PCI-DSS                                  |
| **support-service**         | Java 21     | Quarkus 3.17      | ✅ Complete | Support training mgmt                                       |
| **investment-service**      | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Deposits, Funds, Gold                                       |
| **lending-service**         | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Loans, PayLater                                             |
| **promotion-service**       | Java 21     | Quarkus 3.17      | ✅ Complete | Rewards, Cashback                                           |
| **backoffice-service**      | Java 21     | Quarkus 3.17      | ✅ Complete | Admin, Fraud Ops                                            |
| **partner-service**         | Java 21     | Quarkus 3.17      | ✅ Complete | SNAP BI Basic                                               |
| **dispute-service**         | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Refund & Dispute Management                                 |
| **integration-service**     | Java 21     | Spring Boot 3.4.1 | ✅ Complete | External Swift/ISO20022 Adapter                             |
| **biller-simulator**        | Java 21     | Quarkus 3.17      | ✅ Complete | PLN, PDAM, Telco simulation                                 |
| **va-simulator**            | Java 21     | Quarkus 3.17      | ✅ Complete | Virtual Account simulation                                  |
| **cms-service**             | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Content Management, Banners                                 |
| **fx-service**              | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Foreign Exchange Rates                                      |
| **product-catalog-service** | Java 21     | Spring Boot 3.4.1 | ✅ Complete | Banking products & fees catalog                             |
| **api-portal-service**      | Java 21     | Quarkus 3.17      | ✅ Complete | Centralized OpenAPI Docs & Sandbox                          |
| **statement-service**       | Java 21     | Spring Boot 3.4.1 | ✅ Complete | E-Statement PDF Generation                                  |

---

## Service Details

### Core Banking Services (Spring Boot)

#### 1. account-service (Port 8001)

- ✅ User Management (User, Account, Profile)
- ✅ PostgreSQL integration with JSONB
- ✅ eKYC Integration with Dukcapil Simulator
- ✅ Kafka Producer for user events
- ✅ Hexagonal Architecture
- ✅ Unit tests (OnboardingServiceTest, ControllerTest, ArchitectureTest)

#### 2. auth-service (Port 8002)

- ✅ Keycloak Admin Client Integration
- ✅ Login Proxy with WebClient (Password Grant)
- ✅ User Registration
- ✅ OAuth2 Resource Server
- ✅ Account Lockout (5 failed attempts, 15 min duration)
- ✅ Rate Limiting (5 attempts per minute)
- ✅ Password Policy Enforcement
- ✅ Resilience4j Circuit Breaker & Retry
- ✅ Integration Tests (Testcontainers + Keycloak)

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
- ✅ Integration with wallet-service for balance operations
- ✅ Resilience4j Circuit Breaker
- ✅ Kafka Events: transactions.initiated, validated, completed, failed
- ✅ Unit tests (TransactionServiceTest, ArchitectureTest)

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

### Unit Tests

- ✅ Account Service: Service, Controller, Architecture tests (40 tests)
- ✅ Auth Service: Integration tests with Keycloak (67 tests)
- ✅ Wallet Service: Service, Controller, Architecture tests (compiles)
- ✅ Transaction Service: Service, Architecture tests (60 tests)
- ✅ Billing Service: Service, Controller, Architecture, Integration tests (51 tests)
- ✅ Notification Service: Service, Resource, Architecture, Integration tests (51 tests)
- ⚠️ Gateway Service: Filter, Health tests (49/94 passing)
- ✅ Support Service: ALL PASSING (17 tests) - Reference implementation
- ✅ KYC Service: OCR, Liveness, Face, Dukcapil unit tests
- ✅ Analytics Service: Recommendation engine, Analytics service tests

---

## Summary

**Status**: All 28 modules are ✅ **IMPLEMENTED** and 🟢 **E2E VERIFIED**.

- Production parity images (UBI9)
- 703/703 E2E Tests Passing
- Core Banking & Gateway gaps closed (Mar 16)

**Next Steps (Remediation)**

1. Address **56 open bugs** from Deep Audit 2026-03-21 (mostly P1/P2 logic).
2. Implement PII masking in all backoffice logs (BUG-SECURITY-004-006).
3. Finalize IDOR/Broken Access Control fixes (BUG-SECURITY-027).
4. Resolve "Account Lockout Bypass" in Keycloak integration (BUG-SECURITY-008).
