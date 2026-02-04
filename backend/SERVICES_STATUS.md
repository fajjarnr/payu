# PayU Backend Services Status

> **Last Updated**: February 4, 2026  
> **Total Services**: 22 (19 microservices + 3 simulators)  
> **Lab Status**: ✅ Feature Complete | ⚠️ Test & Environment Stabilization In Progress
>
> **Note**: Container internal ports are standardized to 8080 in `docker-compose.yml`. Use the compose file for external mappings.

---

## Quick Test Status (Last Known)

| Status | Count | Services |
|--------|-------|----------|
| ✅ Unit Tests Passing | 6 | account, auth, support, billing*, notification*, transaction* |
| ⚠️ Integration Requires Docker | 5 | transaction-int, billing-int, notification-int, partner, backoffice |
| ❌ Coverage Needs Expansion | 8 | wallet, investment, lending, fx, statement, cms, ab-testing, promotion |
| 🔴 Local Setup Blocked | 2 | kyc (Python libs), analytics (Python libs) |

*Unit tests pass, integration tests need Docker

## Known Issues / WIP (Feb 2026)

- Public endpoint `/api/v1/accounts/register` still returns 401 due to OAuth2 resource server filter ordering.
- Full OpenAPI contract validation, seed data alignment, RBAC checks, error code standardization, and observability verification are pending.
- E2E pass rate fixes are documented in `docs/test-improvements.md`; re-run full suite to confirm 95%+.
- Local Python test setup for `kyc-service` and `analytics-service` may require extra native libs.

---

## Services Overview

### ✅ Feature-Complete Services (Implementation)

| Service | Language | Framework | Status | Notes |
|---------|----------|------------|--------|-------|
| **account-service** | Java 21 | Spring Boot 3.4.1 | ✅ Complete | Hexagonal architecture, eKYC integration, tests |
| **auth-service** | Java 21 | Spring Boot 3.4.1 | ✅ Complete | Keycloak integration, JWT auth, lockout mechanism, tests |
| **wallet-service** | Java 21 | Spring Boot 3.4.1 | ✅ Complete | Hexagonal architecture, ledger, virtual cards, Kafka events |
| **transaction-service** | Java 21 | Spring Boot 3.4.1 | ✅ Complete | Hexagonal architecture, BI-FAST integration, Kafka events |
| **billing-service** | Java 21 | Quarkus 3.17 | ✅ Complete | Bill payments, wallet integration, Kafka events, tests |
| **notification-service** | Java 21 | Quarkus 3.17 | ✅ Complete | Multi-channel notifications, Kafka consumer, tests |
| **gateway-service** | Java 21 | Quarkus 3.17 | ✅ Complete | API routing, rate limiting, circuit breaker, tracing |
| **kyc-service** | Python 3.12 | FastAPI 0.115.0 | ✅ Complete | OCR, liveness, face matching, tests |
| **analytics-service** | Python 3.12 | FastAPI 0.115.0 | ✅ Complete | TimescaleDB, ML recommendations, tests |
| **bi-fast-simulator** | Java 21 | Quarkus 3.17 | ✅ Complete | BI-FAST API, latency/failure simulation |
| **dukcapil-simulator** | Java 21 | Quarkus 3.17 | ✅ Complete | NIK verification, face matching simulation |
| **qris-simulator** | Java 21 | Quarkus 3.17 | ✅ Complete | QR generation, payment simulation |
| **compliance-service** | Java 21 | Spring Boot 3.4.1 | ✅ Complete | Regulatory audits, PCI-DSS |
| **support-service** | Java 21 | Quarkus 3.17 | ✅ Complete | Support training mgmt |
| **investment-service**| Java 21 | Spring Boot 3.4.1 | ✅ Complete | Deposits, Funds, Gold |
| **lending-service** | Java 21 | Spring Boot 3.4.1 | ✅ Complete | Loans, PayLater |
| **promotion-service** | Java 21 | Quarkus 3.17 | ✅ Complete | Rewards, Cashback |
| **backoffice-service** | Java 21 | Quarkus 3.17 | ✅ Complete | Admin, Fraud Ops |
| **partner-service** | Java 21 | Quarkus 3.17 | ✅ Complete | SNAP BI Basic |

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

#### 9. support-service (Port 8086)
- ✅ Support team training management
- ✅ Hibernate Panache ORM
- ✅ Quarkus 3.17

### ML/Data Services (Python)

#### 10. kyc-service (Port 8007)
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

#### 11. analytics-service (Port 8008)
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

#### 12. bi-fast-simulator (Port 8090)
- ✅ Account Inquiry endpoint
- ✅ Fund Transfer endpoint
- ✅ Status Check endpoint
- ✅ Configurable Latency (50-500ms)
- ✅ Configurable Failure Rate (5% default)
- ✅ Test Bank Accounts (BCA, BRI, MANDIRI, BNI, etc.)
- ✅ Blocked/Timeout Scenarios
- ✅ Health Checks & Prometheus Metrics
- ✅ OpenTelemetry Tracing

#### 13. dukcapil-simulator (Port 8091)
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

#### 14. qris-simulator (Port 8092)
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

| Category | Framework | Scope |
|----------|-----------|-------|
| Unit Tests | JUnit 5 / Mockito / pytest | Isolated business logic |
| Architecture Tests | ArchUnit | Layer dependency enforcement |
| Controller Tests | @WebMvcTest / @QuarkusTest | REST API endpoints |
| Integration Tests | Testcontainers | PostgreSQL, Kafka, Keycloak |
| E2E Tests | Full workflow | Complete user journeys |

### Known Issues & Fixes

| Issue | Affected Services | Fix |
|-------|-------------------|-----|
| Testcontainers Docker | billing, notification, partner, backoffice | Start Docker daemon |
| EncryptionService Bean | All using security-starter | Add @ConditionalOnProperty |
| Resilience4j 2.x API | All using resilience-starter | Update to new builder API |
| Python shared libs | kyc, analytics | Inline or create shared package |
| Gateway env config | gateway-service | Mock Redis/Keycloak in tests |

### E2E Tests
- ✅ KYC Service E2E: Complete workflow (start → KTP → selfie → verified)
- ✅ Analytics Service E2E: Complete user journey with analytics
- ✅ docker-compose.test.yml: Complete test environment

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

**Status**: All 22 services are ✅ **IMPLEMENTED** with:
- Full implementation
- Dockerfiles (UBI9 compliant)
- Database migrations (Flyway)
- Kafka integration
- Monitoring (Prometheus + OpenTelemetry)

**Test Status**: ⚠️ **IN PROGRESS**
- Unit tests: last known passing for core services; expand coverage for remaining services
- Integration tests: require Docker/Testcontainers
- E2E: fixes applied (see `docs/test-improvements.md`), re-run to confirm 95%+ pass rate
- Env: public endpoint 401 issue pending resolution

**Ready for**: 
- Docker Compose local development
- OpenShift deployment
- Demo/presentation

**Needs Work**:
1. Fix shared library issues (security-starter, resilience-starter)
2. Complete unit tests for all services
3. Fix integration test Docker dependencies
4. Achieve 80% coverage target
