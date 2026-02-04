# Service Catalog

> **Complete inventory of all PayU Digital Banking Platform services**

## 📊 Services Overview

| Category | Services | Count |
|----------|----------|-------|
| **Core Banking** | account, auth, transaction, wallet, investment, lending, fx, statement | 8 |
| **Operations** | billing, notification, compliance | 3 |
| **Platform** | gateway, api-portal, cms, ab-testing | 4 |
| **Support** | support, backoffice, partner, promotion | 4 |
| **ML/Analytics** | kyc (Python), analytics (Python) | 2 |
| **Simulators** | bi-fast, dukcapil, qris | 3 |

**Total: 24 services (21 microservices + 3 simulators)**

> **Port Standard**: All services expose port **8080** inside containers. Refer to `docker-compose.yml` / `docker-compose.test.yml` for host mappings.

---

## 🏦 Core Banking Services

### account-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_account) |
| **Port** | 8080 (container) |
| **Responsibilities** | User accounts, multi-pocket, profile management |
| **Owner** | Core Banking Team |
| **Documentation** | [README.md](../../backend/account-service/README.md) |

**Key APIs:**
- `POST /api/v1/accounts` - Open new account
- `GET /api/v1/accounts/{id}` - Get account details
- `POST /api/v1/accounts/{id}/pockets` - Create savings pocket

---

### auth-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4, Keycloak |
| **Database** | PostgreSQL (payu_auth) |
| **Port** | 8080 (container) |
| **Responsibilities** | Authentication, MFA, OAuth2, session management |
| **Owner** | Core Banking Team |
| **Documentation** | [README.md](../../backend/auth-service/README.md) |

**Key APIs:**
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout
- `POST /api/v1/auth/refresh` - Refresh access token

---

### transaction-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL + Event Store (payu_transaction) |
| **Port** | 8080 (container) |
| **Responsibilities** | Transfers, BI-FAST, QRIS, payment processing |
| **Owner** | Core Banking Team |
| **Documentation** | [README.md](../../backend/transaction-service/README.md) |

**Key APIs:**
- `POST /api/v1/transfers` - Initiate transfer
- `GET /api/v1/transactions/{id}` - Get transaction status
- `POST /api/v1/payments/qris` - QRIS payment

---

### wallet-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_wallet) |
| **Port** | 8080 (container) |
| **Responsibilities** | Balance management, double-entry ledger |
| **Owner** | Core Banking Team |
| **Documentation** | [README.md](../../backend/wallet-service/README.md) |

**Key APIs:**
- `GET /api/v1/wallet/balance` - Get wallet balance
- `POST /api/v1/wallet/reserve` - Reserve balance for transfer
- `POST /api/v1/wallet/commit` - Commit reserved balance

---

### investment-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_investment) |
| **Port** | 8080 (container) |
| **Responsibilities** | Mutual funds, Gold investment, Portfolio management |
| **Owner** | Financial Products Team |
| **Documentation** | [README.md](../../backend/investment-service/README.md) |

---

### lending-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_lending) |
| **Port** | 8080 (container) |
| **Responsibilities** | Loans, PayLater, Credit scoring integration |
| **Owner** | Financial Products Team |
| **Documentation** | [README.md](../../backend/lending-service/README.md) |

---

### fx-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_fx) |
| **Port** | 8080 (container) |
| **Responsibilities** | Currency exchange rates, conversion logic |
| **Owner** | Financial Products Team |
| **Documentation** | [README.md](../../backend/fx-service/README.md) |

---

### statement-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_statement) |
| **Port** | 8080 (container) |
| **Responsibilities** | PDF E-Statement generation & storage |
| **Owner** | Core Banking Team |
| **Documentation** | [README.md](../../backend/statement-service/README.md) |

---

## 🔧 Operations Services

### billing-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x Native |
| **Database** | PostgreSQL (payu_billing) |
| **Port** | 8080 (container) |
| **Responsibilities** | Bill payments (PLN, PDAM, etc) |
| **Owner** | Operations Team |
| **Documentation** | [README.md](../../backend/billing-service/README.md) |

---

### notification-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x Native |
| **Database** | PostgreSQL (payu_notification) |
| **Messaging** | AMQ Broker (AMQP 1.0) |
| **Port** | 8080 (container) |
| **Responsibilities** | Push, SMS, Email, WhatsApp notifications |
| **Owner** | Operations Team |
| **Documentation** | [README.md](../../backend/notification-service/README.md) |

---

### compliance-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_compliance) |
| **Port** | 8080 (container) |
| **Responsibilities** | Regulatory compliance, AML/CFT, transaction screening |
| **Owner** | Compliance Team |
| **Documentation** | [README.md](../../backend/compliance-service/README.md) |

---

