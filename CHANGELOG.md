# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Cross-Service Integration Tests** (Testing):
  - Implemented holistic End-to-End test suite covering full user journeys across all PayU services
  - Created comprehensive test files in `tests/e2e_blackbox/`:
    - `test_complete_user_journey.py` - Complete onboarding and transaction flows (registration, login, wallet, topup, transfers, bill payments, QRIS)
    - `test_investment_flow.py` - Wealth management features (investment accounts, deposits, mutual funds, digital gold)
    - `test_lending_flow.py` - Credit and lending services (credit score, loans, repayments, PayLater)
    - `test_promotion_flow.py` - Rewards and gamification (promotions, cashback, loyalty points, referrals)
    - `test_compliance_flow.py` - Regulatory compliance (AML/CFT audit reports, compliance checks, report search)
    - `test_support_flow.py` - Support team management (agents, training modules, training assignment, status tracking)
    - `test_partner_flow.py` - Partner and SNAP BI integration (partner CRUD, API keys, OAuth2, payments)
    - `test_analytics_flow.py` - Analytics and ML features (user metrics, spending trends, cash flow, recommendations)
    - `test_backoffice.py` - Operational flows (KYC reviews, fraud cases, customer support cases)
  - Enhanced `client.py` with improved HTTP client (timeout support, PATCH/DELETE methods, better error handling)
  - Created comprehensive test infrastructure:
    - `requirements.txt` - Python dependencies (pytest, requests, faker, pytest-asyncio)
    - `pytest.ini` - Pytest configuration with markers (smoke, critical, integration, e2e, service-specific)
    - `conftest.py` - Shared fixtures and test configuration
    - `Makefile` - Convenience commands for running tests (make test, make test-smoke, etc.)
    - `run_tests.sh` - Bash script for test execution with options (verbose, coverage, stop-on-fail)
    - `README.md` - Comprehensive documentation for test suite (setup, usage, troubleshooting, CI/CD integration)
  - Test architecture:
    - Holistic approach covering complete user workflows
    - Cross-service integration verification
    - Event-driven operation validation with retries
    - Graceful degradation using pytest.skip() for unavailable services
    - Realistic test data generation using Faker library
  - Test coverage:
    - All 15 PayU microservices (account, auth, wallet, transaction, billing, notification, investment, lending, promotion, compliance, support, partner, analytics, backoffice, kyc)
    - 50+ test cases across 9 test files
    - Service-specific test markers for selective execution
  - Updated TODOS.md to mark cross-service integration tests as complete

- **OJK/BI Regulatory Audit Documentation** (Compliance):
  - Created comprehensive OJK/BI regulatory audit technical documentation at `docs/compliance/OJK_BI_REGULATORY_AUDIT.md`
  - Documentation covers:
    - Executive Summary with licensing status and compliance matrix
    - System Architecture Overview (technology stack, microservices, data architecture)
    - Regulatory Compliance Framework (OJK and BI regulations compliance matrices)
    - Information Security Management (security architecture, encryption, IAM, AML/CFT)
    - Data Privacy & Protection (UU PDP No. 27/2022 compliance, data subject rights)
    - Anti-Money Laundering (AML/CFT program, transaction monitoring, STR reporting)
    - Transaction Monitoring & Fraud Detection (multi-layered detection, transaction limits)
    - Business Continuity & Disaster Recovery (RTO/RPO, backup strategy, procedures)
    - Audit Trails & Logging (comprehensive audit logging, immutable logging)
    - Risk Management Framework (risk identification, assessment matrix, KRIs)
    - Testing & Certification Evidence (security audits, performance testing)
    - Compliance Gap Analysis (current status, mitigation timeline)
  - Includes references to existing documentation (ARCHITECTURE.md, PENTEST_REPORT.md, DISASTER_RECOVERY.md)
  - Provides complete evidence for OJK/BI regulatory audit submission
  - Maps all requirements from POJK and BI regulations to technical implementations
  - Updated TODOS.md to mark regulatory audit documentation as complete

- **Disaster Recovery Verification** (Testing):
  - Added comprehensive integration test suite for PostgreSQL backup-restore procedures
  - Added comprehensive integration test suite for Kafka backup-restore procedures
  - Created `test_backup_restore_integration.py` with 13 test cases verifying:
    - PostgreSQL container accessibility and connectivity
    - PostgreSQL test data creation and backup generation
    - PostgreSQL backup integrity verification
    - Kafka container accessibility and topic management
    - Kafka message production and verification
    - Complete disaster recovery workflow scenarios for both PostgreSQL and Kafka
  - Updated existing `test_backup_restore.py` to use correct DRP documentation path (`docs/operations/DISASTER_RECOVERY.md`)
  - Fixed DRP documentation path references across 6 test classes
  - All disaster recovery procedures for PostgreSQL and Kafka verified through automated testing

