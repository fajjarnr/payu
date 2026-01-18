# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Updated technology stack with polyglot strategy:
  - Core Banking: Spring Boot 3.4 (account, auth, transaction, wallet)
  - Supporting Services: Quarkus 3.x Native (billing, notification, gateway, card, promotion)
  - ML Services: Python 3.12 FastAPI (kyc, analytics, recommendation)
- Updated message broker to hybrid approach:
  - AMQ Streams (Kafka) for event sourcing, saga, CDC
  - AMQ Broker (AMQP 1.0) for notifications, webhooks, legacy integration
- Updated identity provider to Red Hat SSO (Keycloak)
- Added polyglot microservices strategy diagram

### Added
- Initial PRD.md with comprehensive digital banking requirements
- ARCHITECTURE.md with production-ready microservices architecture
  - Microservices decomposition (Account, Auth, Transaction, Wallet, Billing, KYC, Notification, Analytics)
  - Event-driven architecture with Apache Kafka
  - Saga pattern for distributed transactions
  - CQRS and Event Sourcing patterns
  - Security architecture (PCI DSS, ISO 27001 compliance)
  - TokoBapak payment-service integration API specification
  - Infrastructure & DevOps (Kubernetes, Istio, Observability)
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