## 🚪 Platform Services

### gateway-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x Native |
| **Port** | 8080 (container) |
| **Responsibilities** | API Gateway, Rate limiting, Circuit breaker |
| **Owner** | Platform Team |
| **Documentation** | [README.md](../../backend/gateway-service/README.md) |

---

### api-portal-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x Native |
| **Port** | 8080 (container) |
| **Responsibilities** | Centralized OpenAPI Docs & Sandbox |
| **Owner** | Platform Team |
| **Documentation** | [README.md](../../backend/api-portal-service/README.md) |

---

### cms-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_cms) |
| **Port** | 8080 (container) |
| **Responsibilities** | Banners, Promos, Dynamic App Content |
| **Owner** | Platform Team |
| **Documentation** | [README.md](../../backend/cms-service/README.md) |

---

### ab-testing-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_abtesting) |
| **Port** | 8080 (container) |
| **Responsibilities** | Feature flags, Experimentation, Variant bucketing |
| **Owner** | Product Team |
| **Documentation** | [README.md](../../backend/ab-testing-service/README.md) |

---

## 🎧 Support Services

### backoffice-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_backoffice) |
| **Port** | 8080 (container) |
| **Responsibilities** | Internal admin dashboard, audit, user management |
| **Owner** | Operations Team |
| **Documentation** | [README.md](../../backend/backoffice-service/README.md) |

---

### partner-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_partner) |
| **Port** | 8080 (container) |
| **Responsibilities** | Partner integration, API key management, webhooks |
| **Owner** | Business Development Team |
| **Documentation** | [README.md](../../backend/partner-service/README.md) |

---

### promotion-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_promotion) |
| **Port** | 8080 (container) |
| **Responsibilities** | Promo campaigns, vouchers, rewards, cashback |
| **Owner** | Marketing Team |
| **Documentation** | [README.md](../../backend/promotion-service/README.md) |

---

### support-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4 |
| **Database** | PostgreSQL (payu_support) |
| **Port** | 8080 (container) |
| **Responsibilities** | Customer support, ticketing, FAQ, chat support |
| **Owner** | Customer Experience Team |
| **Documentation** | [README.md](../../backend/support-service/README.md) |

---

## 🤖 ML/Analytics Services

### kyc-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Python 3.12, FastAPI (UBI-based) |
| **Database** | PostgreSQL (payu_kyc) with JSONB |
| **Port** | 8080 (container) |
| **Responsibilities** | eKYC, OCR, Liveness detection |
| **Owner** | Data Science Team |
| **Documentation** | [README.md](../../backend/kyc-service/README.md) |

---

### analytics-service

| Attribute | Value |
|-----------|-------|
| **Technology** | Python 3.12, FastAPI (UBI-based) |
| **Database** | PostgreSQL + TimescaleDB |
| **Port** | 8080 (container) |
| **Responsibilities** | Fraud scoring, User insights |
| **Owner** | Data Science Team |
| **Documentation** | [README.md](../../backend/analytics-service/README.md) |

---

## 🔌 Simulators (Development)

### bi-fast-simulator

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x |
| **Port** | 8080 (container) |
| **Responsibilities** | BI-FAST transfer simulation |
| **Documentation** | [README.md](../../backend/simulators/bi-fast-simulator/README.md) |

---

### dukcapil-simulator

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x |
| **Port** | 8080 (container) |
| **Responsibilities** | Dukcapil identity verification simulation |
| **Documentation** | [README.md](../../backend/simulators/dukcapil-simulator/README.md) |

---

### qris-simulator

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x |
| **Port** | 8080 (container) |
| **Responsibilities** | QRIS payment simulation |
| **Documentation** | [README.md](../../backend/simulators/qris-simulator/README.md) |

---

## 📚 Shared Libraries

| Library | Purpose | Documentation |
|---------|---------|---------------|
| **security-starter** | PII encryption, Data masking, Audit logging | [README.md](../../backend/shared/security-starter/README.md) |
| **resilience-starter** | Circuit Breaker, Retry, Bulkhead | [README.md](../../backend/shared/resilience-starter/README.md) |
| **cache-starter** | Multi-layer caching (Redis + Caffeine) | [README.md](../../backend/shared/cache-starter/README.md) |

---

## 🔗 Dependencies

### External Services

| Service | Provider | Purpose |
|---------|----------|---------|
| **PostgreSQL** | Crunchy Data | Primary database |
| **Redis/Data Grid** | Red Hat | Caching layer |
| **Kafka** | AMQ Streams | Event streaming |
| **Keycloak** | Red Hat SSO | Identity & access |
| **SendGrid** | SendGrid | Email service |
| **Twilio** | Twilio | SMS service |

---

_Last Updated: February 4, 2026_
