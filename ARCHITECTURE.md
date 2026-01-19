# PayU Digital Banking - Architecture Documentation

> Production-Ready Microservices Architecture for Digital Banking Platform

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Overview](#2-system-overview)
3. [Microservices Architecture](#3-microservices-architecture)
   - 3.4 [Testing Strategy](#34-testing-strategy)
4. [Event-Driven Architecture](#4-event-driven-architecture)
5. [Data Architecture](#5-data-architecture)
6. [Security Architecture](#6-security-architecture)
7. [API Gateway & Service Mesh](#7-api-gateway--service-mesh)
8. [Infrastructure & DevOps](#8-infrastructure--devops)
9. [Monitoring & Observability](#9-monitoring--observability)
10. [External Service Simulators](#10-external-service-simulators)
11. [Frontend Architecture](#11-frontend-architecture)
12. [TokoBapak Integration](#12-tokobapak-integration)
13. [Disaster Recovery & High Availability](#13-disaster-recovery--high-availability)
14. [Lab Configuration & Decisions](#14-lab-configuration--decisions)

---

## Lab Project Context

> **Note**: This is a **lab project** with a realistic production approach.
> External integrations (BI-FAST, Dukcapil, QRIS) will use **simulators** for development and testing.

## 1. Executive Summary

PayU adalah platform digital banking modern yang dibangun dengan arsitektur **microservices** dan **event-driven** untuk mencapai:

- **Scalability**: Horizontal scaling per service
- **Resilience**: Fault isolation dan self-healing
- **Security**: PCI DSS Level 1 & ISO 27001 compliant
- **Performance**: Sub-second transaction processing
- **Availability**: 99.95% uptime SLA

### Technology Stack Overview

| Layer | Red Hat Product | Portable Alternative |
|-------|-----------------|----------------------|
| **Container Platform** | Red Hat OpenShift 4.20+ | Kubernetes (EKS/GKE/AKS) |
| **Core Banking Services** | Red Hat Runtimes (Spring Boot 3.4) | Spring Boot |
| **Supporting Services** | Red Hat Build of Quarkus 3.x | Quarkus |
| **ML/Data Services** | Python 3.12 (UBI-based) | Python FastAPI |
| **API Gateway** | Red Hat Build of Quarkus | Any API Gateway |
| **Event Streaming** | AMQ Streams (Kafka) | Apache Kafka, Confluent |
| **Message Queue** | AMQ Broker (Artemis) | ActiveMQ Artemis, RabbitMQ |
| **Database** | Crunchy PostgreSQL 16 | Any PostgreSQL |
| **Caching** | Red Hat Data Grid (RESP mode) | Redis, ElastiCache |
| **Identity & Access** | Red Hat SSO (Keycloak) 24 | Keycloak, Auth0 |
| **Service Mesh** | OpenShift Service Mesh | Istio, Linkerd |
| **Logging** | OpenShift Logging (LokiStack) | Grafana Loki |
| **Monitoring** | OpenShift Monitoring | Prometheus/Grafana |
| **Tracing** | OpenShift Distributed Tracing | Jaeger, Tempo |
| **CI/CD** | OpenShift Pipelines + GitOps | Tekton + ArgoCD |

> **Portability Note**: All components use standard APIs (OIDC, RESP, Kafka Protocol, SQL, AMQP).
> Code remains portable - only configuration changes needed to switch providers.

### Polyglot Microservices Strategy

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                  RED HAT OPENSHIFT 4.20+ ECOSYSTEM                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  CORE BANKING (Red Hat Runtimes)       SUPPORTING (Red Hat Build of Quarkus)│
│  ┌───────────────────────────────┐    ┌───────────────────────────────┐     │
│  │ account-service               │    │ gateway-service               │     │
│  │ auth-service                  │    │ billing-service               │     │
│  │ transaction-service           │    │ notification-service          │     │
│  │ wallet-service                │    │ card-service                  │     │
│  │                               │    │ promotion-service             │     │
│  │ Spring Boot 3.4               │    │ Quarkus 3.x Native            │     │
│  │ ✓ Axon Framework (CQRS/ES)    │    │ ✓ <50ms startup, <50MB RAM    │     │
│  │ ✓ Strong ACID transactions    │    │ ✓ Redis-compatible (RESP)     │     │
│  └───────────────────────────────┘    └───────────────────────────────┘     │
│                                                                              │
│  ML/DATA (Python 3.12 UBI)            DATA LAYER                            │
│  ┌───────────────────────────────┐    ┌───────────────────────────────┐     │
│  │ kyc-service (OCR, liveness)   │    │ PostgreSQL 16 (JSONB)         │     │
│  │ analytics-service (ML)        │    │ Red Hat Data Grid (RESP)      │     │
│  │ recommendation-service        │    │ OpenShift Elasticsearch       │     │
│  └───────────────────────────────┘    └───────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. System Overview

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               EXTERNAL CLIENTS                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │  Mobile App  │  │   Web App    │  │  Admin Web   │  │  External Partners   │ │
│  │  (iOS/Android)│  │  (Next.js)  │  │  (Next.js)   │  │  (TokoBapak, etc)    │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘ │
└─────────┼──────────────────┼──────────────────┼────────────────────┼────────────┘
          │                  │                  │                    │
          └──────────────────┴────────┬─────────┴────────────────────┘
                                      │
                           ┌──────────▼──────────┐
                           │    Load Balancer    │
                           │  (AWS ALB / Nginx)  │
                           └──────────┬──────────┘
                                      │
                           ┌──────────▼──────────┐
                           │    WAF & DDoS       │
                           │   (AWS WAF/Shield)  │
                           └──────────┬──────────┘
                                      │
┌─────────────────────────────────────┼─────────────────────────────────────────┐
│                          API GATEWAY LAYER                                     │
│                    ┌────────────────▼────────────────┐                        │
│                    │    Spring Cloud Gateway          │                        │
│                    │    - Rate Limiting               │                        │
│                    │    - JWT Validation              │                        │
│                    │    - Request Routing             │                        │
│                    └────────────────┬────────────────┘                        │
└─────────────────────────────────────┼─────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼─────────────────────────────────────────┐
│                          SERVICE MESH (Istio)                                  │
│  ┌────────────┬────────────┬────────┴───────┬────────────┬────────────┐       │
│  │            │            │                │            │            │       │
│  ▼            ▼            ▼                ▼            ▼            ▼       │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│ │ Account  │ │   Auth   │ │Transaction│ │  Wallet  │ │ Billing  │ │   KYC    │ │
│ │ Service  │ │ Service  │ │ Service  │ │ Service  │ │ Service  │ │ Service  │ │
│ │(Spring)  │ │(Spring)  │ │ (Spring) │ │(Spring)  │ │(Quarkus) │ │(Python)  │ │
│ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
└──────┼────────────┼────────────┼────────────┼────────────┼────────────┼───────┘
       │            │            │            │            │            │
       └────────────┴────────────┴─────┬──────┴────────────┴────────────┘
                                       │
┌──────────────────────────────────────┼────────────────────────────────────────┐
│                    HYBRID MESSAGING (AMQ Streams + AMQ Broker)                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                      AMQ STREAMS (Kafka)                                 │  │
│  │  ┌─────────────────▼─────────────────┐                                   │  │
│  │  │  Event Sourcing & Saga            │  Topics:                          │  │
│  │  │  - Transaction events             │  - payu.transactions.*            │  │
│  │  │  - Audit logs                     │  - payu.accounts.*                │  │
│  │  │  - CDC (Debezium)                 │  - payu.wallet.*                  │  │
│  │  └───────────────────────────────────┘                                   │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                      AMQ BROKER (AMQP 1.0)                               │  │
│  │  ┌───────────────────────────────────┐                                   │  │
│  │  │  Point-to-Point Messaging         │  Queues:                          │  │
│  │  │  - Notification delivery          │  - notification.send              │  │
│  │  │  - External callbacks             │  - webhook.outbound               │  │
│  │  │  - Legacy integration             │  - legacy.bridge                  │  │
│  │  └───────────────────────────────────┘                                   │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┼────────────────────────────────────────┘
                                       │
┌──────────────────────────────────────┼────────────────────────────────────────┐
│                           DATA LAYER                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  PostgreSQL  │  │   MongoDB    │  │    Redis     │  │Elasticsearch │       │
│  │  (Primary)   │  │  (Activity)  │  │   (Cache)    │  │  (Search)    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘       │
└───────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Domain-Driven Design** | Services aligned with banking domains |
| **Database per Service** | Each service owns its data store |
| **Event Sourcing** | Complete audit trail for all transactions |
| **CQRS** | Separated read/write models for performance |
| **Saga Pattern** | Distributed transaction management |
| **Zero Trust** | mTLS between all services |

---

## 3. Microservices Architecture

### 3.1 Service Decomposition

```
                              ┌─────────────────────────────────────┐
                              │           CORE BANKING              │
                              └─────────────────────────────────────┘
                                             │
         ┌───────────────┬───────────────┬───┴───┬───────────────┬───────────────┐
         │               │               │       │               │               │
    ┌────▼────┐    ┌─────▼─────┐   ┌─────▼─────┐ ┌▼────────────┐ ┌▼────────────┐ │
    │ Account │    │   Auth    │   │Transaction│ │   Wallet    │ │  Billing    │ │
    │ Service │    │  Service  │   │  Service  │ │   Service   │ │  Service    │ │
    └─────────┘    └───────────┘   └───────────┘ └─────────────┘ └─────────────┘ │
                                                                                  │
                              ┌─────────────────────────────────────┐            │
                              │         SUPPORTING SERVICES         │            │
                              └─────────────────────────────────────┘            │
                                             │                                    │
    ┌───────────────┬───────────────┬────────┴────┬───────────────┬─────────────┘
    │               │               │             │               │
┌───▼───────┐ ┌─────▼─────┐ ┌───────▼─────┐ ┌─────▼─────┐ ┌───────▼─────┐
│    KYC    │ │Notification│ │  Analytics  │ │   Card    │ │  Promotion  │
│  Service  │ │  Service   │ │   Service   │ │  Service  │ │   Service   │
└───────────┘ └────────────┘ └─────────────┘ └───────────┘ └─────────────┘
```

### 3.2 Service Specifications

#### 3.2.1 Account Service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4.x |
| **Database** | PostgreSQL |
| **Port** | 8001 |
| **Responsibilities** | User accounts, multi-pocket, profile management |

```
account-service/
├── src/main/java/id/payu/account/
│   ├── AccountServiceApplication.java
│   ├── config/                     # Configuration classes
│   ├── domain/
│   │   ├── entity/                 # Account, Pocket, Profile
│   │   ├── event/                  # Domain events
│   │   ├── repository/             # Repository interfaces
│   │   └── service/                # Domain services
│   ├── application/
│   │   ├── command/                # CQRS commands
│   │   ├── query/                  # CQRS queries
│   │   └── saga/                   # Saga participants
│   ├── infrastructure/
│   │   ├── persistence/            # JPA implementations
│   │   ├── messaging/              # Kafka producers/consumers
│   │   └── external/               # External service clients
│   └── api/
│       ├── rest/                   # REST controllers
│       └── grpc/                   # gRPC services (internal)
└── src/main/resources/
    ├── application.yml
    └── db/migration/               # Flyway migrations
```

**Key APIs:**
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/accounts` | POST | Open new account |
| `/v1/accounts/{id}` | GET | Get account details |
| `/v1/accounts/{id}/pockets` | POST | Create savings pocket |
| `/v1/accounts/{id}/pockets` | GET | List all pockets |

---

#### 3.2.2 Auth Service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4.x, Keycloak |
| **Database** | PostgreSQL |
| **Port** | 8002 |
| **Responsibilities** | Authentication, MFA, OAuth2, session management |

**Security Features:**
- Biometric authentication (fingerprint, face ID)
- Device binding & trust management
- Transaction PIN with rate limiting
- Adaptive MFA based on risk score

---

#### 3.2.3 Transaction Service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4.x |
| **Database** | PostgreSQL + Event Store |
| **Port** | 8003 |
| **Responsibilities** | Transfer, BI-FAST, QRIS, payment processing |

**Transaction Types:**
| Type | Processing | SLA |
|------|------------|-----|
| Internal Transfer | Synchronous | < 1s |
| BI-FAST | Async (callback) | < 5s |
| QRIS Payment | Synchronous | < 3s |
| Bill Payment | Async (callback) | < 30s |

---

#### 3.2.4 Wallet Service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Spring Boot 3.4.x |
| **Database** | PostgreSQL (Double-entry ledger) |
| **Port** | 8004 |
| **Responsibilities** | Balance management, ledger, holds |

**Double-Entry Ledger Design:**
```sql
-- Ledger entries table
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL, -- DEBIT, CREDIT
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    balance_after DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT positive_amount CHECK (amount > 0)
);

-- Always balance: SUM(CREDIT) = SUM(DEBIT) per transaction
```

---

#### 3.2.5 KYC Service

| Attribute | Value |
|-----------|-------|
| **Technology** | Python 3.12, FastAPI (UBI-based) |
| **Database** | PostgreSQL (JSONB) |
| **Port** | 8005 |
| **Responsibilities** | eKYC, OCR, liveness detection, Dukcapil verification |

**ML Pipeline:**
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  KTP Image  │───▶│  OCR Model  │───▶│  Liveness   │───▶│  Dukcapil   │
│   Upload    │    │ (PyTorch)   │    │  Detection  │    │    API      │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                                                │
                                                                ▼
                                                         ┌─────────────┐
                                                         │  KYC Score  │
                                                         │  & Decision │
                                                         └─────────────┘
```

---

#### 3.2.6 Notification Service

| Attribute | Value |
|-----------|-------|
| **Technology** | Java 21, Quarkus 3.x (Native) |
| **Database** | PostgreSQL |
| **Cache** | Red Hat Data Grid (RESP mode) |
| **Messaging** | AMQ Broker (AMQP 1.0) |
| **Port** | 8006 |
| **Responsibilities** | Push notifications, SMS, Email, in-app messages |

**Notification Channels:**
| Channel | Provider | Use Case |
|---------|----------|----------|
| Push | Firebase FCM | Real-time alerts |
| SMS | Twilio / Local | OTP, critical alerts |
| Email | SendGrid | Statements, marketing |
| WhatsApp | Meta Business | Customer support |

---

### 3.3 Service Communication Matrix

| From → To | Protocol | Pattern |
|-----------|----------|---------|
| Gateway → Services | HTTP/REST | Sync |
| Service → Service (query) | gRPC | Sync |
| Service → Service (command) | Kafka | Async |
| Service → External | HTTP/REST | Async + Callback |

---

### 3.4 Testing Strategy

#### Testing Stack

| Tool | Purpose | Scope |
|------|---------|-------|
| **JUnit 5** | Unit testing framework | All Java services |
| **Mockito** | Mocking dependencies | Service layer tests |
| **Testcontainers** | Integration testing | PostgreSQL, Kafka |
| **ArchUnit** | Architecture rules | Layered architecture enforcement |
| **JaCoCo** | Code coverage | Coverage reporting |
| **Spring Security Test** | Security testing | Auth context mocking |

#### Test Types

| Type | Description | Tools |
|------|-------------|-------|
| **Unit Tests** | Isolated business logic | Mockito |
| **Controller Tests** | REST API endpoints | @WebMvcTest |
| **Architecture Tests** | Enforce layer dependencies | ArchUnit |
| **Integration Tests** | Full service with real DB | Testcontainers |

#### Test Structure (per service)

```
src/test/java/id/payu/<service>/
├── service/           # Unit tests with Mockito
├── controller/        # WebMvcTest for REST endpoints
├── architecture/      # ArchUnit rules enforcement
└── integration/       # Testcontainers-based tests
```

#### Test Commands

```bash
mvn test                # Run all tests
mvn test jacoco:report  # With coverage report
mvn test -Dtest=*Arch*  # Architecture tests only
```

#### Clean Architecture Decision

| Service Type | Architecture | Rationale |
|-------------|--------------|-----------|
| Core Banking (account, transaction, wallet) | Clean/Hexagonal | Complex domain, high testability needed |
| Supporting (notification, billing) | Layered | Simple CRUD, no over-engineering |
| ML Services (kyc, analytics) | Simplified Clean | Focus on ML logic isolation |

---

## 4. Event-Driven Architecture

### 4.1 Kafka Topic Design

```
payu.                              # Namespace prefix
├── accounts.                      # Account domain
│   ├── account-created            # Account lifecycle events
│   ├── account-updated
│   ├── pocket-created
│   └── pocket-balance-changed
├── transactions.                  # Transaction domain
│   ├── transaction-initiated      # Transaction saga events
│   ├── transaction-validated
│   ├── transaction-completed
│   └── transaction-failed
├── wallet.                        # Wallet domain
│   ├── balance-reserved           # Hold/release events
│   ├── balance-committed
│   └── balance-released
├── notifications.                 # Notification domain
│   ├── notification-requested     # Outbound notifications
│   └── notification-delivered
└── dlq.                          # Dead Letter Queues
    ├── transactions-dlq
    └── notifications-dlq
```

### 4.2 Saga Pattern - Transfer Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        TRANSFER SAGA ORCHESTRATION                           │
└─────────────────────────────────────────────────────────────────────────────┘

     User                Transaction        Wallet            Account      Notification
      │                    Service          Service           Service         Service
      │                       │                │                 │               │
      │  1. Transfer Request  │                │                 │               │
      ├──────────────────────▶│                │                 │               │
      │                       │                │                 │               │
      │                       │ 2. Reserve     │                 │               │
      │                       │    Balance     │                 │               │
      │                       ├───────────────▶│                 │               │
      │                       │                │                 │               │
      │                       │ 3. Reserved OK │                 │               │
      │                       │◀───────────────┤                 │               │
      │                       │                │                 │               │
      │                       │ 4. Validate Recipient            │               │
      │                       ├──────────────────────────────────▶               │
      │                       │                │                 │               │
      │                       │ 5. Recipient Valid               │               │
      │                       │◀──────────────────────────────────               │
      │                       │                │                 │               │
      │                       │ 6. Commit      │                 │               │
      │                       │    Transfer    │                 │               │
      │                       ├───────────────▶│                 │               │
      │                       │                │                 │               │
      │                       │ 7. Committed   │                 │               │
      │                       │◀───────────────┤                 │               │
      │                       │                │                 │               │
      │                       │ 8. Send Notification             │               │
      │                       ├───────────────────────────────────────────────────▶
      │                       │                │                 │               │
      │  9. Transfer Success  │                │                 │               │
      │◀──────────────────────┤                │                 │               │
      │                       │                │                 │               │
```

### 4.3 Compensating Transactions

```java
@Saga
public class TransferSaga {
    
    @StartSaga
    @SagaEventHandler(associationProperty = "transactionId")
    public void handle(TransferInitiatedEvent event) {
        // Step 1: Reserve balance from sender
        commandGateway.send(new ReserveBalanceCommand(
            event.getSenderId(),
            event.getAmount()
        ));
    }
    
    @SagaEventHandler(associationProperty = "transactionId")
    public void handle(BalanceReservedEvent event) {
        // Step 2: Credit to recipient
        commandGateway.send(new CreditAccountCommand(
            event.getRecipientId(),
            event.getAmount()
        ));
    }
    
    @SagaEventHandler(associationProperty = "transactionId")
    public void handle(BalanceReservationFailedEvent event) {
        // Compensation: No action needed (nothing committed yet)
        commandGateway.send(new FailTransactionCommand(
            event.getTransactionId(),
            "Insufficient balance"
        ));
        SagaLifecycle.end();
    }
    
    @SagaEventHandler(associationProperty = "transactionId")
    public void handle(CreditFailedEvent event) {
        // Compensation: Release reserved balance
        commandGateway.send(new ReleaseBalanceCommand(
            event.getSenderId(),
            event.getAmount()
        ));
    }
    
    @EndSaga
    @SagaEventHandler(associationProperty = "transactionId")
    public void handle(TransferCompletedEvent event) {
        // Success: Commit the reservation
        commandGateway.send(new CommitBalanceCommand(
            event.getSenderId(),
            event.getAmount()
        ));
    }
}
```

---

## 5. Data Architecture

### 5.1 Database Strategy

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│              UNIFIED POSTGRESQL + DATA GRID STRATEGY                        │
└─────────────────────────────────────────────────────────────────────────────┘

                        PRIMARY DATABASE (Crunchy PostgreSQL 16)
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Account Service │     │Transaction Svc  │     │  Wallet Service │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PostgreSQL    │     │   PostgreSQL    │     │   PostgreSQL    │
│  payu_accounts  │     │payu_transactions│     │   payu_wallet   │
│                 │     │  + Event Store  │     │  (Double-entry) │
└─────────────────┘     └─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   KYC Service   │     │Notification Svc │     │Analytics Service│
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PostgreSQL    │     │   PostgreSQL    │     │   TimescaleDB   │
│    payu_kyc     │     │payu_notification│     │  payu_analytics │
│    (JSONB)      │     │                 │     │  (Time-series)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘

                        CACHING LAYER (Red Hat Data Grid - RESP Mode)
                        ┌─────────────────────────────────────────────┐
                        │  • Session Store (TTL-based)            │
                        │  • Rate Limiting Counters                │
                        │  • Token Cache                           │
                        │  • Hot Data Cache (account balances)     │
                        │                                          │
                        │  Redis Protocol (RESP) = Portable Code   │
                        └─────────────────────────────────────────────┘
```

> **Portability**: All services use standard Redis clients (`spring-data-redis`, `quarkus-redis-client`).
> Can switch to AWS ElastiCache, Azure Cache, or plain Redis by changing configuration only.

### 5.2 CQRS Implementation

```
┌─────────────────────────────────────────────────────────────────┐
│                    CQRS PATTERN                                  │
└─────────────────────────────────────────────────────────────────┘

        COMMAND SIDE                          QUERY SIDE
    ┌───────────────────┐                ┌───────────────────┐
    │   Mobile/Web App  │                │   Mobile/Web App  │
    └─────────┬─────────┘                └─────────┬─────────┘
              │ POST/PUT/DELETE                    │ GET
              ▼                                    ▼
    ┌───────────────────┐                ┌───────────────────┐
    │  Command Handler  │                │   Query Handler   │
    └─────────┬─────────┘                └─────────┬─────────┘
              │                                    │
              ▼                                    ▼
    ┌───────────────────┐                ┌───────────────────┐
    │   Write Model     │───── CDC ─────▶│    Read Model     │
    │   (PostgreSQL)    │    (Debezium)  │    (Data Grid)    │
    └───────────────────┘                └───────────────────┘
              │
              ▼
    ┌───────────────────┐
    │   Event Store     │
    │   (PostgreSQL)    │
    └───────────────────┘
```

### 5.3 Event Store Schema

```sql
-- Event Store for Event Sourcing
CREATE TABLE event_store (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,
    metadata JSONB,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT unique_aggregate_version 
        UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_event_store_aggregate 
    ON event_store(aggregate_id, version);
CREATE INDEX idx_event_store_type 
    ON event_store(event_type, created_at);

-- Snapshot store for performance
CREATE TABLE event_snapshots (
    aggregate_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    state JSONB NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### 5.4 Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CORE BANKING ERD                                    │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│     users       │         │    accounts     │         │     pockets     │
├─────────────────┤         ├─────────────────┤         ├─────────────────┤
│ id         (PK) │         │ id         (PK) │◄────────│ id         (PK) │
│ phone_number    │◄────────│ user_id    (FK) │         │ account_id (FK) │
│ email           │         │ account_number  │         │ name            │
│ full_name       │         │ account_type    │         │ target_amount   │
│ nik             │         │ status          │         │ current_balance │
│ kyc_status      │         │ tier            │         │ target_date     │
│ created_at      │         │ created_at      │         │ is_locked       │
│ updated_at      │         │ updated_at      │         │ created_at      │
└─────────────────┘         └────────┬────────┘         └─────────────────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
         ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
         │   transactions  │ │  ledger_entries │ │  virtual_cards  │
         ├─────────────────┤ ├─────────────────┤ ├─────────────────┤
         │ id         (PK) │ │ id         (PK) │ │ id         (PK) │
         │ account_id (FK) │ │ account_id (FK) │ │ account_id (FK) │
         │ type            │ │ transaction_id  │ │ card_number     │
         │ amount          │ │ entry_type      │ │ cvv_encrypted   │
         │ currency        │ │ amount          │ │ expiry_date     │
         │ reference_id    │ │ balance_after   │ │ spending_limit  │
         │ status          │ │ created_at      │ │ is_active       │
         │ metadata        │ └─────────────────┘ │ created_at      │
         │ created_at      │                     └─────────────────┘
         └─────────────────┘
```

---

## 6. Security Architecture

### 6.1 Security Layers

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SECURITY ARCHITECTURE                               │
└─────────────────────────────────────────────────────────────────────────────┘

Layer 1: PERIMETER SECURITY
├── AWS WAF (Web Application Firewall)
├── AWS Shield (DDoS Protection)
├── CloudFlare (CDN + Bot Protection)
└── Rate Limiting (per IP, per user)

Layer 2: NETWORK SECURITY
├── VPC with Private Subnets
├── Security Groups (whitelist)
├── Network ACLs
└── VPN for internal access

Layer 3: APPLICATION SECURITY
├── mTLS (Service Mesh - Istio)
├── OAuth2 / OIDC (Keycloak)
├── JWT with short expiry (15 min)
└── CORS strict policy

Layer 4: DATA SECURITY
├── Encryption at rest (AES-256)
├── Encryption in transit (TLS 1.3)
├── Field-level encryption (PII)
└── HSM for key management

Layer 5: COMPLIANCE
├── PCI DSS Level 1
├── ISO 27001
├── GDPR / UU PDP
└── OJK Regulations
```

### 6.2 Authentication Flow

```
┌──────────┐                    ┌──────────┐                    ┌──────────┐
│  Client  │                    │ Keycloak │                    │  Service │
└────┬─────┘                    └────┬─────┘                    └────┬─────┘
     │                               │                               │
     │  1. Login (phone + PIN)       │                               │
     ├──────────────────────────────▶│                               │
     │                               │                               │
     │  2. Challenge (OTP/Biometric) │                               │
     │◀──────────────────────────────┤                               │
     │                               │                               │
     │  3. OTP/Biometric Response    │                               │
     ├──────────────────────────────▶│                               │
     │                               │                               │
     │  4. Access Token + Refresh    │                               │
     │◀──────────────────────────────┤                               │
     │                               │                               │
     │  5. API Request + Bearer Token│                               │
     ├───────────────────────────────┼──────────────────────────────▶│
     │                               │                               │
     │                               │  6. Validate Token (cached)   │
     │                               │◀──────────────────────────────┤
     │                               │                               │
     │  7. Response                  │                               │
     │◀──────────────────────────────┼───────────────────────────────┤
     │                               │                               │
```

### 6.3 Transaction Security

| Control | Implementation |
|---------|----------------|
| **Transaction PIN** | 6-digit PIN, 3 attempts before lock |
| **Transaction Limit** | Daily/monthly limits per tier |
| **Device Binding** | Max 2 devices per account |
| **Fraud Detection** | ML model (velocity, geo, behavior) |
| **3D Secure** | For card transactions |
| **Idempotency** | UUID-based request deduplication |

### 6.4 Encryption Standards

```yaml
# Encryption Configuration
encryption:
  at_rest:
    algorithm: AES-256-GCM
    key_management: AWS KMS
    
  in_transit:
    protocol: TLS 1.3
    cipher_suites:
      - TLS_AES_256_GCM_SHA384
      - TLS_CHACHA20_POLY1305_SHA256
      
  field_level:
    pii_fields:
      - nik
      - phone_number
      - email
    algorithm: AES-256-GCM
    key_rotation: 90 days
```

---

## 7. API Gateway & Service Mesh

### 7.1 Spring Cloud Gateway Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: account-service
          uri: lb://account-service
          predicates:
            - Path=/v1/accounts/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
            - name: CircuitBreaker
              args:
                name: accountServiceCB
                fallbackUri: forward:/fallback/account
                
        - id: transaction-service
          uri: lb://transaction-service
          predicates:
            - Path=/v1/transactions/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
                
      default-filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE
        - AddRequestHeader=X-Request-ID, ${random.uuid}
```

### 7.2 Istio Service Mesh

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: payu
spec:
  mtls:
    mode: STRICT
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: circuit-breaker
  namespace: payu
spec:
  host: "*.payu.svc.cluster.local"
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

---

## 8. Infrastructure & DevOps

### 8.1 Kubernetes Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AWS EKS CLUSTER (Multi-AZ)                            │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                        NAMESPACE: payu-production                       │ │
│  │                                                                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │ │
│  │  │  account    │  │transaction  │  │   wallet    │  │    auth     │   │ │
│  │  │  service    │  │  service    │  │   service   │  │   service   │   │ │
│  │  │ replicas: 3 │  │ replicas: 5 │  │ replicas: 3 │  │ replicas: 3 │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │ │
│  │                                                                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │ │
│  │  │   billing   │  │     kyc     │  │notification │  │  analytics  │   │ │
│  │  │   service   │  │   service   │  │   service   │  │   service   │   │ │
│  │  │ replicas: 2 │  │ replicas: 2 │  │ replicas: 3 │  │ replicas: 2 │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │ │
│  │                                                                         │ │
│  │  ┌───────────────────────────────────────────────────────────────────┐ │ │
│  │  │                    Istio Ingress Gateway                           │ │ │
│  │  └───────────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                     NAMESPACE: payu-infrastructure                      │ │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   │ │
│  │  │ Kafka  │ │ Redis  │ │Keycloak│ │Grafana │ │Prometheus│ │ Jaeger │   │ │
│  │  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘   │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.2 Helm Chart Structure

```
payu-helm/
├── charts/
│   ├── account-service/
│   │   ├── Chart.yaml
│   │   ├── values.yaml
│   │   └── templates/
│   │       ├── deployment.yaml
│   │       ├── service.yaml
│   │       ├── hpa.yaml
│   │       ├── configmap.yaml
│   │       └── secret.yaml
│   ├── transaction-service/
│   ├── wallet-service/
│   └── ...
├── values/
│   ├── production.yaml
│   ├── staging.yaml
│   └── development.yaml
└── Chart.yaml
```

### 8.3 CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Tests
        run: ./mvnw test
      - name: SonarQube Analysis
        run: ./mvnw sonar:sonar
        
  security-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Trivy Container Scan
        uses: aquasecurity/trivy-action@master
      - name: OWASP Dependency Check
        uses: dependency-check/gh-action@main
        
  build:
    needs: [test, security-scan]
    runs-on: ubuntu-latest
    steps:
      - name: Build & Push Image
        run: |
          docker build -t payu/${{ matrix.service }}:${{ github.sha }} .
          docker push payu/${{ matrix.service }}:${{ github.sha }}
          
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to EKS
        run: |
          helm upgrade --install ${{ matrix.service }} \
            ./charts/${{ matrix.service }} \
            -f ./values/production.yaml \
            --set image.tag=${{ github.sha }}
```

---

## 9. Monitoring & Observability

### 9.1 Observability Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         OBSERVABILITY ARCHITECTURE                           │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌────────────────────────────────────┐
                    │            Grafana                 │
                    │    (Unified Dashboard)             │
                    └───────────────┬────────────────────┘
                                    │
           ┌────────────────────────┼────────────────────────┐
           │                        │                        │
           ▼                        ▼                        ▼
┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│    Prometheus       │  │    Elasticsearch    │  │      Jaeger         │
│    (Metrics)        │  │    (Logs via ELK)   │  │    (Traces)         │
└──────────┬──────────┘  └──────────┬──────────┘  └──────────┬──────────┘
           │                        │                        │
           │                        │                        │
           └────────────────────────┼────────────────────────┘
                                    │
                    ┌───────────────▼───────────────┐
                    │        Service Mesh           │
                    │     (Istio + Envoy Proxy)     │
                    └───────────────────────────────┘
```

### 9.2 Key Metrics & SLIs

| Metric | SLI | SLO |
|--------|-----|-----|
| **Availability** | Successful requests / Total requests | 99.95% |
| **Latency (p99)** | Request duration at 99th percentile | < 500ms |
| **Error Rate** | 5xx errors / Total requests | < 0.1% |
| **Throughput** | Transactions per second | > 1000 TPS |

### 9.3 Alerting Rules

```yaml
# Prometheus Alerting Rules
groups:
  - name: payu-critical
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_requests_total{status=~"5.."}[5m])) /
          sum(rate(http_requests_total[5m])) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          
      - alert: TransactionLatencyHigh
        expr: |
          histogram_quantile(0.99, 
            rate(transaction_duration_seconds_bucket[5m])) > 3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Transaction latency p99 > 3s"
          
      - alert: KafkaConsumerLag
        expr: kafka_consumer_lag > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag high"
```

---

## 10. TokoBapak Integration

### 10.1 Integration Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      TOKOBAPAK ↔ PAYU INTEGRATION                            │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐           ┌─────────────────────────────┐
│         TOKOBAPAK           │           │            PAYU             │
│                             │           │                             │
│  ┌───────────────────────┐  │           │  ┌───────────────────────┐  │
│  │    payment-service    │  │   HTTPS   │  │  Integration Gateway  │  │
│  │    (Spring Boot)      │◄─┼───────────┼─▶│   (Rate Limited)      │  │
│  └───────────┬───────────┘  │           │  └───────────┬───────────┘  │
│              │              │           │              │              │
│              │              │           │              ▼              │
│              │              │           │  ┌───────────────────────┐  │
│              │              │           │  │  Transaction Service  │  │
│              │              │           │  └───────────────────────┘  │
│              │              │           │                             │
│              │              │  Webhook  │                             │
│              │◄─────────────┼───────────┼─────Callback────────────────│
│              │              │           │                             │
│              ▼              │           │                             │
│  ┌───────────────────────┐  │           │                             │
│  │   Order Service       │  │           │                             │
│  └───────────────────────┘  │           │                             │
└─────────────────────────────┘           └─────────────────────────────┘
```

### 10.2 API Specification

#### Authentication
```http
POST /v1/partner/auth/token
Content-Type: application/json

{
  "client_id": "tokobapak_merchant_id",
  "client_secret": "xxxxx",
  "grant_type": "client_credentials"
}

Response:
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

#### Create Payment

```http
POST /v1/partner/payments
Authorization: Bearer {access_token}
X-Idempotency-Key: {unique_request_id}
Content-Type: application/json

{
  "merchant_reference": "TOKOBAPAK-ORD-12345",
  "amount": {
    "value": 150000,
    "currency": "IDR"
  },
  "customer": {
    "external_id": "user_12345",
    "phone": "+6281234567890",
    "email": "customer@email.com"
  },
  "payment_method": "PAYU_BALANCE",
  "description": "Payment for Order #12345",
  "callback_url": "https://api.tokobapak.id/webhooks/payu",
  "redirect_url": "https://tokobapak.id/orders/12345/status",
  "metadata": {
    "order_id": "12345",
    "items": ["Product A", "Product B"]
  }
}

Response:
{
  "payment_id": "pay_abc123xyz",
  "status": "PENDING",
  "payment_url": "https://pay.payu.id/checkout/pay_abc123xyz",
  "expires_at": "2026-01-18T21:00:00Z"
}
```

#### Payment Callback (Webhook)

```http
POST https://api.tokobapak.id/webhooks/payu
X-Payu-Signature: sha256=xxxxx
Content-Type: application/json

{
  "event_type": "payment.completed",
  "payment_id": "pay_abc123xyz",
  "merchant_reference": "TOKOBAPAK-ORD-12345",
  "status": "COMPLETED",
  "amount": {
    "value": 150000,
    "currency": "IDR"
  },
  "paid_at": "2026-01-18T20:15:30Z",
  "transaction_id": "txn_xyz789"
}
```

### 10.3 Integration with payment-service

Update TokoBapak's `payment-service` to integrate with PayU:

```java
// PayU Client Configuration
@Configuration
public class PayuClientConfig {
    
    @Bean
    public PayuClient payuClient(
        @Value("${payu.base-url}") String baseUrl,
        @Value("${payu.client-id}") String clientId,
        @Value("${payu.client-secret}") String clientSecret
    ) {
        return PayuClient.builder()
            .baseUrl(baseUrl)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }
}

// PayU Payment Provider Implementation
@Service
@RequiredArgsConstructor
public class PayuPaymentProvider implements PaymentProvider {
    
    private final PayuClient payuClient;
    private final StreamBridge streamBridge;
    
    @Override
    public PaymentResult processPayment(ProcessPaymentRequest request) {
        // Create payment request to PayU
        PayuPaymentRequest payuRequest = PayuPaymentRequest.builder()
            .merchantReference(request.getOrderId())
            .amount(new Amount(request.getAmount(), "IDR"))
            .customer(mapCustomer(request))
            .paymentMethod("PAYU_BALANCE")
            .callbackUrl(webhookUrl)
            .build();
            
        PayuPaymentResponse response = payuClient.createPayment(payuRequest);
        
        return PaymentResult.builder()
            .paymentId(response.getPaymentId())
            .status(PaymentStatus.PENDING)
            .paymentUrl(response.getPaymentUrl())
            .build();
    }
    
    // Webhook handler for PayU callbacks
    @PostMapping("/webhooks/payu")
    public ResponseEntity<Void> handlePayuWebhook(
        @RequestHeader("X-Payu-Signature") String signature,
        @RequestBody PayuWebhookEvent event
    ) {
        // Verify signature
        if (!payuClient.verifySignature(signature, event)) {
            return ResponseEntity.status(401).build();
        }
        
        // Publish event to Kafka
        PaymentProcessedEvent processedEvent = PaymentProcessedEvent.builder()
            .paymentId(event.getPaymentId())
            .orderId(event.getMerchantReference())
            .status(mapStatus(event.getStatus()))
            .transactionId(event.getTransactionId())
            .amount(event.getAmount().getValue())
            .build();
            
        streamBridge.send("paymentEvents-out-0", processedEvent);
        
        return ResponseEntity.ok().build();
    }
}
```

### 10.4 SDK Design (Optional)

```xml
<!-- Maven Dependency -->
<dependency>
    <groupId>id.payu</groupId>
    <artifactId>payu-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
// Usage Example
PayuClient payu = PayuClient.builder()
    .apiKey("pk_live_xxxxx")
    .secretKey("sk_live_xxxxx")
    .build();

// Create payment
Payment payment = payu.payments().create(
    CreatePaymentRequest.builder()
        .merchantReference("ORDER-123")
        .amount(150000L)
        .currency("IDR")
        .customerPhone("+6281234567890")
        .build()
);

// Check status
Payment status = payu.payments().get(payment.getId());
```

---

## 11. Disaster Recovery & High Availability

### 11.1 Multi-AZ Deployment

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        AWS REGION: ap-southeast-1                            │
│                                                                              │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐           │
│  │      Availability Zone A     │  │      Availability Zone B     │           │
│  │                              │  │                              │           │
│  │  ┌────────────────────────┐ │  │  ┌────────────────────────┐ │           │
│  │  │    EKS Node Group      │ │  │  │    EKS Node Group      │ │           │
│  │  │    (3 nodes)           │ │  │  │    (3 nodes)           │ │           │
│  │  └────────────────────────┘ │  │  └────────────────────────┘ │           │
│  │                              │  │                              │           │
│  │  ┌────────────────────────┐ │  │  ┌────────────────────────┐ │           │
│  │  │  PostgreSQL Primary    │ │  │  │  PostgreSQL Standby    │ │           │
│  │  │  (RDS Multi-AZ)        │◄┼──┼─▶│  (Sync Replication)    │ │           │
│  │  └────────────────────────┘ │  │  └────────────────────────┘ │           │
│  │                              │  │                              │           │
│  │  ┌────────────────────────┐ │  │  ┌────────────────────────┐ │           │
│  │  │  Redis Primary         │ │  │  │  Redis Replica         │ │           │
│  │  │  (ElastiCache)         │◄┼──┼─▶│  (ElastiCache)         │ │           │
│  │  └────────────────────────┘ │  │  └────────────────────────┘ │           │
│  └─────────────────────────────┘  └─────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 11.2 Recovery Objectives

| Metric | Target |
|--------|--------|
| **RTO** (Recovery Time Objective) | < 15 minutes |
| **RPO** (Recovery Point Objective) | < 1 minute |
| **Backup Frequency** | Continuous + Daily snapshots |
| **Backup Retention** | 30 days (7 years for compliance) |

### 11.3 Backup Strategy

| Data Type | Backup Method | Frequency | Retention |
|-----------|---------------|-----------|-----------|
| PostgreSQL | RDS Automated | Continuous | 7 days |
| PostgreSQL | Manual Snapshots | Weekly | 1 year |
| Data Grid | Cluster backup | Daily | 7 days |
| Kafka | Log retention | Continuous | 7 days |
| S3 (Documents) | Cross-region replication | Real-time | Compliance-based |

---

## 12. External Service Simulators

> For lab/development environment, external banking integrations use simulators.

### 12.1 BI-FAST Simulator

```text
bi-fast-simulator (Quarkus Native)
├── POST /api/v1/inquiry          → Account inquiry (name, bank)
├── POST /api/v1/transfer         → Initiate transfer
├── GET  /api/v1/status/{ref}     → Check transfer status
└── POST /webhook/callback        → Async notification

Features:
• Configurable network latency (50-500ms)
• Random failure simulation (5% default)
• Test bank accounts database
• Webhook callback simulation
```

**Test Bank Accounts:**

| Bank Code | Account Number | Name | Status |
|-----------|----------------|------|--------|
| BCA | 1234567890 | John Doe | Active |
| BRI | 0987654321 | Jane Doe | Active |
| MANDIRI | 1111222233 | Test Blocked | Blocked |
| BNI | 9999888877 | Test Timeout | Timeout |

### 12.2 Dukcapil Simulator

```text
dukcapil-simulator (Quarkus Native)
├── POST /api/v1/verify           → NIK verification
├── POST /api/v1/match-photo      → Face matching (KTP vs Selfie)
└── GET  /api/v1/nik/{nik}        → Get citizen data

Features:
• Test NIK database (valid/invalid)
• Configurable match scores (0-100%)
• Various error scenarios
• Photo similarity simulation
```

**Test NIK Database:**

| NIK | Name | Status | Match Score |
|-----|------|--------|-------------|
| 3201234567890001 | JOHN DOE | Valid | 95% |
| 3201234567890002 | JANE DOE | Valid | 88% |
| 3201234567890003 | BLOCKED USER | Blocked | N/A |
| 3299999999999999 | INVALID NIK | Invalid | N/A |

### 12.3 QRIS Simulator

```text
qris-simulator (Quarkus Native)
├── POST /api/v1/generate         → Generate QR code
├── POST /api/v1/pay              → Simulate payment
├── GET  /api/v1/status/{id}      → Check payment status
└── POST /webhook/callback        → Payment notification

Features:
• QR code generation (PNG/Base64)
• Payment simulation with configurable delay
• Expiry handling (5 min default)
• Multiple merchant simulation
```

---

## 13. Frontend Architecture

### 13.1 Technology Stack

| Platform | Technology | Purpose |
|----------|------------|---------|
| **Web App** | Next.js 15 + Tailwind CSS 4 | Customer portal |
| **Admin Dashboard** | Next.js 15 + shadcn/ui | Internal ops |
| **Mobile App** | Expo (React Native) | iOS/Android/Web |
| **Shared** | TypeScript, Zustand, TanStack Query | Cross-platform |

### 13.2 Architecture

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND ARCHITECTURE                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                        SHARED LAYER                                      ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    ││
│  │  │  API Client │  │   Stores    │  │    Types    │  │ Validation  │    ││
│  │  │ (TanStack)  │  │  (Zustand)  │  │(TypeScript) │  │   (Zod)     │    ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │    WEB APP       │  │  ADMIN DASHBOARD │  │   MOBILE APP     │          │
│  │   (Next.js 15)   │  │   (Next.js 15)   │  │    (Expo)        │          │
│  │                  │  │                  │  │                  │          │
│  │ • SSR/SSG        │  │ • Role-based UI  │  │ • iOS/Android    │          │
│  │ • App Router     │  │ • Data tables    │  │ • Web preview    │          │
│  │ • Tailwind CSS 4 │  │ • Charts         │  │ • Push notif     │          │
│  │ • shadcn/ui      │  │ • shadcn/ui      │  │ • Biometrics     │          │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘          │
│           │                    │                    │                       │
│           └────────────────────┴────────────────────┘                       │
│                                │                                            │
│                    ┌───────────▼───────────┐                               │
│                    │     API Gateway       │                               │
│                    │   (gateway-service)   │                               │
│                    └───────────────────────┘                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 13.3 Mobile Development Workflow

```text
Daily Development (95% of time):
┌─────────────────────────────────────────┐
│ 1. Expo Web (Browser)                   │
│    $ cd mobile && bun run web           │
│    → Opens http://localhost:8081        │
│    → Instant preview, hot reload        │
│                                         │
│ 2. Expo Go (Real Android Phone)         │
│    $ cd mobile && bun run start         │
│    → Scan QR code with Expo Go app      │
│    → Test on real device                │
└─────────────────────────────────────────┘

Testing Native Features (5% of time):
┌─────────────────────────────────────────┐
│ 3. Android Studio Emulator              │
│    $ cd mobile && bun run android       │
│    → Camera, biometrics, etc.           │
│                                         │
│ 4. EAS Build (Production-like)          │
│    $ eas build --platform android       │
│    → Download APK, install on device    │
└─────────────────────────────────────────┘
```

---

## 14. Lab Configuration & Decisions

### 14.1 Environment Strategy

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                    5 ENVIRONMENT STRATEGY (Banking Standard)                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   DEV ────► SIT ────► UAT ────► PREPROD ────► PROD                          │
│                                                                              │
│   ┌─────┐   ┌─────┐   ┌─────┐   ┌─────────┐   ┌─────────┐                  │
│   │ DEV │   │ SIT │   │ UAT │   │ PREPROD │   │  PROD   │                  │
│   └──┬──┘   └──┬──┘   └──┬──┘   └────┬────┘   └────┬────┘                  │
│      │         │         │           │             │                        │
│      ▼         ▼         ▼           ▼             ▼                        │
│   Fake      Synthetic  Anonymized  Prod-like    Real                        │
│   Data      Data       Real-like   Volume       Data                        │
│                                                                              │
│   Smallest   Small     Medium     SAME AS      Full                         │
│   Infra      Infra     Infra      PROD         Scale                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

| Environment | Purpose | Data | OpenShift Namespace |
|-------------|---------|------|---------------------|
| **DEV** | Daily development | Fake/minimal | `payu-dev` |
| **SIT** | Integration testing | Synthetic | `payu-sit` |
| **UAT** | Business validation | Anonymized | `payu-uat` |
| **PREPROD** | Production rehearsal | Prod copy | `payu-preprod` |
| **PROD** | Live production | Real | `payu-prod` |

### 14.2 Infrastructure Decisions

| Component | Decision | Notes |
|-----------|----------|-------|
| **Cloud Provider** | AWS | Region: ap-southeast-1 |
| **Cluster** | Single cluster, Multi-AZ | Cost-effective for lab |
| **Platform** | Red Hat OpenShift 4.20+ | Full ecosystem |
| **PostgreSQL** | AWS RDS (primary) | + Crunchy Operator for learning |
| **Object Storage** | OpenShift Data Foundation + S3 | ODF for persistence |
| **Backup Retention** | 7 days (lab) | Extend for production |

### 14.3 Security Tools

| Category | Tool | Purpose |
|----------|------|---------|
| **Key Management** | HashiCorp Vault | Secrets, PKI, transit encryption |
| **Container Security** | RHACS (OpenShift ACS) | Image scanning, runtime |
| **Runtime Security** | Falco | Container runtime threats |
| **SIEM** | Wazuh | Security monitoring, compliance |
| **Alerting** | AlertManager → Gmail | Email notifications |

### 14.4 External Service Strategy

| Service | Strategy | Provider |
|---------|----------|----------|
| **BI-FAST** | Simulator (Quarkus) | Self-built |
| **Dukcapil** | Simulator (Quarkus) | Self-built |
| **QRIS** | Simulator + Sandbox | Self-built + Xendit |
| **SMS OTP** | Telesign (500 free) | telesign.com |
| **Push Notification** | Firebase FCM | Free unlimited |

### 14.5 Rate Limiting Configuration

| Endpoint Category | RPS/User | Burst | Purpose |
|-------------------|----------|-------|---------|
| **Authentication** | 5/min | 10 | Brute force protection |
| **OTP Request** | 3/min | 5 | SMS cost control |
| **Transfer** | 10/min | 20 | Transaction protection |
| **Balance Inquiry** | 30/min | 50 | High-frequency reads |
| **Public API** | 100/IP/min | 200 | General protection |
| **Partner API** | 1000/min | 2000 | B2B high volume |

### 14.6 User Onboarding Flow (Target: 2-3 minutes)

| Step | Action | Target Time |
|------|--------|-------------|
| 1 | Phone number input | 5 sec |
| 2 | OTP verification (4-digit) | 15 sec |
| 3 | KTP photo capture (AI-guided) | 20 sec |
| 4 | Selfie with liveness | 15 sec |
| 5 | Data confirmation (OCR pre-filled) | 30 sec |
| 6 | PIN setup (6-digit) | 15 sec |
| 7 | Biometric setup (optional) | 5 sec |
| **Total** | | **~2 minutes** |

### 14.7 Implementation Phases

```text
Phase 1: Foundation (Infrastructure)
├── 1. OpenShift cluster setup + namespaces (5 envs)
├── 2. PostgreSQL (RDS) + Data Grid deployment
├── 3. AMQ Streams (Kafka) deployment
├── 4. Red Hat SSO (Keycloak) setup
├── 5. HashiCorp Vault deployment
├── 6. Wazuh + Falco setup
└── 7. CI/CD pipeline (OpenShift Pipelines + GitOps)

Phase 2: Simulators & Gateway
├── 1. bi-fast-simulator
├── 2. dukcapil-simulator
├── 3. qris-simulator
└── 4. gateway-service (API Gateway)

Phase 3: Core Banking Services
├── 1. account-service (user registration, profile)
├── 2. auth-service (login, MFA, session)
├── 3. wallet-service (balance, ledger)
└── 4. transaction-service (transfer, payment)

Phase 4: Supporting Services
├── 1. kyc-service (eKYC flow, ML)
├── 2. notification-service (OTP, alerts)
├── 3. billing-service (bills, top-up)
└── 4. analytics-service (insights)

Phase 5: Frontend Applications
├── 1. Web App (Next.js 15)
├── 2. Mobile App (Expo)
└── 3. Admin Dashboard (Next.js 15)

Phase 6: Integration (Later)
├── 1. TokoBapak Partner API
├── 2. Real BI-FAST integration
└── 3. Real QRIS integration
```

---

## Appendix

### A. Technology Versions

| Component | Version |
|-----------|---------|
| Java | 21 LTS |
| Spring Boot | 3.4.x |
| Quarkus | 3.17.x |
| Python | 3.12 |
| FastAPI | 0.115.x |
| Next.js | 15.x |
| Expo | 52.x |
| Kafka | 3.7.x |
| PostgreSQL | 16.x |
| Redis/Data Grid | 7.x / 8.x |
| OpenShift | 4.20+ |
| Istio | 1.23.x |

### B. Compliance Checklist

- [ ] PCI DSS Level 1 Certification
- [ ] ISO 27001 Certification
- [ ] SOC 2 Type II Report
- [ ] OJK Digital Banking License
- [ ] BI-FAST Participation
- [ ] QRIS Certification
- [ ] Penetration Test (Annual)
- [ ] Security Audit (Quarterly)

### C. References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Quarkus Documentation](https://quarkus.io/guides/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Red Hat OpenShift Documentation](https://docs.openshift.com/)
- [Expo Documentation](https://docs.expo.dev/)
- [Bank Indonesia BI-FAST](https://www.bi.go.id/id/fungsi-utama/sistem-pembayaran/bi-fast/default.aspx)
- [OJK Digital Banking Guidelines](https://www.ojk.go.id/)

---

**Document Version**: 2.0  
**Last Updated**: January 2026  
**Owner**: Engineering Team PayU  
**Status**: Lab Configuration Ready
