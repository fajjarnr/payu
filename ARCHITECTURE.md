# PayU Digital Banking - Architecture Documentation

> Production-Ready Microservices Architecture for Digital Banking Platform

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Overview](#2-system-overview)
3. [Microservices Architecture](#3-microservices-architecture)
4. [Event-Driven Architecture](#4-event-driven-architecture)
5. [Data Architecture](#5-data-architecture)
6. [Security Architecture](#6-security-architecture)
7. [API Gateway & Service Mesh](#7-api-gateway--service-mesh)
8. [Infrastructure & DevOps](#8-infrastructure--devops)
9. [Monitoring & Observability](#9-monitoring--observability)
10. [TokoBapak Integration](#10-tokobapak-integration)
11. [Disaster Recovery & High Availability](#11-disaster-recovery--high-availability)

---

## 1. Executive Summary

PayU adalah platform digital banking modern yang dibangun dengan arsitektur **microservices** dan **event-driven** untuk mencapai:

- **Scalability**: Horizontal scaling per service
- **Resilience**: Fault isolation dan self-healing
- **Security**: PCI DSS Level 1 & ISO 27001 compliant
- **Performance**: Sub-second transaction processing
- **Availability**: 99.95% uptime SLA

### Technology Stack Overview

| Layer | Technology |
|-------|------------|
| **Backend Services** | Java 21 (Spring Boot 3.4.x), Python 3.12 (FastAPI) |
| **API Gateway** | Spring Cloud Gateway |
| **Message Broker** | Apache Kafka 3.x |
| **Databases** | PostgreSQL 16, MongoDB 7, Redis 7 |
| **Identity & Access** | Keycloak 24 |
| **Container Runtime** | Kubernetes (EKS) |
| **Service Mesh** | Istio |
| **Observability** | Prometheus, Grafana, Jaeger, ELK |

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
│ │ (Java)   │ │ (Java)   │ │ (Java)   │ │ (Java)   │ │ (Java)   │ │(Python)  │ │
│ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │
└──────┼────────────┼────────────┼────────────┼────────────┼────────────┼───────┘
       │            │            │            │            │            │
       └────────────┴────────────┴─────┬──────┴────────────┴────────────┘
                                       │
┌──────────────────────────────────────┼────────────────────────────────────────┐
│                           EVENT BUS (Kafka)                                    │
│                    ┌─────────────────▼─────────────────┐                      │
│                    │     Apache Kafka Cluster          │                      │
│                    │     - Event Sourcing              │                      │
│                    │     - Saga Orchestration          │                      │
│                    │     - CDC (Debezium)              │                      │
│                    └─────────────────┬─────────────────┘                      │
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
| **Technology** | Python 3.12, FastAPI |
| **Database** | MongoDB |
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
| **Technology** | NestJS (TypeScript) |
| **Database** | MongoDB |
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

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DATABASE PER SERVICE                               │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Account Service │     │Transaction Svc  │     │  Wallet Service │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PostgreSQL    │     │   PostgreSQL    │     │   PostgreSQL    │
│  payu_accounts  │     │ payu_transactions│    │   payu_wallet   │
│                 │     │  + Event Store  │     │  (Double-entry) │
└─────────────────┘     └─────────────────┘     └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   KYC Service   │     │Notification Svc │     │Analytics Service│
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    MongoDB      │     │    MongoDB      │     │   ClickHouse    │
│    payu_kyc     │     │payu_notifications│    │  payu_analytics │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

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
    │   (PostgreSQL)    │    (Debezium)  │    (MongoDB)      │
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
| PostgreSQL | RDS Automated | Continuous | 35 days |
| PostgreSQL | Manual Snapshots | Daily | 7 years |
| MongoDB | Atlas Backup | Continuous | 30 days |
| Redis | RDB + AOF | Every 1 min | 7 days |
| Kafka | Log retention | Continuous | 7 days |
| S3 (Documents) | Cross-region replication | Real-time | Forever |

---

## Appendix

### A. Technology Versions

| Component | Version |
|-----------|---------|
| Java | 21 LTS |
| Spring Boot | 3.4.1 |
| Python | 3.12 |
| FastAPI | 0.115.x |
| Node.js | 22 LTS |
| NestJS | 10.x |
| Kafka | 3.7.x |
| PostgreSQL | 16.x |
| MongoDB | 7.x |
| Redis | 7.x |
| Kubernetes | 1.30.x |
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
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Bank Indonesia BI-FAST](https://www.bi.go.id/id/fungsi-utama/sistem-pembayaran/bi-fast/default.aspx)
- [OJK Digital Banking Guidelines](https://www.ojk.go.id/)

---

**Document Version**: 1.0  
**Last Updated**: January 2026  
**Owner**: Engineering Team PayU  
**Status**: Production Ready
