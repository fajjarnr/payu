# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Project Structure**: Complete monorepo setup
  - `backend/` - All microservices
  - `backend/simulators/` - External service simulators
  - `frontend/` - Web, mobile, admin apps
  - `infrastructure/` - OpenShift, Terraform, Helm configurations
  - `docs/` - API, architecture, runbooks

- **BI-FAST Simulator** (Quarkus 3.17.5):
  - Account inquiry endpoint (`POST /api/v1/inquiry`)
  - Fund transfer endpoint (`POST /api/v1/transfer`)
  - Status check endpoint (`GET /api/v1/status/{ref}`)
  - Configurable latency simulation (50-500ms)
  - Configurable failure rate (default 5%)
  - Test bank accounts (BCA, BRI, MANDIRI, BNI, etc.)
  - Blocked and timeout scenarios for testing
  - Health checks and Prometheus metrics
  - OpenTelemetry tracing
  - Dockerfile with Red Hat UBI base images

- **OpenShift Manifests**:
  - Namespace definitions (5 environments)
  - BI-FAST Simulator deployment, service, configmap

- **External Service Simulators** (Section 12 in ARCHITECTURE.md):
  - BI-FAST Simulator (Quarkus Native) - transfer, inquiry, webhook
  - Dukcapil Simulator (Quarkus Native) - NIK verification, face matching
  - QRIS Simulator (Quarkus Native) - QR generation, payment

- **Frontend Architecture** (Section 13 in ARCHITECTURE.md):
  - Web App: Next.js 15 + Tailwind CSS 4
  - Mobile App: Expo (React Native)
  - Admin Dashboard: Next.js 15 + shadcn/ui
  - Shared layer: TypeScript, Zustand, TanStack Query

- **Lab Configuration & Decisions** (Section 14 in ARCHITECTURE.md):
  - 5 Environment strategy (DEV, SIT, UAT, PREPROD, PROD)
  - Infrastructure decisions (AWS ap-southeast-1, OpenShift 4.20+)
  - Security tools (Vault, RHACS, Falco, Wazuh)
  - External service strategy (simulators + free tier services)
  - Rate limiting configuration
  - User onboarding flow (2-3 min target)
  - Implementation phases (6 phases)

### Changed

- **Platform**: Red Hat OpenShift 4.20+ (full ecosystem focus)
- **Technology Stack** (polyglot strategy):
  - Core Banking: Red Hat Runtimes (Spring Boot 3.4)
  - Supporting Services: Red Hat Build of Quarkus 3.x Native
  - ML Services: Python 3.12 FastAPI (UBI-based)
- **Database Strategy**: Unified PostgreSQL + Data Grid
  - Replaced MongoDB with PostgreSQL (JSONB) for document storage
  - KYC, Notification services now use PostgreSQL
  - Red Hat Data Grid (RESP mode) for caching - Redis-compatible API
  - TimescaleDB for analytics (PostgreSQL extension)
- **Message Broker** (hybrid approach):
  - AMQ Streams (Kafka) for event sourcing, saga, CDC
  - AMQ Broker (AMQP 1.0) for notifications, webhooks
- **Observability**:
  - OpenShift Logging (LokiStack) - not ELK
  - OpenShift Monitoring (Prometheus/Grafana)
  - OpenShift Distributed Tracing (Jaeger)
- **Identity Provider**: Red Hat SSO (Keycloak)
- **CI/CD**: OpenShift Pipelines + GitOps (Tekton + ArgoCD)
- **Document Version**: Updated to 2.0
- Added portability notes for all components (no vendor lock-in)

### Initial Setup

- Initial PRD.md with comprehensive digital banking requirements
- ARCHITECTURE.md with production-ready microservices architecture
  - Microservices decomposition (Account, Auth, Transaction, Wallet, Billing, KYC, Notification, Analytics)
  - Event-driven architecture with AMQ Streams (Kafka)
  - Saga pattern for distributed transactions
  - CQRS and Event Sourcing patterns
  - Security architecture (PCI DSS, ISO 27001 compliance)
  - TokoBapak payment-service integration API specification
  - Infrastructure & DevOps (OpenShift, Istio, Observability)
  - Disaster Recovery & High Availability design

## [0.1.0] - 2026-01-18

### Added

- Project initialization
- PRD.md v1.1 with:
  - Core banking features (Account, Transfer, Payment, Bill Payment)
  - Financial management features (Budget, Goals, Insights)
  - Investment and loan features
  - Technical requirements and compliance
  - TokoBapak integration section
- ARCHITECTURE.md v1.0 with complete microservices design
