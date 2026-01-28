---
name: payu-development
description: High-level development guide for PayU Digital Banking Platform - architecture overview, technology stack, and entry point for specialized skills.
---

# PayU Digital Banking Development (High-Level)

This skill provides the architectural overview and entry points for developing the **PayU Digital Banking Platform**. For specific implementation details, refer to the specialized skills.

## Project Overview

| Attribute | Value |
|-----------|-------|
| **Project Name** | PayU |
| **Type** | Standalone Digital Banking Platform |
| **Platform** | Red Hat OpenShift 4.20+ |
| **Primary Languages** | Java 21 (Core), Python 3.12 (ML) |

## ⚡ AI-Accelerated Development (SDLC Loop)

PayU mengoptimalkan kecepatan development dengan memanfaatkan kemampuan multi-agent secara paralel. Ikuti siklus 4-fase ini untuk setiap fitur/bug:

1. **Discovery**: Gunakan `@explorer-agent` untuk memetakan struktur file dan dependensi.
2. **Analysis**: Panggil spesialis domain (misal: `@database-engineer`) untuk merancang skema atau `@security-engineer` untuk audit kepatuhan.
3. **Implementation**: Eksekusi coding dengan `@backend-engineer` atau `@frontend-engineer` mengikuti standar TDD.
4. **Verification**: Gunakan `@qa-engineer` untuk memverifikasi perubahan dengan test suite otomatis dan `@code-review` untuk kepatuhan gaya kode.

> **Tip**: Untuk tugas lintas-service, gunakan workflow `/multi-agent-coordination`.

### Microservices Map

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

## Specialized Skills Map

| Task Category | Use Skill | Description |
|---------------|-----------|-------------|
| **Backend & DB** | `@backend-engineer`, `@database-engineer`, `@error-handling-engineer` | Microservices, SQL Optimization, Error Handling Patterns |
| **Frontend & Mobile** | `@frontend-engineer`, `@mobile-engineer` | Next.js, React Native, UI/UX, Design Systems |
| **API & Messaging** | `@api-design`, `@event-driven-architecture` | REST/OpenAPI, Kafka, Saga Patterns, CloudEvents |
| **Testing & QA** | `@qa-engineer` | Integration Tests, Performance, Strategy |
| **Security & Compliance** | `@security-engineer` | PCI-DSS, Encryption, OJK Compliance, PII |
| **DevOps & Infra** | `@devops-engineer`, `@container-engineer` | CI/CD, Tekton, ArgoCD, Automation Scripts |
| **Observability** | `@observability-engineer` | Distributed Tracing, Jaeger, Logs (Loki), Metrics |
| **Automation** | `@debugging-engineer` | Root Cause Analysis, Performance Debugging |
| **Workflow & Docs** | `@c4-architecture`, `@git-workflow`, `@code-review`, `@docs-engineer`, `/multi-agent-coordination` | Architecture Visualization, PR Standards, Documentation |
| **AI & Data** | `@ml-engineer` | Python, FastAPI, ML Models, Fraud Analytics |
| **Strategy & Lead** | `@cto-advisor` | Technology Strategy, Engineering Metrics, Scaling |

## 🤖 AI Agent Orchestration (Modular Execution)

PayU menggunakan arsitektur **Specialized Agents** untuk eksekusi tugas yang terfokus dan aman. Skills (High-level) dapat menugaskan (fork) spesialis berikut di direktori `.claude/agents/`:

- **`@scaffolder`**: Pembuatan service/folder structure baru.
- **`@logic-builder`**: Implementasi Domain logic & DDD patterns.
- **`@tester`**: Penulisan & eksekusi test suite (JUnit, Gatling).
- **`@security-auditor`**: Audit keamanan & kepatuhan (PCI-DSS).
- **`@migrator`**: Manajemen skema database & Flyway.
- **`@styler`**: Implementasi UI/UX & estetika (Emerald Design).
- **`@orchestrator`**: Manajemen CI/CD, Git, & alur kerja otomatis.

> **Workflow**: Untuk tugas kompleks, fork agent yang relevan dengan parameter perintah yang spesifik. Laporan akhir dari agent harus diverifikasi sebelum penggabungan kode.

## Architecture Decision & Trade-offs

Setiap keputusan arsitektur di PayU harus melalui evaluasi **Trade-off Analysis** yang tercatat dalam ADR.

1. **Simplicity First**: Pilih solusi paling sederhana yang memenuhi syarat. Hindari over-engineering.
2. **Rationality over Patterns**: Jangan gunakan pola (misal: Microservices) hanya karena tren, tetapi karena ia menyelesaikan hambatan spesifik (misal: *independent scaling*).
3. **Explicit Trade-offs**: Akui apa yang dikorbankan (misal: *eventual consistency* demi *high availability*).

## Core Architecture Patterns

### 1. Hexagonal Architecture (Core Banking)
Used in Account, Auth, Transaction, Wallet services.
- Decouples domain logic from frameworks.
- Easier to test business rules in isolation.

### 2. Event-Driven Architecture
- **Broker**: AMQ Streams (Kafka).
- **Patterns**: Saga (Choreography for simple flows, Orchestration for complex distributed transactions).
- **Consistency**: Eventual consistency for cross-service operations.
- **Advanced**: Entity Workflows, Fan-Out/Fan-In, and **Event Store Design** (PostgreSQL streams, snapshots, OCC).

### 3. CQRS (Command-Query Responsibility Segregation)
- Separates Read and Write operations for performance and scalability.
- Read models are optimized for UI/Dashboard requirements (e.g., in `analytics-service`).
- Implemented via the `@backend-engineer` power patterns.

### 4. Domain-Driven Design (DDD)
- **Bounded Contexts**: Microservices mapped to business domains.
- **Ubiquitous Language**: Shared terminology between business and dev.
- **Tactical Patterns**: Entities, Value Objects, and Aggregates for robust logic.

### 5. PCI DSS Compliance
- Strict adherence to cardholder data protection standards.
- Scope reduction via tokenization and network segmentation.
- Managed by the `@security-engineer` directives.

### 6. Polyglot Persistence
- **PostgreSQL**: Primary transactional storage (isolated schemas).
- **Redis**: Caching and distributed locks.
- **TimescaleDB**: Time-series analytics.

### 7. Observability
- **Logs**: LokiStack (JSON Structured).
- **Metrics**: Prometheus/Grafana.
- **Traces**: Jaeger (OpenTelemetry).
- **History**: Architecture Decision Records (ADR) in `docs/adr/`.

## 🚀 Feature Rollout & A/B Testing Workflow

Every new high-impact feature must follow the gated rollout process via **`ab-testing-service`**.

| Step | Action | Responsibility |
| :--- | :--- | :--- |
| **1. Define** | Create Experiment Key in `ab-testing-service` (e.g., `fx_v2_engine`). | Product/Dev |
| **2. Code** | Wrap feature logic in variant checks. Use **`@mobile-engineer`** or **`@frontend-engineer`** gating patterns. | Developer |
| **3. Test** | Verify both Control and Variant paths using **`@qa-engineer`** gated testing patterns. | QA/Dev |
| **4. Rollout** | Increase traffic split incrementally (10% -> 50% -> 100%). | @devops-engineer |
| **5. Cleanup** | Once 100% rolled out, remove the gating logic and experiment key. | Developer |

---
*ALWAYS use the specialized skills for deeper context when performing specific tasks.*