- **Distributed Tracing with Jaeger/OpenTelemetry** (Observability):
  - Added Jaeger all-in-one container to docker-compose.yml (port 16686 for UI, port 4317 for OTLP)
  - Configured OTLP trace export for all 15 PayU microservices:
    - Spring Boot services (account, auth, transaction, wallet, compliance, investment, lending) with management.tracing and management.otlptracing configuration
    - Quarkus services (gateway, billing, notification, backoffice, partner, promotion, support) with quarkus.otel configuration
    - Python FastAPI services (analytics, kyc) with existing OpenTelemetry instrumentation
  - Added OTEL_ENDPOINT environment variable to all services in docker-compose.yml (pointing to http://jaeger:4317)
  - Added TracingConfigurationTest.java for account-service to verify tracing instrumentation
  - Enabled 10% sampling probability for production trace optimization
  - Configured service name and version attributes for proper trace identification in Jaeger UI
  - Added health check dependency for Jaeger to ensure tracing backend is ready before services start

- **Grafana Dashboards for All Microservices** (Monitoring):
  - Created comprehensive Grafana dashboards for all 15 PayU microservices organized by service category:
    - Core Banking Services Dashboard (account, auth, transaction, wallet)
    - Supporting Services Dashboard (billing, notification, gateway, compliance)
    - ML & Analytics Services Dashboard (kyc, analytics)
    - Business & Operations Services Dashboard (investment, lending, backoffice, partner, promotion, support)
    - Infrastructure Monitoring Dashboard (postgres, redis, kafka, prometheus, grafana, loki)
  - Updated Prometheus configuration to include all 15 microservices with correct metrics paths:
    - Spring Boot services: `/actuator/prometheus`
    - Quarkus services: `/q/metrics`
    - FastAPI services: `/metrics`
  - Added business and operations services to docker-compose.yml:
    - investment-service (port 8009)
    - lending-service (port 8010)
    - backoffice-service (port 8011)
    - partner-service (port 8012)
    - promotion-service (port 8013)
    - support-service (port 8014)
  - Created PostgreSQL databases for new services (investment, lending, backoffice, partner, promotion, support)
  - Updated gateway-service routing configuration to include all new services
  - Created comprehensive test suite with 9 test cases validating dashboard JSON structure and service targets
  - Configured health, performance, and resource monitoring panels for each service category
  - Added JVM metrics for Java services, memory usage for Python services
  - Included Kafka integration metrics, database connection pooling, and GC statistics

### Added

- **LokiStack for Centralized Log Management** (Infrastructure):
  - Deployed LokiStack operator for OpenShift-native centralized log aggregation
  - Created logging namespaces (openshift-logging, openshift-operators-redhat)
  - Configured ClusterLogForwarder to forward application, infrastructure, and audit logs
  - Set up LokiStack with S3 storage backend and 30-day retention
  - Implemented Vector-based log collection for all PayU microservices
  - Added Loki alert rules for error rate, latency, database connections, and service downtime
  - Created OpenShift Route for external Loki gateway access
  - Configured RBAC permissions for log collection (loki-promtail)
  - Added comprehensive LokiStack deployment script (`scripts/deploy_lokistack.sh`)
  - Created test suite with 19 test cases validating LokiStack infrastructure
  - Documented LokiStack deployment, configuration, and usage (`docs/operations/LOKISTACK.md`)

### Changed

- **Vault Integration** (Secrets Management):
  - Added HashiCorp Vault service to docker-compose.yml for secure secrets management
  - Migrated hardcoded secrets to Vault KV secrets engine (db, keycloak, kafka, redis, grafana)
  - Updated docker-compose.yml to use environment variables with fallback defaults
  - Added Spring Cloud Vault dependencies to account-service and auth-service
  - Configured Vault integration in application.yaml files with enabled/disabled flag
  - Created Vault initialization script (`infrastructure/docker/init-vault.sh`) for populating secrets
  - Created Vault configuration file (`infrastructure/docker/vault-config.json`)
  - Added Vault configuration tests for both services
  - Updated test profiles to disable Vault for unit tests
  - Created comprehensive Vault integration guide (`docs/guides/VAULT.md`)

- **Frontend Feature Enhancements** (web-app):
  - **Transfer Evolution**: Added BI-FAST, SKN, RTGS transfer type selection to transfer page with fee information and processing times
  - **Scheduled Transfers**: Implemented transfer scheduling options (now, scheduled date, recurring monthly transfers)
  - **Live Analytics**: Integrated WebSocket for real-time portfolio updates with connection status indicator
  - **Shared Pockets**: Added joint savings pockets UI with member management, role-based access (OWNER, ADMIN, MEMBER)
  - **WebSocket Infrastructure**: Created reusable WebSocket hook with reconnection logic and event handling
  - **Type Updates**: Extended types to support new transfer types, scheduling options, and shared pocket members
  - **Tests**: Added comprehensive unit tests for WebSocket hooks (10 test cases passing)
  - **Code Quality**: Fixed linting errors and improved type safety across all new components

- **UI Standardization & Cleanup (Premium Emerald)**:
  - **Refined Typography**: Removed all italic fonts and reduced excessive use of uppercase and tracking-tighter for a cleaner, more professional look across the entire application.
  - **Standardized Spacing**: Applied consistent vertical spacing (`space-y-12`, `mt-12`) and `rounded-xl` borders to all major pages (`/pockets`, `/cards`, `/investments`, `/transfer`, `/support`, `/security`, `/settings`).
  - **Page Refactoring**:
    - **Pockets**: Standardized "Main Balance", "Savings Goals", and "Recent History" cards.
    - **Cards**: Implemented glassmorphism aesthetics for virtual cards and standardized control panels.
    - **Transfer**: Cleaned up the "Instant Transfer" and "Review" flows with consistent input fields and motion transitions.
    - **Investments**: Refactored the marketplace grid and portfolio overview for better data visualization.
    - **Settings/Support/Security**: Unified sidebar layouts, profile summaries, and status indicators.
  - **Mobile Responsiveness**: Fixed bottom padding issues in `DashboardLayout` to prevent content from being obscured by the fixed mobile navigation bar (`pb-40` for mobile).
  - **Global Theme**: Resolved inconsistent styling tokens in `globals.css` and ensured full compliance with the Emerald Green design system.

- **CI/CD Simplification**:
  - Disabled GitHub Actions workflows (`.github/workflows`) by renaming them to `.yml.disabled` as the project transitions to OpenShift Pipelines (Tekton) and ArgoCD for CI/CD.
- **Documentation Restructuring**:
  - Reorganized project documentation into a dedicated `docs/` directory with subdirectories for `architecture`, `product`, `operations`, `security`, `guides`, and `roadmap`.
  - Updated `README.md` and related files to point to the new documentation paths.

### Added

- **TokoBapak Integration** (partner-service):
  - Implemented `/v1/partner/payments/{id}/refund` endpoint for payment refunds
  - Added RefundRequest and RefundResponse DTOs for refund API
  - Enhanced SnapBiPaymentService with refund processing logic and RefundRecord storage
  - Extended webhook events to support `payment.failed`, `payment.expired`, and `refund.completed` notifications
  - Added `@Blocking` annotation to Uni-returning Resource methods for proper thread management
  - Implemented comprehensive TokoBapak integration tests (3 test cases):
    - Full flow test (payment creation, completion, and refund)
    - Refund non-existent payment error handling
    - Refund pending payment validation (should fail)
  - All 50 tests passing with proper test coverage

- **GDPR Compliance Audit System**:
  - Added GDPR to ComplianceStandard enum
  - DataAccessAudit domain model for tracking user data access patterns
  - DataAccessAuditService for audit logging with comprehensive query capabilities
  - DataAccessAudit persistence adapter and repository with JPA support
  - GdprAuditController with RESTful endpoints for GDPR compliance
  - DTOs for data access audit API (DataAccessAuditRequest, DataAccessAuditResponse, DataAccessAuditSearchRequest)
  - Comprehensive unit tests for DataAccessAuditService (14 test cases)
  - Comprehensive unit tests for GdprAuditController (11 test cases)
  - Data access tracking by user, service, operation type, and date range
  - Failed access attempt monitoring
  - Search and filter capabilities for GDPR compliance reporting

- **Native Mobile Apps Boilerplate**:
  - **iOS App** (Swift/SwiftUI):
    - Implemented Swift 5.9+ with SwiftUI for iOS 16.0+
    - MVVM architecture with async/await
    - Key screens: Home, Accounts, Transfers, Cards, Profile
    - URLSession-based API client with comprehensive error handling
    - Balance cards, transaction history, and quick actions
    - Virtual card management interface
    - AppState for user session management
    - Comprehensive unit tests for models and API client
  - **Android App** (Kotlin/Jetpack Compose):
    - Implemented Kotlin 1.9.22 with Jetpack Compose
    - MVVM architecture with Hilt dependency injection
    - Target SDK 35 (Android 15) with minimum SDK 26
    - Key screens: Home, Accounts, Transfers, Cards, Profile
    - Retrofit + OkHttp for networking
    - DataStore for secure token storage
    - Material 3 design system with custom theming
    - Comprehensive unit tests for models and token management
  - **Shared Features**:
    - Consistent UI/UX across both platforms
    - RESTful API integration with PayU backend
    - Authentication and session management
    - Error handling and loading states
    - Modular architecture for easy feature additions
    - Production-ready configurations
  - Documentation:
    - Comprehensive README.md with setup instructions
    - API configuration guidelines
    - Testing and build instructions
    - Security best practices

- **Analytics Service - Real-time Updates (WebSocket/Kafka)**:
  - Enhanced WebSocket connection management with event filtering capabilities
  - Implemented subscription-based event delivery to dashboard clients
  - Added connection establishment confirmation messages with subscribed events list
  - Implemented dynamic subscription updates via WebSocket messages
  - Enhanced ping/pong heartbeat mechanism with timestamps
  - Added event type filtering based on user subscriptions
  - Kafka consumer now broadcasts events with proper event type metadata
  - Fixed Boolean type import in database schema
  - Fixed AsyncMock import in e2e tests
  - Added integration tests for Kafka message consumption
  - Added unit tests for subscription management and event filtering

- **Frontend Overhaul (Premium Emerald)**:
  - Implemented **Premium Emerald** design system across all web applications.
  - Added `DashboardLayout` with persistent sidebar, responsive header, and glassmorphism mobile navigation.
  - **Localization (Bahasa Indonesia)**: Translated all frontend pages and components to Bahasa Indonesia as the primary language for Phase 1.
  - Redesigned **Pockets** (`/pockets`) page with Premium Emerald UI standard, including large overview cards and goals trackers.
  - Refined **Dashboard Components** with premium typography and standardized Rupiah formatting.
  - Implemented core pages with high-fidelity UI: Dashboard, Transfer, Bills, Login, and Onboarding (eKYC).
  - Implemented new functional pages with consistent UI and backend service mapping:
    - **QRIS Payments** (`/qris`)
    - **Virtual Card Management** (`/cards`)
    - **Financial Analytics & Intelligence** (`/analytics`)
    - **Wealth Management / Investments** (`/investments`)
    - **Security & MFA Governance** (`/security`)
    - **Account Settings & Ecosystem** (`/settings`)
    - **Help & Support Terminal** (`/support`)
  - Integrated `GEMINI.md` with official Frontend Design System rules and color palette.
  - Updated `TODOS.md` with detailed frontend implementation progress and upcoming tasks.
  - Fixed scroll issues, layout scaling, and bottom white-space gaps in the root layout.

### Added

- **Partner Service** (partner-service):
  - Initial implementation of Partner Management Service
  - Quarkus 3.x with Java 21 layered architecture
  - Domain models: Partner
  - Service layer: PartnerService
  - REST API: Partner CRUD endpoints
  - PostgreSQL integration with Hibernate Panache
  - Unit tests with TDD approach (Red-Green-Refactor)

- **Backoffice Dashboard**:
  - Implemented Next.js Dashboard for Backoffice operations
  - Features: KYC Review, Fraud Monitoring, Customer Operations
  - Integration with Backoffice Service REST API
  - Pages: Dashboard Overview, KYC List/Detail, Fraud List/Detail, Customer Cases List/Detail
  - E2E tests for Backoffice backend flow

- **Promotion Service** (promotion-service):
  - Initial implementation of promotion, rewards, cashback, referral, and loyalty points management
  - Quarkus 3.x with Java 21 layered architecture
  - Domain models: Promotion, Reward, Cashback, Referral, LoyaltyPoints
  - Service layer: PromotionService, RewardService, CashbackService, ReferralService, LoyaltyPointsService
  - REST API: Promotions, Rewards, Cashbacks, Referrals, Loyalty Points endpoints
  - PostgreSQL with Flyway migrations
  - Kafka event publishing for promotion, reward, cashback, referral, and loyalty events
  - Test resources for PostgreSQL and Kafka
  - Dockerfile with UBI9 for OpenShift deployment
  - Fixed LoyaltyPointsService.calculateCurrentBalance() to properly query database for current balance
  - Fixed LoyaltyPointsService.getBalance() to calculate real metrics from database
  - Added comprehensive unit and integration tests for all services and resources
  - Fixed database column mapping issues in domain entities (explicit @Column annotations for enums)
  - Test resources for PostgreSQL and Kafka using Testcontainers

- **Lending Service Enhancements** (lending-service):
  - Enhanced credit underwriting with multi-factor scoring:
    - KYC verification status integration (50 points for APPROVED, 25 for PENDING)
    - Account tenure scoring (up to 40 points for 3+ years)
    - Transaction history scoring based on volume, success rate, and transaction count
    - Maximum credit score cap at 850
  - Feign clients for Account and Transaction services integration
  - Personal Loan Repayment Schedule Management:
    - Automated repayment schedule generation using amortization formula
    - Per-installment tracking (principal, interest, outstanding balance)
    - Repayment processing with partial and full payment support
    - Status tracking (PENDING, PARTIALLY_PAID, FULLY_PAID, OVERDUE)
  - PayLater Transaction Management:
    - Purchase transaction recording with credit limit validation
    - Payment transaction processing with used/available credit updates
    - Transaction history retrieval with date ordering
  - New database tables:
    - `repayment_schedules` - Installment tracking with foreign key to loans
    - `paylater_transactions` - Transaction history with purchase/payment types
  - New domain models:
    - `RepaymentSchedule` - Installment tracking domain model
    - `PayLaterTransaction` - Transaction tracking domain model
  - New service classes:
    - `EnhancedCreditScoringService` - Multi-factor credit scoring
    - `LoanManagementService` - Repayment schedule management
    - `PayLaterTransactionService` - PayLater transaction processing
  - New controller endpoints:
    - `POST /api/v1/lending/loans/{loanId}/repayment-schedule` - Create repayment schedule
    - `GET /api/v1/lending/loans/{loanId}/repayment-schedule` - Get repayment schedules
    - `POST /api/v1/lending/repayment-schedules/{scheduleId}/pay` - Process repayment
    - `POST /api/v1/lending/paylater/{userId}/purchase` - Record purchase
    - `POST /api/v1/lending/paylater/{userId}/payment` - Record payment
    - `GET /api/v1/lending/paylater/{userId}/transactions` - Get transaction history
  - New DTOs for external service integration:
    - `UserResponse` - Account service user data
    - `TransactionResponse` - Transaction service transaction data
    - `TransactionSummaryResponse` - Transaction summary for credit scoring
  - Database migration V2 for new tables with proper indexes

### Added

- **Investment Service** (investment-service):
  - New Spring Boot 3.4 service for digital investments
  - Hexagonal architecture implementation with ports and adapters
  - Features: Digital Deposits, Mutual Funds Marketplace, Digital Gold
  - Investment account management with balance tracking
  - Database schema with investment_accounts, deposits, mutual_funds, gold_holdings, investment_transactions tables
  - Kafka event publishing for investment events (created, completed, failed)
  - Wallet service integration for balance management
  - Circuit breaker and retry patterns with Resilience4j
  - Unit tests with TDD approach (Red-Green-Refactor)
  - JaCoCo code coverage with 80% line and 70% branch thresholds

### Added

- **PCI-DSS & OJK Regulatory Compliance Audit Service** (compliance-service):
  - New Spring Boot 3.4 service for regulatory compliance auditing
  - PCI-DSS compliance checks for card data handling and security
  - OJK regulatory compliance for Indonesian financial operations
  - Audit report creation and retrieval APIs
  - Compliance check result tracking (PASS/FAIL/WARNING/NOT_APPLICABLE)
  - Audit report search by transaction ID and merchant ID
  - Database migration for audit_reports and compliance_checks tables
  - Unit tests with TDD approach (Red-Green-Refactor)
  - ArchUnit architecture tests for hexagonal architecture validation
  - JaCoCo code coverage with 59% line coverage

### Added

- **Production Monitoring & Alerting** (LokiStack/Prometheus):
  - Prometheus server (v2.54.1) with 15-day retention and alerting rules
  - Loki log aggregation (v2.9.10) with 744h (31 days) retention
  - Grafana dashboards (v11.1.4) with pre-built monitoring dashboards
  - Alertmanager (v0.27.0) with Slack and webhook notifications
  - Promtail (v2.9.10) for log collection from Docker containers
  - Configuration files in `infrastructure/docker/`:
    - `loki-config.yml` - Loki server configuration
    - `promtail-config.yml` - Log collection agent configuration
    - `prometheus-alerts.yml` - 33 alert rules for services, performance, transactions, databases, and infrastructure
    - `alertmanager-config.yml` - Alert routing to Slack/PagerDuty
    - Updated `prometheus.yml` - Service discovery for all PayU services
  - Grafana dashboards:
    - Service Health Dashboard - Request rate, error rate, response times, memory/CPU usage
    - Transaction Dashboard - Transaction volume, success rate, transaction value distribution
    - Infrastructure Dashboard - PostgreSQL, Redis, Kafka health and performance metrics
  - Logback XML configuration for structured JSON logging in account-service
  - Monitoring test suite (`tests/infrastructure/test_monitoring_alerting.py`):
    - 26 tests covering Prometheus, Loki, Grafana, Alertmanager, Promtail
    - Tests: Service availability, configuration loading, metrics scraping, alert rules, datasources, dashboards

- **Disaster Recovery Plan (DRP)** (`DISASTER_RECOVERY.md`):
  - Comprehensive backup and restore procedures for all PayU components
  - Recovery objectives: RTO < 15 min, RPO < 1 min (production)
  - Coverage: PostgreSQL (11 databases), Redis, Kafka, configuration files
  - Incident response procedures and communication templates
  - Environment-specific settings (dev, staging, production)

- **Backup Scripts** (`scripts/`):
  - `run_backup.sh` - Orchestration script for all backup operations
  - `backup_postgres.sh` - PostgreSQL logical and physical backups
  - `restore_postgres.sh` - PostgreSQL restore procedures
  - `backup_restore_redis.sh` - Redis snapshot backup and restore
  - `backup_restore_kafka.sh` - Kafka topic backup and restore
  - `verify_docker_compose.sh` - Docker infrastructure verification

   - **Backup-Restore Test Suite** (`tests/infrastructure/test_backup_restore.py`):
   - 31 tests covering backup scripts, documentation, and DRP scenarios
   - Tests: Script existence, syntax validation, DRP documentation content
   - Coverage: PostgreSQL, Redis, Kafka, orchestration, and DRP workflows
   - All 22 tests passing (9 tests skipped - require running infrastructure)

### Fixed
- **Backup Script Configuration**:
  - Added `BACKUP_ROOT` environment variable support to all backup scripts
  - Modified scripts: `backup_postgres.sh`, `backup_restore_redis.sh`, `backup_restore_kafka.sh`, `restore_postgres.sh`
  - Allows specifying custom backup directory via environment variable
  - Default location: `/backups` if `BACKUP_ROOT` not set
  - Fixed backup verification to use stdin for pg_restore (PostgreSQL)

- **Logging Output Redirection**:
  - Fixed log function in all backup scripts to output to stderr (`>&2`)
  - Prevents log messages from being captured in command output (e.g., `topics=($(list_topics))`)
  - Affected scripts: All backup scripts, `run_backup.sh`, `restore_postgres.sh`

- **Kafka Topic List Filtering**:
  - Fixed `backup_restore_kafka.sh` to filter "Listing" header line from topic list
  - Prevents log messages from being treated as topic names during backup

- **Verification Script** (`scripts/verify_backup_restore.sh`):
  - Comprehensive verification script for backup/restore functionality
  - Tests: Docker/docker-compose availability, script syntax, DRP documentation
  - Infrastructure tests: PostgreSQL backup/restore, Redis backup, Kafka backup
  - Generates test report with pass/fail/skip counts

 - **E2E Tests for KYC Service** (`backend/kyc-service/tests/e2e/`):
  - `test_kyc_workflow.py` - Complete KYC verification workflow tests
  - Tests: Start verification, KTP upload, selfie upload, status retrieval
  - Test scenarios: Success case, liveness failure, face match failure
  - Mock services: OCR, Liveness, Face Matching, Dukcapil, Kafka

- **E2E Tests for Analytics Service** (`backend/analytics-service/tests/e2e/`):
  - `test_analytics_workflow.py` - Complete analytics workflow tests
  - Tests: User metrics, spending trends, cash flow analysis, recommendations
  - Test scenario: Complete user journey with analytics integration

- **Unit Tests for Both Services**:
  - KYC Service unit tests (`backend/kyc-service/tests/unit/test_services.py`)
  - Analytics Service unit tests (`backend/analytics-service/tests/unit/test_services.py`)
  - Coverage: OCR, Liveness, Face Matching, Dukcapil, Recommendation Engine

- **Test Infrastructure**:
  - `pyproject.toml` for both services with pytest configuration
  - `conftest.py` with shared fixtures
  - `docker-compose.test.yml` - Complete test environment setup
    - PostgreSQL for KYC Service (port 5433)
    - TimescaleDB for Analytics Service (port 5434)
    - Kafka + Zookeeper (port 9092)
    - Dukcapil Simulator (port 8091)
    - KYC Service (port 8007)
    - Analytics Service (port 8008)
  - `run_tests.sh` - Automated test runner script

- **Billing Service Integration Tests** (Quarkus + Testcontainers):
  - `BillingIntegrationTest.java` - Integration tests for payment creation and event publishing
  - `PostgresTestResource.java` & `KafkaTestResource.java` - Quarkus TestResourceLifecycleManager for containers
  - Added Testcontainers (PostgreSQL, Kafka) and Awaitility dependencies
  - Mocked `WalletClient` for integration scenarios

- **Wallet Service Ledger Implementation** (Spring Boot 3.4):
  - Added `LedgerEntry` domain model and JPA entity
  - Implemented automatic ledger recording for balance change operations
  - New API Endpoints:
    - `GET /wallets/{walletId}/ledger` - Get ledger entries for a wallet

- **Docker Compose Infrastructure Verification**:
  - `tests/infrastructure/test_docker_infrastructure.py` - Pytest tests for docker-compose up/down operations
  - `tests/infrastructure/test_docker_compose_verification.py` - Standalone Python verification script
  - `scripts/verify_docker_compose.sh` - Shell script for manual infrastructure verification
  - Tests verify: service startup, health checks, database connectivity, Kafka, Redis, Keycloak, microservices accessibility
  - Validates all 17 required services are running and healthy
  - Verifies 11 databases are created in PostgreSQL
  - Verifies clean shutdown and removal of all containers
    - `GET /wallets/ledger/transaction/{transactionId}` - Get ledger entries by transaction ID
  - Flyway migration `V3__create_ledger_entries_table.sql` for ledger persistence
  - Updated `WalletController`, `WalletService`, and persistence adapters

- **Frontend Development Skill** (`.agent/skills/frontend-development/SKILL.md`):
  - Expert guidance for Next.js 15 web application development
  - React Native (Expo) mobile development best practices
  - Material UI / shadcn/ui design patterns for financial apps
  - State management (Zustand, TanStack Query) standards

- **Service Hardening & Documentation**:
  - Added `.dockerignore` files for all major services
  - OpenApi documentation configuration for Transaction and Wallet services
  - Structured logging configuration (`logback-spring.xml`) for Spring Boot services

- **KYC Service (FastAPI 0.115.0 + Python 3.12)**:
  - Full eKYC implementation with OCR, liveness detection, and face matching
  - **OCR Service**: PaddleOCR for Indonesian KTP scanning with confidence scoring
  - **Liveness Detection**: Computer vision-based anti-spoofing (eye openness, mouth movement, head pose)
  - **Face Matching**: Cosine similarity-based KTP vs selfie comparison
  - **Dukcapil Integration**: Real-time NIK verification with external simulator
  - **Database**: PostgreSQL with asyncpg and SQLAlchemy 2.0
  - **Kafka Producer**: Events for KYC status updates (verified/failed/ktp_uploaded)
  - **API Endpoints**:
    - `POST /api/v1/kyc/verify/start` - Start new verification
    - `POST /api/v1/kyc/verify/ktp` - Upload KTP for OCR
    - `POST /api/v1/kyc/verify/selfie` - Upload selfie for verification
    - `GET /api/v1/kyc/verify/{id}` - Get verification status
    - `GET /api/v1/kyc/user/{user_id}` - Get user KYC history
  - **Dockerfile**: Red Hat UBI9 Python 3.12 minimal base image
  - **Monitoring**: Prometheus metrics, OpenTelemetry tracing, structured JSON logs

- **Analytics Service (FastAPI 0.115.0 + Python 3.12)**:
  - Time-series analytics with TimescaleDB (PostgreSQL extension)
  - **Kafka Consumer**: Real-time event consumption from wallet/transaction/KYC topics
  - **Hypertables**: Automatic partitioning for transactions, wallet balances, user activities
  - **User Metrics**: Total transactions, amount, average, account age, KYC status
  - **Spending Insights**:
    - Spending trends by category with month-over-month analysis
    - Top merchant identification
    - Cash flow analysis (income vs expenses)
  - **ML Recommendations Engine**:
    - Savings goal suggestions
    - Budget alerts for category overruns
    - Spending trend notifications
    - Inactivity reminders
    - Investment suggestions
  - **API Endpoints**:
    - `GET /api/v1/analytics/user/{user_id}/metrics` - User metrics
    - `POST /api/v1/analytics/spending/trends` - Spending patterns
    - `POST /api/v1/analytics/cashflow` - Cash flow analysis
    - `GET /api/v1/analytics/user/{user_id}/recommendations` - ML recommendations
  - **Dockerfile**: Red Hat UBI9 Python 3.12 minimal base image
  - **Monitoring**: Prometheus metrics, OpenTelemetry tracing, structured JSON logs

- **Wallet Service Kafka Integration Tests** (Testcontainers):
  - `WalletKafkaIntegrationTest.java` - 7 test cases for Kafka event publishing
  - Tests topics: `wallet.created`, `wallet.balance.changed`, `wallet.balance.reserved`, `wallet.reservation.committed`, `wallet.reservation.released`
  - Created missing port interfaces: `WalletEventPublisherPort`, `WalletPersistencePort`, `CardPersistencePort`

- **Transaction Service Kafka Integration Tests** (Testcontainers):
  - `TransactionKafkaIntegrationTest.java` - 10 test cases for Kafka event publishing
  - Tests topics: `payu.transactions.initiated`, `payu.transactions.validated`, `payu.transactions.completed`, `payu.transactions.failed`
  - Lightweight Kafka-only testing without Spring context

- **QA Expert Skill Update** (`.agent/skills/qa-expert/SKILL.md`):
  - PayU-specific testing patterns (Testcontainers, Kafka, Hexagonal Architecture)
  - Financial transaction test requirements (idempotency, BigDecimal, saga compensation)
  - Test data patterns and test user accounts
  - Coverage thresholds (80% line, 70% branch)
  - P0-P3 test priority guidelines

- **Auth Service Integration Tests** (Testcontainers + Keycloak):
  - `AuthIntegrationTest.java` - 6 test cases for authentication flow
  - Uses `testcontainers-keycloak` to spin up real Keycloak 26.0 instance
  - Tests: container running, endpoint accessibility, invalid credentials, non-existent user, direct Keycloak token, account lockout
  - Added `SecurityConfig.java` to allow public access to login endpoints
  - Fixed `KeycloakService.login()` to use `BodyInserters.fromFormData()` for proper form encoding
  - Added Testcontainers dependencies (`junit-jupiter`, `testcontainers-keycloak`, `rest-assured`)
  - Configured maven-surefire-plugin and maven-failsafe-plugin for integration test separation

- **ArchUnit Tests for Quarkus Services**:
  - `billing-service/ArchitectureTest.java` - Layered architecture, naming conventions, domain isolation
  - `notification-service/ArchitectureTest.java` - Sender abstraction pattern enforcement
  - Added `archunit-junit5:1.2.1` dependency to both services

- **JaCoCo Coverage for Quarkus Services**:
  - Added `quarkus-jacoco` extension to billing-service
  - Added `quarkus-jacoco` extension to notification-service

- **Flyway Migrations for Quarkus Services**:
  - `V1__create_bill_payments_table.sql` for billing-service
  - `V1__create_notifications_table.sql` for notification-service
  - Proper indexes and constraints for performance

- **Domain Exception Hierarchies**:
  - `AccountDomainException` with ACCT_xxx_xxx error codes (VAL, BUS, EXT, SYS)
  - `TransactionDomainException` with TXN_xxx_xxx error codes (VAL, BUS, BAL, EXT, SYS)
  - `AuthDomainException` with AUTH_xxx_xxx error codes (VAL, BUS, EXT, SYS)
  - Updated GlobalExceptionHandlers to use domain exceptions
  - Indonesian user-friendly error messages

- **Gateway Service Test Suite** (New):
  - `ArchitectureTest.java` - Layered architecture, naming conventions, Quarkus/Jakarta rules
  - `CorrelationIdFilterTest.java` - 7 test cases for ID generation and propagation
  - `HealthResourceTest.java` - Integration tests for health endpoints
  - Added `archunit-junit5:1.2.1`, `quarkus-junit5-mockito`, `quarkus-jacoco` dependencies

- **Unit Tests for Quarkus Service Layers**:
  - `PaymentServiceTest` - 6 test cases (payment creation, wallet integration, admin fees)
  - `NotificationServiceTest` - 8 test cases (multi-channel, failure handling)

- **Dockerfile Standardization (UBI9 + Multi-stage)**:
  - All services now use `registry.access.redhat.com/ubi9/openjdk-21:1.20` for build
  - All services now use `registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20` for runtime
  - Multi-stage builds for smaller and more secure images
  - Consistent JVM tuning (G1GC, MaxRAMPercentage, HeapDumpOnOutOfMemoryError)
  - Non-root user (185 - jboss) for security
  - Health checks for all services
  - Services updated: account, auth, transaction, wallet, billing, notification, gateway, simulators

- **Container Specialist Skill** (`.agent/skills/container-specialist/SKILL.md`):
  - Mandatory UBI9 base image requirements
  - Multi-stage build templates (Spring Boot, Quarkus Fast-JAR, Quarkus Native)
  - Non-root user enforcement
  - Label requirements (maintainer, description, version)
  - Health check patterns for Spring Boot and Quarkus
  - JVM container-aware settings
  - Security best practices (no secrets, .dockerignore, pinned versions)
  - Port assignments for all services
  - Verification checklist

- **Security Standards Enhancement** (code-review SKILL.md):
  - PCI-DSS compliance checklist for payment systems
  - OJK (Indonesian Financial Regulations) compliance checks
  - Secrets management guidelines (Vault, OpenShift Secrets)
  - Audit logging requirements with mandatory fields
  - Sensitive data handling (PII classification and masking)

- **Testing Standards & Coverage Thresholds**:
  - JaCoCo coverage thresholds enforced via Maven (80% line, 70% branch)
  - Per-class minimum coverage (60%) with exclusions for DTOs/configs
  - Event-driven testing patterns (saga compensation, idempotency, DLQ)
  - Performance testing guidelines with Gatling/JMeter thresholds

- **ArchUnit Rules Enhancement** (account-service):
  - Domain isolation rules (domain must not depend on infrastructure)
  - Service access rules (controllers cannot access repositories directly)
  - Repository access rules
  - No field injection enforcement (@Autowired/@Inject on fields prohibited)
  - Naming convention rules (Service, Controller, Repository suffixes)
  - Exception handling rules (domain exceptions must extend RuntimeException)

- **Error Handling Taxonomy** (payu-development SKILL.md):
  - Error code structure: `[DOMAIN]_[CATEGORY]_[SPECIFIC]`
  - Domain prefixes: AUTH (4xxx), ACCT (5xxx), TXN (6xxx), INTG (7xxx), SYS (9xxx)
  - Complete error code tables for all domains
  - Resilience patterns: Retry, Circuit Breaker, Bulkhead with Resilience4j configs

- **Development Workflow Documentation**:
  - Created `CONTRIBUTING.md` with comprehensive workflow guidelines
  - Trunk-Based Development branching strategy
  - Conventional Commits format (feat, fix, docs, refactor, test, chore)
  - Pull Request process with size guidelines and approval matrix
  - Definition of Done (DoD) checklist
  - CI/CD pipeline stages (Build → Test → Scan → Deploy)
  - Quality gates with thresholds

- **PR Template** (`.github/pull_request_template.md`):
  - Structured checklist for code quality, testing, documentation
  - Security checklist (secrets, PII, input validation)
  - Database migration checklist
  - Service selection for affected components

- **External Service Simulators Documentation** (payu-development SKILL.md):
  - BI-FAST, Dukcapil, QRIS simulator guides
  - Test accounts and NIKs for different scenarios
  - Simulator configuration (latency, failure rates)
  - Integration testing patterns with Testcontainers
  - Contract testing examples with PACT
  - Failure scenario testing patterns

- **Observability & Monitoring Standards** (payu-development SKILL.md):
  - Structured JSON logging format with correlation IDs
  - Distributed tracing with OpenTelemetry/Jaeger
  - SLI/SLO definitions (99.9% availability, P95 < 200ms)
  - Micrometer/Prometheus metrics (business + technical)
  - Alerting rules (critical P1/P2, warning P3)
  - Error budget calculations

- **Database Migration Guidelines** (payu-development SKILL.md):
  - Flyway naming convention: `V{version}__{description}.sql`
  - Migration best practices and structure
  - Backup & recovery strategy (RTO 4hr, RPO 5min)
  - Indexing guidelines and query optimization
  - Anti-patterns to avoid

- **Billing Service** (Quarkus 3.17 Native):
  - Bill payments for PLN, PDAM, Pulsa, BPJS, etc.
  - REST API: `/api/v1/billers`, `/api/v1/payments`
  - Integration with wallet-service for balance debit
  - Kafka events for payment notifications
  - Hibernate Panache ORM with PostgreSQL

- **Notification Service** (Quarkus 3.17 Native):
  - Multi-channel: Email, SMS, Push, In-App notifications
  - REST API: `/api/v1/notifications`
  - Kafka consumers for wallet, transaction, payment events
  - Quarkus Mailer integration for emails
  - Sender abstraction (EmailSender, SmsSender, PushSender)

- **Wallet Service** (Spring Boot 3.4.1 - Hexagonal Architecture):
  - Domain Layer: `Wallet`, `WalletTransaction`, and `Card` models
  - Ports: `WalletUseCase`, `CardUseCase` (input), `WalletPersistencePort`, `CardPersistencePort` (output)
  - Adapters: JPA persistence, REST controller, Kafka event publisher
  - Balance management: get balance, reserve, commit, release, credit
  - **Virtual Debit Card**: Create, list, freeze/unfreeze virtual cards
  - Flyway database migrations for wallet and cards tables
  - Unit tests (WalletServiceTest), Controller tests, ArchUnit architecture tests

- **Gateway Service** (Updated):
  - Added routing for all microservices (`/api/v1/accounts`, `/wallets`, `/transactions`, `/billers`, `/notifications`)
  - Configured proxy logic with Vert.x WebClient
  - Removed outdated dependencies and fixing build configuration

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
