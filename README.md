# PayU Digital Banking Platform

<div align="center">
  <img src="frontend/web-app/public/logo.png" alt="PayU Logo" width="120" height="120" />
  <h1>PayU</h1>
  <p><strong>Platform digital banking modern untuk generasi digital Indonesia</strong></p>
  <p>Built on <strong>Red Hat OpenShift 4.20+</strong> ecosystem</p>

  [![Platform](https://img.shields.io/badge/platform-OpenShift%204.20+-EE0000?logo=redhat)]()
  [![License](https://img.shields.io/badge/license-Proprietary-red.svg)]()
  [![Status](https://img.shields.io/badge/status-In%20Development-yellow.svg)]()
</div>

---

## 📋 Overview

**PayU** (bahasa Jawa: "laku/berhasil") adalah platform digital banking standalone yang menyediakan pengalaman perbankan yang mudah, cepat, dan aman. Platform ini dirancang sebagai payment infrastructure untuk multiple projects.

## 🎯 Key Features

- **Digital Account Opening** - eKYC dalam < 5 menit
- **Multi-Pocket System** - Kelola hingga 10 kantong tabungan
- **Instant Transfer** - BI-FAST, QRIS, dan transfer internal
- **Bill Payment** - PLN, PDAM, Pulsa, dan lainnya
- **Financial Management** - Budget tracker, goals, dan insights
- **Virtual Cards** - Kartu virtual untuk belanja online

## 🏗️ Technology Stack

### Red Hat OpenShift 4.20+ Ecosystem

| Layer | Red Hat Product | Portable Alternative |
|-------|-----------------|----------------------|
| **Container Platform** | OpenShift 4.20+ | Kubernetes |
| **Core Banking** | Red Hat Runtimes (Spring Boot 3.4) | Spring Boot |
| **Supporting Services** | Red Hat Build of Quarkus 3.x | Quarkus |
| **Caching** | Red Hat Data Grid (RESP mode) | Redis, ElastiCache |
| **Event Streaming** | AMQ Streams (Kafka) | Apache Kafka |
| **Message Queue** | AMQ Broker (AMQP 1.0) | ActiveMQ Artemis |
| **Identity** | Red Hat SSO (Keycloak) | Keycloak, Auth0 |
| **Logging** | OpenShift Logging (LokiStack) | Grafana Loki |
| **Monitoring** | OpenShift Monitoring | Prometheus/Grafana |

> **Portability**: All components use standard APIs. Code remains portable - only configuration changes needed to switch providers.

### Service Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                    RED HAT OPENSHIFT 4.20+ ECOSYSTEM                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  CORE BANKING (Spring Boot)         SUPPORTING (Quarkus Native)         │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐     │
│  │ account-service             │    │ gateway-service             │     │
│  │ auth-service                │    │ billing-service             │     │
│  │ transaction-service         │    │ notification-service        │     │
│  │ wallet-service              │    │ card-service                │     │
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
├── docs/               # Documentation (Architecture, Product, Roadmap)
├── scripts/            # Infrastructure & utility scripts
├── tests/              # Infrastructure & monitoring tests
├── CHANGELOG.md        # Version history
├── README.md           # This file
└── backend/            # Microservices implementation
    ├── account-service/
    ├── auth-service/
    ├── transaction-service/
    ├── wallet-service/
    ├── billing-service/
    ├── notification-service/
    ├── kyc-service/
    └── gateway-service/
```

## 🔗 Integration

PayU dapat diintegrasikan sebagai **External Banking Provider** untuk project lain:

```text
┌──────────────────┐         ┌──────────────────┐
│    TokoBapak     │         │    Project X     │
│  payment-service │         │  payment-client  │
└────────┬─────────┘         └────────┬─────────┘
         │                            │
         │  HTTPS + OAuth2            │
         └──────────┬─────────────────┘
                    │
         ┌──────────▼──────────┐
         │       PayU          │
         │  (Standalone API)   │
         │                     │
         │  /v1/partner/auth   │
         │  /v1/partner/payments│
         │  Webhook Callbacks  │
         └─────────────────────┘
```

### Partner API

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/partner/auth/token` | POST | Get access token (OAuth2) |
| `/v1/partner/payments` | POST | Create payment |
| `/v1/partner/payments/{id}` | GET | Get payment status |
| `/v1/partner/payments/{id}/refund` | POST | Refund payment |

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [PRD.md](./docs/product/PRD.md) | Product Requirements Document |
| [ARCHITECTURE.md](./docs/architecture/ARCHITECTURE.md) | Technical Architecture |
| [Roadmap](./docs/roadmap/TODOS.md) | Project Roadmap |
| [Disaster Recovery](./docs/operations/DISASTER_RECOVERY.md) | DR & Backup Procedures |
| [Contributing](./docs/guides/CONTRIBUTING.md) | Development Guidelines |
| [CHANGELOG.md](./CHANGELOG.md) | Version History |

## 🛡️ Compliance

- PCI DSS Level 1
- ISO 27001
- OJK Digital Banking License (target)
- BI-FAST Participation (target)

## 🚀 Getting Started

```bash
# Clone repository
git clone <repository-url>
cd payu

# View documentation
cat docs/architecture/ARCHITECTURE.md
cat docs/product/PRD.md
```

## 📞 Contact

- **Architecture**: backend-team@payu.id
- **Infrastructure**: platform-team@payu.id

---

**© 2026 PayU Digital Banking** | Built with ❤️ on Red Hat OpenShift
