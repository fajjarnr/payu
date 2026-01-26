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

| Layer                   | Red Hat Product                    | Portable Alternative |
| ----------------------- | ---------------------------------- | -------------------- |
| **Container Platform**  | OpenShift 4.20+                    | Kubernetes           |
| **Core Banking**        | Red Hat Runtimes (Spring Boot 3.4) | Spring Boot          |
| **Supporting Services** | Red Hat Build of Quarkus 3.x       | Quarkus              |
| **Identity & SSO**      | Red Hat SSO (Keycloak 24+)         | Keycloak, Auth0      |
| **Event Streaming**     | AMQ Streams (Kafka)                | Apache Kafka         |
| **Message Queue**       | AMQ Broker (Artemis)               | ActiveMQ Artemis     |
| **Caching**             | Red Hat Data Grid (RESP mode)      | Redis, ElastiCache   |
| **Service Mesh**        | OpenShift Service Mesh             | Istio                |
| **Logging**             | OpenShift Logging (LokiStack)      | Grafana Loki         |
| **Monitoring**          | OpenShift Monitoring               | Prometheus/Grafana   |

### Service Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                    RED HAT OPENSHIFT 4.20+ ECOSYSTEM                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  CORE BANKING (Spring Boot)         SUPPORTING (Quarkus Native)         │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐     │
│  │ account-svc   auth-svc      │    │ gateway-svc   billing-svc   │     │
│  │ transaction-svc wallet-svc  │    │ notification-svc card-svc   │     │
│  │ investment-svc lending-svc  │    │ support-svc   cms-svc       │     │
│  │ fx-svc  statement-svc       │    │ api-portal-svc ab-testing-svc│     │
│  └─────────────────────────────┘    └─────────────────────────────┘     │
│                                                                          │
│  AI/ML (FastAPI)                    SHARED LIBRARIES                    │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐     │
│  │ kyc-svc  analytics-svc      │    │ security-starter            │     │
│  │                             │    │ resilience-starter          │     │
│  │                             │    │ cache-starter               │     │
│  └─────────────────────────────┘    └─────────────────────────────┘     │
│                                                                          │
│  DATA LAYER                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ PostgreSQL 16 (JSONB)  │  Data Grid (RESP)  │  TimescaleDB     │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📁 Project Structure

```
payu/
├── .agent/             # AI Agent skills & workflows
├── docs/               # Architecture, PRD, & Operations guides
├── backend/            # Microservices implementation
│   ├── shared/         # Shared libraries (Security, Resilience, Cache)
│   ├── simulators/     # External service simulators (BI-FAST, QRIS)
│   └── [service-name]/ # Individual microservices
├── frontend/           # Web applications
│   ├── web-app/        # Core digital banking web app
│   └── developer-docs/ # Partner documentation site
├── mobile/             # Mobile application (Expo/React Native)
├── infrastructure/     # OpenShift manifests, Helm, & Pipelines
├── tests/              # Performance (Gatling) & Regression (Pytest)
└── CHANGELOG.md        # Detailed version history
```

## 📚 Documentation

| Document                                               | Description                              |
| ------------------------------------------------------ | ---------------------------------------- |
| [ARCHITECTURE.md](./docs/architecture/ARCHITECTURE.md) | Technical Architecture & Design Patterns |
| [GEMINI.md](./docs/guides/GEMINI.md)                   | AI Assistant Guidelines (CLAUDE.md)      |
| [PRD.md](./docs/product/PRD.md)                        | Product Requirements & Features          |
| [TODOS.md](./docs/roadmap/TODOS.md)                    | Project Roadmap                          |
| [CONTRIBUTING.md](./docs/guides/CONTRIBUTING.md)       | Development & Git Guidelines             |

## 🚀 Getting Started

```bash
# Clone repository
git clone <repository-url>
cd payu

# Local Infrastructure (Docker Compose)
docker-compose up -d

# Build Backend
mvn clean package -DskipTests -T 1C

# Run Web App
cd frontend/web-app && npm run dev
```

## 📞 Contact

- **Architecture**: architect@payu.id
- **Engineering**: backend-team@payu.id
- **Platform**: platform-team@payu.id

---

**© 2026 PayU Digital Banking** | Built with ❤️ on Red Hat OpenShift
