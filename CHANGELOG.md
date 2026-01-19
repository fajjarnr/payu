# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Billing Service** (Quarkus 3.17 Native):
  - Bill payments for PLN, PDAM, Pulsa, BPJS, etc.
  - REST API: `/api/v1/billers`, `/api/v1/payments`
  - Integration with wallet-service for balance debit
  - Kafka events for payment notifications
  - Hibernate Panache ORM with PostgreSQL

- **Wallet Service** (Spring Boot 3.4.1 - Hexagonal Architecture):
  - Domain Layer: `Wallet` and `WalletTransaction` models with business logic
  - Ports: `WalletUseCase` (input), `WalletPersistencePort`, `WalletEventPublisherPort` (output)
  - Adapters: JPA persistence, REST controller, Kafka event publisher
  - Balance management: get balance, reserve, commit, release, credit
  - Flyway database migrations for wallet tables
  - Unit tests (WalletServiceTest), Controller tests, ArchUnit architecture tests

- **Project Housekeeping**:
  - Removed duplicate `AGENTS.md` (content already in `GEMINI.md`)
  - Added `README.md` to `transaction-service`
  - Updated `GEMINI.md` project structure

- **Inter-Service Integration** (transaction-service → wallet-service):
  - Updated `WalletServiceAdapter` to call wallet-service REST API
  - Added Resilience4j circuit breaker and retry for wallet-service calls
  - Updated DTOs to match wallet-service API (`ReserveBalanceResponse`, `ReserveBalanceRequest`)
  - Added resilience4j configuration to ArchUnit allowed dependencies

- **TDD Infrastructure** (account-service):
  - Testcontainers for PostgreSQL and Kafka integration testing
  - ArchUnit 1.2.1 for architecture rule enforcement
  - JaCoCo 0.8.11 for code coverage reporting
  - H2 database for fast unit tests
  - Spring Security Test for authentication context

- **Test Classes** (account-service):
  - `OnboardingServiceTest` - Unit tests with Mockito
  - `OnboardingControllerTest` - WebMvcTest with security
  - `ArchitectureTest` - Layered architecture enforcement

- **SKILL.md** (Antigravity agent skill):
  - Created `.agent/skills/payu-development/SKILL.md`
  - Comprehensive development guidelines
  - TDD patterns and examples

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

- **Dukcapil Simulator** (Quarkus 3.17.5):
  - NIK verification endpoint (`POST /api/v1/verify`)
  - Face matching endpoint (`POST /api/v1/match-photo`)
  - Citizen data retrieval (`GET /api/v1/nik/{nik}`)
  - Configurable latency simulation (100-800ms)
  - Configurable failure rate (default 3%)
  - Simulated face match scores with configurable threshold (75%)
  - Liveness detection simulation
  - Test citizens (VALID, BLOCKED, INVALID, DECEASED statuses)
  - Verification audit logging
  - Health checks and Prometheus metrics

- **QRIS Simulator** (Quarkus 3.17.5):
  - QR code generation endpoint (`POST /api/v1/generate`)
  - Payment simulation endpoint (`POST /api/v1/pay`)
  - Status check endpoint (`GET /api/v1/status/{qrId}`)
  - Real QR code image generation (ZXing library)
  - QRIS-compliant QR content format
  - Configurable latency simulation (50-300ms)
  - Configurable failure rate (default 2%)
  - QR expiry handling (default 5 minutes)
  - Test merchants (Food & Beverage, Electronics, Health, etc.)
  - Health checks and Prometheus metrics

- **OpenShift Manifests**:
  - Namespace definitions (5 environments)
  - BI-FAST Simulator deployment, service, configmap
  - Dukcapil Simulator deployment, service, configmap
  - QRIS Simulator deployment, service, configmap
  - Gateway Service deployment, service, route, configmaps

- **Gateway Service** (Quarkus 3.17.5):
  - API Gateway for all backend services
  - Distributed rate limiting with Redis
  - Circuit breaker with fault tolerance
  - Correlation ID for distributed tracing
  - OIDC/JWT authentication support (Red Hat SSO)
  - Proxy routing to simulators and core services
  - Health, status, and version endpoints
  - Prometheus metrics and OpenTelemetry tracing
- **Account Service** (Spring Boot 3.4.1):
  - User Management (User, Account, Profile entities)
  - PostgreSQL integration with JSONB support for profiles
  - eKYC Integration with Dukcapil Simulator via Feign Client
  - OAuth2 Resource Server Security
  - Kafka Producer configuration
  - Registration API (`POST /api/v1/accounts/register`)

 - **Auth Service** (Spring Boot 3.4.1):
   - Keycloak Admin Client Integration
   - Login Proxy (Password Grant) with WebClient (Reactive)
   - User Registration support
   - OAuth2 Resource Server Security
   - Account lockout mechanism (5 failed attempts, 15 min duration)
   - Rate limiting for login endpoint (5 attempts per minute)
   - Password policy enforcement (8+ chars, uppercase, lowercase, digit, special char)
   - Resilience4j circuit breaker and retry for Keycloak calls

 - **Account Service** (Spring Boot 3.4.1) - Production Hardening:
   - Flyway database migrations (replaced hibernate.ddl-auto)
   - HikariCP connection pooling with production settings
   - Resilience4j circuit breaker for external gateway calls
   - Retry logic with exponential backoff
   - Security configuration with JWT authentication
   - Audit logging aspect for service methods
   - Proper SLF4J logging in exception handlers (removed printStackTrace)
   - JPA batch operations optimization
   - WebClient support added

 - **Auth Service** (Spring Boot 3.4.1) - Production Hardening:
   - WebClient replacing RestTemplate (non-blocking, better resource usage)
   - Rate limiting (5 login attempts per minute)
   - Account lockout after failed attempts
   - Password policy enforcement with validation
   - Resilience4j circuit breaker and retry
   - Proper SLF4J logging in exception handlers
   - Reactive endpoint handlers

 - **Docker Production Hardening**:
   - Non-root user (spring user)
   - JVM container support with memory percentage limits
   - G1GC configuration with max pause time
   - Heap dump on OOM
   - Health checks for both services
   - Secure random number generator



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
- Docker & Integration Test setup complete. Installed docker.io, created docker-compose.yml, added Testcontainers.
