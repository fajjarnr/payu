# PayU Digital Banking Platform

<div align="center">
  <img src="frontend/web-app/public/logo.png" alt="PayU Logo" width="120" height="120" />
  <h1>PayU</h1>
  <p><strong>Platform digital banking modern untuk generasi digital Indonesia</strong></p>
  <p>Built on <strong>Red Hat OpenShift 4.20+</strong> ecosystem</p>

[![Platform](https://img.shields.io/badge/platform-OpenShift%204.20+-EE0000?logo=redhat)]()
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)]()
[![Status](https://img.shields.io/badge/status-Active%20Development-green.svg)]()

</div>

---

## 📋 Overview

**PayU** (bahasa Jawa: "laku/berhasil") adalah platform digital banking standalone yang menyediakan pengalaman perbankan yang mudah, cepat, dan aman. Platform ini dirancang sebagai payment infrastructure berskala enterprise yang mengadopsi arsitektur microservices dan event-driven.

## 🎯 Key Features

- **Digital Account Opening** - eKYC dengan OCR & Liveness Detection.
- **Multi-Pocket System** - Kelola hingga 10 kantong tabungan dengan multi-currency.
- **Investment & Lending** - Robo-advisory, Reksa Dana, Emas, dan Loan Pre-approval (PayLater).
- **Instant Transfer** - BI-FAST, QRIS, dan transfer internal real-time.
- **Smart Bill Payment** - Pembayaran rutin otomatis (PLN, PDAM, Pulsa, TV Cable, Cicilan).
- **AI Fraud Detection** - Pengamanan transaksi real-time berbasis Machine Learning.
- **E-Statement Engine** - Laporan keuangan bulanan otomatis dalam format PDF.

## 🏗️ Technology Stack

### Red Hat OpenShift 4.20+ Ecosystem

| Layer                   | Red Hat Product                    | Portable Alternative | Purpose |
| ----------------------- | ---------------------------------- | -------------------- | ------- |
| **Container Platform**  | OpenShift 4.20+                    | Kubernetes           | Container orchestration, auto-scaling |
| **Core Banking**        | Red Hat Runtimes (Spring Boot 3.4) | Spring Boot          | Microservices framework (account, transaction, wallet) |
| **Supporting Services** | Red Hat Build of Quarkus 3.x       | Quarkus              | Native-compiled services (gateway, billing, notification) |
| **API Management**      | Red Hat 3scale                     | Kong, Apigee         | Partner API gateway, rate limiting, developer portal |
| **Identity & SSO**      | Red Hat Build of Keycloak (RHBK) v26 | Keycloak, Auth0    | OAuth2/OIDC, JWT token issuance, realm management |
| **Event Streaming**     | AMQ Streams (Kafka)                | Apache Kafka         | Domain event bus, transactional outbox relay |
| **Message Queue**       | AMQ Broker (Artemis)               | ActiveMQ Artemis     | Point-to-point messaging (notifications, dunning) |
| **Database**            | Crunchy PostgreSQL 16              | Any PostgreSQL       | ACID transactions, JSONB, database-per-service |
| **Caching**             | Red Hat Data Grid (RESP mode)      | Redis, ElastiCache   | Session cache, distributed locking, rate limit counters |
| **Service Mesh**        | OpenShift Service Mesh             | Istio, Linkerd       | mTLS, traffic management, observability |
| **Logging**             | OpenShift Logging (LokiStack)      | Grafana Loki         | Structured JSON log aggregation |
| **Monitoring**          | OpenShift Monitoring               | Prometheus/Grafana   | Metrics, alerting, SLA dashboards |

> **Portability**: All components use standard APIs (OIDC, RESP, Kafka Protocol, SQL, AMQP). Code remains portable — only configuration changes needed to switch providers.

### Infrastructure Components

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RED HAT OPENSHIFT 4.20+                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  EXTERNAL TRAFFIC                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │  3scale (Partner API)  →  Istio Ingress  →  gateway-service      │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                    │                                      │
│  ┌─────────────────────────────────┼─────────────────────────────────┐    │
│  │              OIDC/JWT           ▼                                 │    │
│  │  ┌─────────────┐    ┌──────────────────┐    ┌─────────────────┐  │    │
│  │  │  Keycloak   │◄───│  gateway-service  │───►│  Microservices  │  │    │
│  │  │  (RHBK v26) │    │  (Quarkus)        │    │  (20+ services) │  │    │
│  │  └─────────────┘    └──────────────────┘    └────────┬────────┘  │    │
│  │                                                      │           │    │
│  │  ┌───────────────────────────────────────────────────┼─────────┐ │    │
│  │  │                    EVENT LAYER                     │         │ │    │
│  │  │  ┌──────────────────┐    ┌──────────────────┐     │         │ │    │
│  │  │  │ AMQ Streams      │    │ AMQ Broker        │     │         │ │    │
│  │  │  │ (Kafka 3-broker) │    │ (Artemis 2-node)  │     │         │ │    │
│  │  │  │ Domain events    │    │ Notifications     │     │         │ │    │
│  │  │  │ Outbox relay     │    │ Dunning/billing   │     │         │ │    │
│  │  │  └──────────────────┘    └──────────────────┘     │         │ │    │
│  │  └───────────────────────────────────────────────────┘         │ │    │
│  │                                                                │ │    │
│  │  ┌───────────────────────────────────────────────────────────┐ │ │    │
│  │  │                    DATA LAYER                             │ │ │    │
│  │  │  PostgreSQL 16    │  Data Grid (RESP)  │  Vault (secrets) │ │ │    │
│  │  │  (per-service DB) │  (session/cache)   │  (encrypt/seal)  │ │ │    │
│  │  └───────────────────────────────────────────────────────────┘ │ │    │
│  └────────────────────────────────────────────────────────────────┘ │    │
│                                                                     │    │
│  OBSERVABILITY: Prometheus + Grafana + LokiStack + Tempo (planned)  │    │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Why Each Component?

| Component | Why PayU Uses It |
|:---|:---|
| **3scale** | External partner API management — rate limiting, API keys, usage analytics, developer portal. Tier 1 gateway for TokoBapak, Nobar, etc. |
| **Keycloak (RHBK)** | OAuth2/OIDC identity provider — JWT token issuance, realm-per-tenant, MFA, session management. All services validate JWT via gateway. |
| **AMQ Streams (Kafka)** | Domain event backbone — transactional outbox relay, CloudEvents 1.0.2 format, DLQ with `.dlq` suffix, topic pattern `payu.<domain>.<event>.v<n>`. |
| **AMQ Broker (Artemis)** | Point-to-point messaging — notification delivery (SMS/email/push), dunning for recurring billing, delayed message scheduling. |
| **PostgreSQL** | Database-per-service pattern — ACID guarantees, `DECIMAL(19,4)` for money, JSONB for flexible schemas, Flyway migrations. |
| **Data Grid** | Distributed cache (RESP protocol) — session data, rate limit counters, distributed locks (ShedLock), idempotency keys. |
| **Service Mesh (Istio)** | Zero-trust networking — mTLS between services, traffic management, canary deployments, observability sidecar. |
| **Vault** | Secret management — database credentials, API keys, encryption keys (Transit engine for PII). No secrets in code/properties. |

> 📖 Deep dive: [ARCHITECTURE.md §7 (API Gateway & Service Mesh)](./docs/architecture/ARCHITECTURE.md#7-api-gateway--service-mesh)

### Service Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                    RED HAT OPENSHIFT 4.20+ ECOSYSTEM                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  CORE BANKING (Spring Boot)         SUPPORTING (Quarkus Native)        │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐    │
│  │ account-svc   auth-svc      │    │ gateway-svc   billing-svc   │    │
│  │ transaction-svc wallet-svc  │    │ notification-svc cms-svc    │    │
│  │ investment-svc lending-svc  │    │ support-svc  api-portal-svc │    │
│  │ fx-svc statement-svc        │    │ partner-svc  promotion-svc  │    │
│  │ dispute-svc compliance-svc  │    │ integration-svc             │    │
│  └─────────────────────────────┘    └─────────────────────────────┘    │
│                                                                         │
│  AI/ML (FastAPI)                    SHARED LIBRARIES                   │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐    │
│  │ kyc-svc  analytics-svc      │    │ security-starter            │    │
│  │                             │    │ resilience-starter          │    │
│  │                             │    │ cache-starter outbox-starter│    │
│  │                             │    │ saga-starter events-starter │    │
│  └─────────────────────────────┘    └─────────────────────────────┘    │
│                                                                         │
│  DATA LAYER                                                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ PostgreSQL 16 (JSONB)  │  Data Grid (RESP)  │  TimescaleDB     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📁 Project Structure

```
payu/
├── .agents/              # AI Agent skills & workflows
├── docs/
│   ├── architecture/     # ARCHITECTURE.md, C4 diagrams
│   ├── adr/              # Architecture Decision Records (21 ADRs)
│   ├── api/              # API standards & specs
│   ├── guides/           # Onboarding, TDD, Vault, Webhooks, Lessons
│   ├── product/          # PRD, user stories
│   ├── roadmap/          # TODOS.md, PROGRESS.md, GATEWAY_ARCH.md
│   ├── security/         # Security policies
│   ├── compliance/       # PCI-DSS, UU PDP
│   └── operations/       # Runbooks, DR procedures
├── backend/
│   ├── shared/           # Shared starters (security, resilience, cache, outbox, saga, events)
│   ├── simulators/       # External service simulators (BI-FAST, QRIS, Dukcapil)
│   └── [service-name]/   # 20+ individual microservices (Hexagonal architecture)
├── frontend/
│   ├── web-app/          # Core digital banking web (Next.js)
│   └── developer-docs/   # Partner API documentation site
├── mobile/               # Mobile application (Expo/React Native)
├── infrastructure/
│   ├── local/podman/     # Local dev environment (podman-compose.yml)
│   ├── foundation/       # Cluster foundation (namespaces, RBAC)
│   ├── platform/         # Platform services (Kafka, Keycloak, 3scale)
│   └── workloads/        # Service deployments (Kustomize base/overlays)
├── scripts/
│   ├── setup/            # Dev environment setup (setup.sh)
│   ├── e2e/              # End-to-end test scripts
│   └── deployment/       # Build, push, deploy scripts
├── tests/                # Performance (Gatling/k6) & Regression (Pytest)
├── sdk/                  # Client SDKs (planned)
├── CHANGELOG.md          # Detailed version history (SemVer)
└── AGENTS.md             # AI Agent rules & coding standards
```

## 📚 Documentation

### Core Documents

| Document | Description |
|:---|:---|
| [ARCHITECTURE.md](./docs/architecture/ARCHITECTURE.md) | Technical architecture, C4 diagrams, design patterns, infrastructure |
| [PRD.md](./docs/product/PRD.md) | Product requirements, user stories, feature specs |
| [TODOS.md](./docs/roadmap/TODOS.md) | Product backlog, production readiness gaps, sprint plan |
| [CHANGELOG.md](./CHANGELOG.md) | Version history (SemVer, Conventional Commits) |
| [AGENTS.md](./AGENTS.md) | AI agent coding rules & non-negotiable standards |

### Developer Guides

| Document | Description |
|:---|:---|
| [ONBOARDING.md](./docs/guides/ONBOARDING.md) | First-day setup, environment config, dev workflow |
| [CONTRIBUTING.md](./docs/guides/CONTRIBUTING.md) | Git flow, PR guidelines, commit conventions |
| [TDD_QUICK_REFERENCE.md](./docs/guides/TDD_QUICK_REFERENCE.md) | Red-green-refactor patterns for PayU |
| [WEBHOOK_HANDLING.md](./docs/guides/WEBHOOK_HANDLING.md) | Partner webhook integration patterns |
| [LESSONS.md](./docs/guides/LESSONS.md) | Implementation patterns & lessons learned (85+ entries) |
| [TROUBLESHOOTING.md](./docs/TROUBLESHOOTING.md) | Common issues & debugging guide |

### Architecture Decisions (ADRs)

| ADR | Decision |
|:---|:---|
| [ADR-0002](./docs/adr/0002-spring-boot-for-core-banking.md) | Spring Boot for core banking services |
| [ADR-0003](./docs/adr/0003-quarkus-for-supporting-services.md) | Quarkus for supporting services (gateway, billing) |
| [ADR-0004](./docs/adr/0004-hexagonal-architecture.md) | Hexagonal architecture for all services |
| [ADR-0005](./docs/adr/0005-kafka-event-streaming.md) | Kafka for event streaming (vs RabbitMQ) |
| [ADR-0006](./docs/adr/0006-postgresql-primary-database.md) | PostgreSQL as primary database |
| [ADR-0014](./docs/adr/0014-api-management-platform.md) | 3scale for partner API management |
| [Full ADR Index](./docs/adr/README.md) | All 21 architecture decision records |

## 🔧 Prerequisites

| Tool | Version | Check |
|:---|:---|:---|
| **Java** | 21+ LTS (GraalVM CE or Temurin) | `java -version` |
| **Maven** | 3.9+ | `mvn -version` |
| **Node.js** | 22+ LTS | `node -v` |
| **Python** | 3.12+ | `python3 --version` |
| **Podman** | Latest (rootless) | `podman --version` |
| **Git** | Latest | `git --version` |

> **Auto-install**: Run `./scripts/setup/setup.sh` to install all dependencies (supports Ubuntu, Fedora, macOS).
> **Verify**: Run `./scripts/setup/setup.sh --check` to verify installed versions.

## 🚀 Getting Started

```bash
# 1. Clone repository
git clone <repository-url>
cd payu

# 2. Start local infrastructure (PostgreSQL, Kafka, Redis, Keycloak)
cd infrastructure/local/podman && podman compose up -d
cd ../../..

# 3. Build all shared starters first
mvn -f backend/shared/pom.xml clean install -DskipTests -q

# 4. Build all backend services
mvn -f backend/pom.xml clean package -DskipTests -T 1C

# 5. Run web app
cd frontend/web-app && npm install && npm run dev
```

### Useful Commands

```bash
# Run all tests
./scripts/run-all-tests.sh

# Test single service
./scripts/test-single-service.sh transaction-service

# Build & push to registry (OpenShift)
./scripts/build-push-all.sh

# Health check all services
./scripts/test-health-check.sh

# Seed test data
./scripts/seed-test-data.sh
```

## 🏗️ Design Patterns

| Pattern | Where | Purpose |
|:---|:---|:---|
| **Hexagonal Architecture** | All services | Port/Adapter separation, testable domain |
| **Transactional Outbox** | `outbox-starter` | Reliable event publishing without 2PC |
| **CQRS** | transaction-service | Separate command/query models |
| **Saga (Orchestration)** | `saga-starter` | Distributed transactions across services |
| **Database per Service** | All services | Data isolation, independent scaling |
| **Circuit Breaker + Retry** | `resilience-starter` | Fault tolerance for inter-service calls |
| **Idempotency** | gateway-service | `X-Idempotency-Key` header enforcement |
| **Double-Entry Bookkeeping** | wallet-service | Financial integrity, immutable ledger |

> 📖 Full details: [ARCHITECTURE.md §2.2 (Design Principles)](./docs/architecture/ARCHITECTURE.md#22-design-principles)

## 📞 Contact

- **Architecture**: architect@payu.fajjjar.my.id
- **Engineering**: backend-team@payu.fajjjar.my.id
- **Platform**: platform-team@payu.fajjjar.my.id

---

**© 2026 PayU Digital Banking** | Built with ❤️ on Red Hat OpenShift

