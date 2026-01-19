---
name: payu-development
description: Skill untuk mengembangkan PayU Digital Banking Platform - mencakup microservices architecture (Spring Boot, Quarkus, FastAPI), event-driven patterns, PostgreSQL databases, dan integrasi dengan Red Hat OpenShift ecosystem.
---

# PayU Digital Banking Development Skill

Skill ini memberikan panduan komprehensif untuk pengembangan **PayU Digital Banking Platform**, sebuah platform digital banking modern dengan arsitektur microservices dan event-driven di atas Red Hat OpenShift.

## Project Overview

| Attribute | Value |
|-----------|-------|
| **Project Name** | PayU |
| **Type** | Standalone Digital Banking Platform |
| **Platform** | Red Hat OpenShift 4.20+ |
| **Primary Languages** | Java 21 (Core), Python 3.12 (ML) |

### Technology Stack

| Layer               | Technology                | Notes                      |
| ------------------- | ------------------------- | -------------------------- |
| **Container**       | OpenShift 4.20+           | Kubernetes-compatible      |
| **Core Banking**    | Spring Boot 3.4           | Java 21, Axon Framework    |
| **Supporting**      | Quarkus 3.x Native        | High density, fast startup |
| **ML Services**     | FastAPI (Python 3.12)     | PyTorch, scikit-learn      |
| **Database**        | PostgreSQL 16 (Crunchy)   | Unified DB with schemas    |
| **Cache**           | Data Grid (Redis API)     | RESP protocol mode         |
| **Events**          | AMQ Streams (Kafka)       | Event sourcing, Sagas      |
| **Messaging**       | AMQ Broker (ActiveMQ)     | Notifications, Webhooks    |
| **Identity**        | Red Hat SSO (Keycloak)    | OIDC/OAuth2                |

## Service Architecture

### Service Map
```
payu/backend/
├── account-service/      # [Spring Boot] User accounts, eKYC
├── auth-service/         # [Spring Boot] Authentication, OAuth2
├── transaction-service/  # [Spring Boot] Transfers, BI-FAST, QRIS
├── wallet-service/       # [Spring Boot] Balance, Ledger (Hexagonal)
├── billing-service/      # [Quarkus]     Bill payments
├── notification-service/ # [Quarkus]     Push, SMS, Email
├── gateway-service/      # [Quarkus]     API Gateway
├── kyc-service/          # [FastAPI]     OCR, Liveness ML
└── analytics-service/    # [FastAPI]     Insights, TimescaleDB
```

### Service Ports
| Service | Port | Technology |
|---------|------|------------|
| Gateway | 8080 | Quarkus |
| Account | 8001 | Spring Boot |
| Auth | 8002 | Spring Boot |
| Transaction | 8003 | Spring Boot |
| Wallet | 8004 | Spring Boot |
| Billing | 8005 | Quarkus |
| Notification | 8006 | Quarkus |
| KYC | 9001 | FastAPI |
| Analytics | 9002 | FastAPI |

## Development Guidelines

### 1. Spring Boot (Core Banking)
**Focus**: Complex business logic, transactions, consistency.

```bash
# Workflow
./mvnw spring-boot:run   # Run locally
./mvnw test              # Run unit tests
./mvnw test -Dtest.excluded.groups=none  # Run integration tests
```

**Structure (Hexagonal/Clean Arch):**
- `domain/` : Entities, Ports, Logic (No Frameworks)
- `application/` : Use Cases, Sagas
- `infrastructure/` : Adapters (Persistence, Messaging, Web)
- `api/` : Controllers

### 2. Quarkus (Supporting Services)
**Focus**: High performance, simple CRUD, reactive.

```bash
# Workflow
./mvnw quarkus:dev       # Live coding
./mvnw package -Pnative  # Native build check
```

**Structure:** Standard Layered Architecture or Resource-Service-Repository.

### 3. Python FastAPI (ML/Data)
**Focus**: Data processing, ML inference.

```bash
# Workflow
uvicorn app.main:app --reload
pytest
```

## Specialized Skills

Untuk panduan mendalam di area spesifik, gunakan skill berikut:

- **QA & Testing**: Lihat `@qa-expert` untuk strategi testing, TDD, Testcontainers, dan performance metrics.
- **Security**: Lihat `@security-specialist` untuk standar keamanan, kepatuhan (compliance), dan secure coding.
- **Containerization**: Lihat `@container-specialist` untuk Dockerfile, OpenShift, dan UBI images.

## Architecture Patterns

### Event-Driven Architecture (Kafka)
PayU menggunakan Kafka untuk komunikasi asinkron antar service dan Saga Pattern.

**Topic Convention**: `payu.<domain>.<event-type>`

Example:
- `payu.transactions.initiated`
- `payu.wallet.balance.reserved`
- `payu.accounts.created`

### Database Design
- **One Service One Database**: Isolated schemas (`payu_accounts`, `payu_wallet`).
- **Migrations**: Gunakan **Flyway** (`V1__description.sql`).
- **Ledger**: Wallet service menggunakan Double-Entry Ledger pattern.

### Error Handling
Standardized error codes format: `DOMAIN_CATEGORY_DETAIL`
- `AUTH_VAL_INVALID_TOKEN` (401)
- `ACCT_BUS_INSUFFICIENT_FUNDS` (400)
- `SYS_INT_TIMEOUT` (504)

---
*For detailed code review, testing, or security tasks, verify you are using the appropriate specialist skill.*
