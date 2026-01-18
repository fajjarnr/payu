# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
- Added portability notes for all components (no vendor lock-in)

### Added

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
