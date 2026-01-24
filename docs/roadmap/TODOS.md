# Project Roadmap & Todo List

## 🚀 Next Priorities

### 1. Compliance Service Implementation
- [x] Implement `compliance-service` (currently empty directory).
- [x] Focus on AML (Anti-Money Laundering) and CFT (Combating the Financing of Terrorism) procedures as per PRD.
- [x] Integrate with `transaction-service` for real-time monitoring.

### 2. Frontend-Backend API Integration
- [x] Connect `frontend/web-app` with `gateway-service` for all core flows.
- [x] Implement API client/services in frontend using defined backend specifications.
- [x] Handle JWT authentication and refresh tokens in web-app.
- [x] Implement real-time updates via WebSocket/Kafka for dashboards (Completed: Full implementation with event filtering and subscriptions).

### 3. Investment & Wealth Management
- [x] Implement `investment-service` (Java Spring Boot - Hexagonal).
- [x] Features: Digital Deposits, Mutual Funds marketplace, Digital Gold.

### 4. Lending & Credit
- [x] Implement `lending-service` (Java Spring Boot - Hexagonal).
- [x] Features: Personal Loan, PayLater (Buy Now Pay Later), automated Credit Underwriting.

### 5. Marketing & Engagement
- [x] Implement `promotion-service` (Java Quarkus - Layered).
- [x] Features: Rewards points, Cashback engine, Referral program.

### 6. Operational Excellence
- [x] Implement `backoffice-service` (Java Quarkus - Layered).
- [x] Features: Dashboard for Manual KYC review, Fraud Monitoring, and Customer Operations.

### 7. Ecosystem & Open Banking
- [x] Implement `partner-service` (Java Quarkus - Layered).
- [x] Features: SNAP BI Standard API integration, Merchant Portal for B2B.

## 🎨 Frontend & Integration (Primary Focus)

### 8. Frontend/Web-App Core Implementation
- [x] Complete UI design system (Premium Emerald rules in `docs/guides/GEMINI.md`).
- [x] **Localization (Bahasa Indonesia)**: Translated all pages and components.
- [x] Implement missing pages with consistent UI:
    - [x] **QRIS Payments** (`/qris`) -> Integrate with `transaction-service`.
    - [x] **Virtual Card** (`/cards`) -> Integrate with `account-service` / `wallet-service`.
    - [x] **Investments** (`/investments`) -> Integrate with `investment-service`.
    - [x] **Financial Analytics** (`/analytics`) -> Integrate with `analytics-service`.
    - [x] **Security & MFA** (`/security`) -> Integrate with `auth-service`.
    - [x] **Account Settings** (`/settings`) -> Integrate with `account-service`.
    - [x] **Help & Support** (`/support`) -> Integrate with `support-service`.
- [x] Implement full API integration via `gateway-service` for all core flows.
- [x] Optimize state management (Zustand) and Data fetching (React Query).
- [x] **Technical Debt & Polish**:
    *   [x] Standardize **Skeleton Loaders** for all data-fetching states across all pages.
    *   [x] Implement **Dynamic Error Boundaries** with user-friendly Indonesian recovery UI.
    *   [x] Add **Micro-animations** (Framer Motion) for page transitions and button interactions.
    *   [x] Optimize **SEO Metadata** and OpenGraph tags for banking security (no-index on sensitive pages).
- [x] **Feature Enhancements**:
    *   [x] **Transfer Evolution**: Add selection for BI-FAST, SKN/RTGS, and Real-time Scheduled transfers.
    *   [x] **Live Analytics**: Integrate WebSocket for real-time portfolio updates in `/analytics`.
    *   [x] **Shared Pockets**: Implement UI for joint savings pockets with member management.
- [x] **Missing Core Modules**:
    *   [x] **Lending & Credit Hub** (`/lending`): UI for Personal Loans application and PayLater management.
    *   [x] **Rewards & Gamification** (`/rewards`): Implement Points dashboard, Cashback history, and Referral engine.
- [x] **Integrated Service Wiring**:
    *   [x] **CMS Integration**: Connect `frontend/web-app` to `cms-service` for dynamic banners and emergency alerts.
    *   [x] **A/B Testing Wiring**: Implement `useExperiment` hook in frontend to consume variants from `ab-testing-service`.
    *   [x] **Customer Segmentation**: Display personalized offers based on user segment fetched from `promotion-service`.

### 9. Mobile App Support
- [x] Prepare mobile-responsive views for web-app.
- [x] **Transition to Expo (React Native)**:
    - [x] Remove current Native (Swift/Kotlin) boilerplate from `mobile/` (Inconsistent with stack).
    - [x] Initialize new Expo project with TypeScript and Expo Router.
    - [x] Implement core banking flows (Onboarding, Transfer, QRIS) using React Native.
    - [x] Target: Cross-platform iOS & Android from single codebase.

### 26. Mobile App Feature Parity (React Native)
- [x] **Core Banking Implementation**:
    - [x] Build **Dashboard** with balance overview and quick actions.
    - [x] Build **Transfer** flow with recipient search and BI-FAST support.
    - [x] Build **QRIS Scanner** with dynamic payment handling.
    - [x] Build **Virtual Cards** management (Freeze, Unfreeze, Show CVV).
- [x] **Enterprise Integration**:
    - [x] Integrate **In-app Feedback Widget** for mobile bug reporting.
    - [x] Integrate **A/B Testing Hook** to toggle mobile features.
    - [x] Implement **Push Notifications** via Expo Notifications (Firebase/APNs).
    - [x] Add **Biometric Auth** (FaceID/Fingerprint) for transaction signing.

## 🛡️ Backend Technical Debt & Hardening

### 10. Security Hardening
- [x] Replace `permitAll()` with production-ready OAuth2/Keycloak configuration in all services.
- [x] Implement strict CORS and rate limiting in `gateway-service`.
- [x] Audit user data access patterns for GDPR compliance.
- [x] **Vault Integration**: Migrate hardcoded secrets to HashiCorp Vault.

### 11. SNAP BI & Partner Integration
- [x] Transition `partner-service` from mock to real SNAP BI standard implementation.
- [x] Implement signature validation and certificate management for partners.
- [x] **TokoBapak Integration**:
    - [x] Implement `/v1/partner/auth/token` for TokoBapak authentication.
    - [x] Implement `/v1/partner/payments` (Create & Status).
    - [x] Implement Webhook system for `payment.completed` notifications.

## ⚙️ Operations & Infrastructure (OpenShift)

### 12. CI/CD & Automation
- [x] Implement **Tekton Pipelines** for automated builds and testing.
- [x] Setup **ArgoCD** for GitOps-based deployment to OpenShift.
- [x] Configure multi-stage Docker builds using Red Hat UBI9 images.

### 13. Observability & SRE
- [x] Deploy **LokiStack** for centralized log management.
- [x] Configure **Prometheus & Grafana** dashboards for all microservices.
- [x] Implement distributed tracing using **Jaeger/OpenTelemetry**.
- [x] **Disaster Recovery**: Verify backup-restore procedures for PostgreSQL and Kafka.

### 14. Legal & Readiness
- [x] Create placeholders for **Terms of Service** and **Privacy Policy**.
- [x] Prepare technical documentation for OJK/BI regulatory audit.

## 🧪 Testing & Quality Assurance

### 15. Testing Suite (Existing)
- [x] **Cross-Service Integration Tests**: Implement holistic End-to-End test suite covering full user journeys.
- [x] **TDD Hardening**: Increase test coverage to >80% for all core banking services.
- [x] **Frontend Quality**:
    *   [x] Setup **Playwright** for E2E testing of critical financial flows (KYC, Transfer, Bill Pay).
    *   [x] Setup **Vitest** for unit testing critical logic in `src/services` and `src/stores`.
- [x] [SKIP] Load Testing: (Postponed as per user request).
- [x] [SKIP] Security Testing: (Postponed as per user request).

### 36. Comprehensive Testing Infrastructure (NEW)
- [ ] **Docker Compose Test Environment**:
    - [ ] Verify `docker-compose up -d` starts all 20+ services successfully.
    - [ ] Add **health check script** to validate all services are healthy before running tests.
    - [ ] Create `docker-compose.test.yml` for isolated test environment with clean databases.
    - [ ] Add **test data seeding** script (`scripts/seed-test-data.sh`).
    - [ ] Configure **test user accounts** with known credentials for automation.
    - [ ] Add **cleanup script** to reset databases between test runs.

- [ ] **Backend Unit Tests (Local)**:
    - [ ] Verify all `account-service` tests pass: `cd backend/account-service && mvn test`.
    - [ ] Verify all `auth-service` tests pass: `cd backend/auth-service && mvn test`.
    - [ ] Verify all `transaction-service` tests pass: `cd backend/transaction-service && mvn test`.
    - [ ] Verify all `wallet-service` tests pass: `cd backend/wallet-service && mvn test`.
    - [ ] Verify all `billing-service` tests pass: `cd backend/billing-service && ./mvnw test`.
    - [ ] Verify all `notification-service` tests pass: `cd backend/notification-service && ./mvnw test`.
    - [ ] Verify all `gateway-service` tests pass: `cd backend/gateway-service && ./mvnw test`.
    - [ ] Verify all `kyc-service` tests pass: `cd backend/kyc-service && pytest`.
    - [ ] Verify all `analytics-service` tests pass: `cd backend/analytics-service && pytest`.
    - [ ] Create **test coverage report** for each service (JaCoCo/coverage.py).
    - [ ] Enforce **minimum 80% coverage** threshold in CI/CD.

- [ ] **Backend Integration Tests (Docker)**:
    - [ ] Run `account-service` integration tests with Testcontainers (PostgreSQL, Kafka).
    - [ ] Run `auth-service` integration tests with Keycloak Testcontainer.
    - [ ] Run `transaction-service` integration tests with PostgreSQL Testcontainer.
    - [ ] Run `wallet-service` integration tests verifying Kafka event publishing.
    - [ ] Run `billing-service` integration tests with mocked wallet-service.
    - [ ] Verify **inter-service communication** between gateway and backend services.
    - [ ] Test **Kafka event flow**: transaction → wallet → notification.
    - [ ] Test **database migrations** (Flyway) run successfully on fresh DB.

- [ ] **API Contract Tests (Postman/Newman)**:
    - [ ] Create **Postman collection** for all API endpoints.
    - [ ] Add **environment files** for local, docker, and staging.
    - [ ] Run `newman run` against docker-compose environment.
    - [ ] Validate **OpenAPI specs** match actual API responses.
    - [ ] Test **authentication flows** (login, token refresh, logout).
    - [ ] Test **error responses** match documented error codes.

- [ ] **E2E Tests - Full User Journeys (Docker)**:
    - [ ] **User Onboarding**: Register → eKYC → Wallet Creation.
    - [ ] **Transfer Flow**: Login → Check Balance → Transfer → Verify Debit.
    - [ ] **Bill Payment**: Login → Select Biller → Pay → Verify Transaction.
    - [ ] **QRIS Payment**: Scan QR → Confirm → Pay → Verify.
    - [ ] **Investment Journey**: Open Account → Deposit → View Portfolio.
    - [ ] **Lending Journey**: Check Eligibility → Apply Loan → View Status.
    - [ ] Create **Playwright test suite** targeting docker-compose frontend.
    - [ ] Generate **E2E test report** with screenshots on failure.

- [ ] **Frontend Tests (Local)**:
    - [ ] Run `cd frontend/web-app && npm run test` for Vitest unit tests.
    - [ ] Run `cd frontend/web-app && npm run test:e2e` for Playwright E2E.
    - [ ] Verify **type checking** passes: `npm run type-check`.
    - [ ] Verify **lint** passes: `npm run lint`.
    - [ ] Verify **build** succeeds: `npm run build`.

- [ ] **Mobile Tests (Local)**:
    - [ ] Run `cd mobile && npm run test` for Jest unit tests.
    - [ ] Run `cd mobile && npm run lint` for ESLint checks.
    - [ ] Verify **TypeScript compilation**: `npx tsc --noEmit`.
    - [ ] Test on **iOS Simulator** via `npm run ios`.
    - [ ] Test on **Android Emulator** via `npm run android`.
    - [ ] Manual testing of **biometric authentication** flows.
    - [ ] Manual testing of **push notification** handling.

- [ ] **Regression Test Suite (Automated)**:
    - [ ] Create `tests/regression/` directory structure.
    - [ ] Implement **nightly regression** run via cron/GitHub Actions.
    - [ ] Test **backward compatibility** of API changes.
    - [ ] Test **database rollback** scenarios.
    - [ ] Generate **regression report** with pass/fail summary.

- [ ] **Performance Smoke Tests (Docker)**:
    - [ ] Run **basic load test** (50 users) against docker-compose.
    - [ ] Verify **response times** < 500ms for critical endpoints.
    - [ ] Verify **no memory leaks** after 1000 requests.
    - [ ] Verify **database connection pooling** works under load.
    - [ ] Generate **performance baseline** metrics.

### 37. Test Automation Scripts
- [ ] Create `scripts/run-all-tests.sh`:
    - [ ] Start docker-compose if not running.
    - [ ] Wait for all services to be healthy.
    - [ ] Run backend unit tests.
    - [ ] Run backend integration tests.
    - [ ] Run API contract tests.
    - [ ] Run E2E tests.
    - [ ] Generate consolidated test report.
    - [ ] Exit with proper error code for CI/CD.
- [ ] Create `scripts/test-single-service.sh <service-name>`:
    - [ ] Run tests for specified service only.
    - [ ] Support both Java (Maven) and Python (pytest) services.
- [ ] Create `Makefile` with test targets:
    - [ ] `make test` - Run all tests.
    - [ ] `make test-unit` - Unit tests only.
    - [ ] `make test-integration` - Integration tests only.
    - [ ] `make test-e2e` - E2E tests only.
    - [ ] `make test-coverage` - Generate coverage report.

### 38. CI/CD Test Integration
- [ ] **GitHub Actions Workflow**:
    - [ ] Create `.github/workflows/test.yml` for PR testing.
    - [ ] Run unit tests on every push.
    - [ ] Run integration tests on PR to main.
    - [ ] Run E2E tests nightly.
    - [ ] Block merge if tests fail.
- [ ] **Test Reports**:
    - [ ] Publish **JUnit XML** reports to GitHub Actions.
    - [ ] Publish **coverage reports** to Codecov/SonarQube.
    - [ ] Display **test badge** in README.
    - [ ] Send **Slack notification** on test failure.


## 🚀 Backend Feature Extensions (Gap Analysis)

### 16. Transaction & Payment Enhancements
- [x] Implement **SKN/RTGS** transfer types in `transaction-service` (currently dummy).
- [x] Implement **Scheduled & Recurring Transfers** engine (logic + scheduler).
- [x] Implement **Split Bill** logic and notification flow across multiple users.

### 17. Expanded Billing & Top-Up
- [x] Implement **E-wallet Top-up** (GoPay, OVO, DANA, LinkAja) in `billing-service`.
- [x] Expand biller list to include **TV Cable** (Indovision, etc.) and **Multifinance** (Cicilan).

### 18. Advanced Financial Services
- [x] Implement **Robo-Advisory** engine in `analytics-service` for automated portfolio allocation.
- [x] Implement **Loan Pre-approval** logic in `lending-service` based on real-time credit scoring.
- [x] Implement **Gamification System** (Daily check-in rewards, transaction-based badges/levels) in `promotion-service`.

## 🔮 Phase 3: Future Enterprise Capabilities

### 19. Multi-Tenancy & Platform Scale
- [x] Implement **SaaS Multitenancy** (Tenant ID isolation at DB and Gateway levels).
- [x] Implement **Universal Search** in Backoffice for cross-service data lookup.
- [x] Implement **Data Archival Strategy** for handling billions of transaction records.

### 20. Advanced Security & AI
- [x] Implement **Real-time AI Fraud Detection** Scoring for all transactions.
- [x] Implement **Biometric Edge Authentication** bridge for Mobile App.
- [x] Implement **Dynamic Risk-based MFA** (trigger MFA only for suspicious login patterns).

### 21. Global Readiness
- [x] Implement **Internationalization (i18n)** support for English.
- [x] Support for **Multi-currency Pockets** and real-time FX (Foreign Exchange) engine.

### 22. Developer Experience & Partner Portal
- [x] Implement **Centralized API Portal** (Swagger/OpenAPI aggregator for all services).
- [x] Implement **Partner Sandbox Environment** with mock data and simulated latencies.
- [x] Build **Developer Documentation Site** with integration guides and SDK examples.

### 23. Advanced Customer Services
- [x] Implement **E-Statement Engine** (Generate monthly transaction PDFs).
- [x] Implement **Web Accessibility (A11y)** compliance as per OJK/WCAG standards.
- [x] Implement **In-app Feedback System** with screenshot/log attachment capabilities.

### 24. CMS & Marketing Automation
- [x] Implement **Dynamic Content Management (CMS)** for banners, promos, and alerts (Full implementation in `cms-service`).
- [x] Implement **A/B Testing Framework** for UI features and promotional offers (Full implementation in `ab-testing-service`).
- [x] Integrate **Customer Segmentation Engine** for personalized notification campaigns.

## 🛠️ Operational Polish & Stability

### 22. Infrastructure Hardening
- [x] Optimize **Docker Resource Limits** (CPU/RAM) across all 20+ containers.
- [x] Implement **Automated Regression Testing** in CI/CD (Tekton pipelines in `infrastructure/pipelines/`).
- [x] Fine-tune **Inter-service Health Checks** for faster recovery in OpenShift.
- [x] Deploy **Red Hat OpenShift Service Mesh (Istio)** for mTLS and advanced traffic management.
- [x] Implement **Distributed Caching Strategy** (Redis/Data Grid) with stale-while-revalidate patterns.

### 25. High Availability & Scalability
- [x] Implement **Database Sharding/Partitioning** for `transaction-service` to support billions of records.
- [x] Setup **Multi-region Active-Passive Failover** configuration manifests for OpenShift.
- [x] Execute **Performance Load Testing** (Gatling/JMeter) to identify microservice bottlenecks.

---

## 📱 Phase 4: Mobile & Frontend Evolution

### 27. Mobile App (Expo) - Core Implementation
- [x] **Project Setup**:
    - [x] Initialize Expo project with `npx create-expo-app mobile --template tabs`.
    - [x] Configure TypeScript, ESLint, and Prettier.
    - [x] Setup NativeWind (TailwindCSS) for styling consistency.
    - [x] Configure Expo Router for file-based navigation.
- [x] **Authentication Flow**:
    - [x] Build **Login Screen** with phone number + OTP.
    - [x] Build **Registration Screen** with progressive form.
    - [x] Implement **JWT Token Management** with `expo-secure-store`.
    - [x] Add **Biometric Authentication** (FaceID/TouchID) via `expo-local-authentication`.
- [x] **Dashboard & Home**:
    - [x] Build **Home Screen** with balance card and quick actions.
    - [x] Implement **Pull-to-Refresh** for balance updates.
    - [x] Display **Recent Transactions** list with pagination.
    - [x] Add **Notification Bell** with unread count badge.
- [x] **Transfer Flow**:
    - [x] Build **Recipient Selection** screen with contact search.
    - [x] Build **Amount Input** with IDR formatting and keyboard.
    - [x] Implement **Transfer Type Selection** (BI-FAST, SKN, RTGS).
    - [x] Build **Review & Confirm** screen with PIN/Biometric.
    - [x] Display **Success/Failure** screen with share receipt.
- [x] **QRIS Payments**:
    - [x] Implement **QR Scanner** using `expo-camera`.
    - [x] Parse QRIS dynamic/static codes.
    - [x] Build **Payment Review** screen.
    - [x] Handle **Offline QR** caching.
- [x] **Bill Payments**:
    - [x] Build **Biller Category** grid (PLN, Pulsa, Internet, etc.).
    - [x] Build **Bill Inquiry** form with customer ID input.
    - [x] Display **Bill Details** and payment confirmation.
- [x] **Virtual Cards**:
    - [x] Display **Card List** with flip animation.
    - [x] Implement **Show/Hide CVV** with biometric verification.
    - [x] Add **Freeze/Unfreeze** toggle.
    - [x] Build **Card Settings** (limits, notifications).
- [x] **Profile & Settings**:
    - [x] Build **Profile Screen** with avatar and user info.
    - [x] Implement **Change PIN** flow.
    - [x] Add **Notification Preferences** toggles.
    - [x] Build **Language Selector** (ID/EN).
    - [x] Add **App Version** and support links.

### 28. Mobile App - Advanced Features
- [x] **Push Notifications**:
    - [x] Configure `expo-notifications` with Firebase Cloud Messaging.
    - [x] Handle **Transaction Alerts** notifications.
    - [x] Handle **Promotional** notifications from CMS.
    - [x] Implement **Notification History** screen.
- [x] **Offline Support**:
    - [x] Implement **Offline Detection** with user feedback.
    - [x] Cache **Recent Transactions** locally.
    - [x] Queue **Pending Transfers** for retry.
- [x] **Security Features**:
    - [x] Implement **App Lock** when backgrounded.
    - [x] Add **Screenshot Prevention** for sensitive screens.
    - [x] Implement **Jailbreak/Root Detection** warning.
    - [x] Add **Session Timeout** with re-authentication.
- [x] **Analytics & Feedback**:
    - [x] Integrate **In-app Feedback Widget**.
    - [x] Track **Screen Views** with analytics service.
    - [x] Implement **Crash Reporting** integration.

### 29. Frontend Web-App Enhancements
- [x] **CMS Integration**:
    - [x] Create `useCmsContent` hook to fetch banners/promos from `cms-service`.
    - [x] Build **Banner Carousel** component for dashboard.
    - [x] Build **Promo Modal** for targeted offers.
    - [x] Implement **Emergency Alert** banner for system messages.
- [x] **A/B Testing Integration**:
    - [x] Create `useExperiment` hook to get variant from `ab-testing-service`.
    - [x] Implement **Feature Flags** for gradual rollout.
    - [x] Track **Conversion Events** back to AB service.
    - [x] Build **Variant Preview** mode for QA testing.
- [x] **Customer Segmentation**:
    - [x] Fetch user segment from `promotion-service`.
    - [x] Display **Personalized Offers** based on segment.
    - [x] Show **Loyalty Tier** badge on profile.
    - [x] Implement **Segment-based Routing** for premium features.
- [x] **Dashboard Improvements**:
    - [x] Add **Financial Health Score** widget.
    - [x] Implement **Spending Insights** chart with categories.
    - [x] Add **Budget Tracking** progress bars.
    - [x] Build **Quick Actions** customization.
- [x] **Accessibility (A11y) Compliance**:
    - [x] Audit all pages with **axe-core**.
    - [x] Fix **Color Contrast** issues.
    - [x] Add **ARIA Labels** to interactive elements.
    - [x] Implement **Keyboard Navigation** for all modals.
    - [x] Test with **Screen Readers** (NVDA/VoiceOver).

---

## 🔧 Phase 4: Backend Improvements

### 30. API & Performance Optimization
- [x] **API Gateway Enhancements**:
    - [x] Implement **API Versioning** strategy (v1, v2).
    - [x] Add **Request Validation** middleware.
    - [x] Implement **Response Compression** (gzip/brotli).
    - [x] Add **API Analytics** for usage tracking.
- [x] **Database Optimization**:
    - [x] Add **Read Replicas** for reporting queries.
    - [x] Implement **Connection Pooling** tuning per service.
    - [x] Create **Materialized Views** for analytics dashboards.
    - [x] Implement **Query Optimization** for slow endpoints.
- [x] **Caching Strategy**:
    - [x] Implement **Cache Warming** on service startup.
    - [x] Add **Cache Invalidation** events via Kafka.
    - [x] Implement **Cache Metrics** dashboard.
    - [x] Add **Local Cache** fallback for Redis failures.

### 31. Service Reliability
- [x] **Circuit Breaker Tuning**:
    - [x] Configure **Resilience4j** thresholds per service.
    - [x] Add **Fallback Responses** for degraded mode.
    - [x] Implement **Bulkhead Pattern** for critical services.
- [x] **Retry & Timeout Policies**:
    - [x] Standardize **Retry Strategies** across services.
    - [x] Configure **Timeout Policies** for external calls.
    - [x] Add **Idempotency Keys** for all write operations.
- [x] **Health Check Improvements**:
    - [x] Add **Liveness** vs **Readiness** probe separation.
    - [x] Implement **Deep Health Checks** (DB, Kafka, Redis).
    - [x] Add **Dependency Health** reporting.

### 32. Security Enhancements
- [x] **API Security**:
    - [x] Implement **Rate Limiting** per user/IP.
    - [x] Add **Request Signing** for partner APIs.
    - [x] Implement **API Key Rotation** automation.
    - [x] Add **IP Whitelisting** for B2B endpoints.
- [x] **Data Protection**:
    - [x] Implement **Field-level Encryption** for PII.
    - [x] Add **Data Masking** in logs and responses.
    - [x] Implement **Audit Logging** for sensitive operations.
    - [x] Configure **Data Retention Policies** automation.
- [x] **Penetration Testing**:
    - [x] Schedule **Quarterly Pentest** with external vendor.
    - [x] Implement **SAST** (Static Analysis) in CI/CD.
    - [x] Add **DAST** (Dynamic Analysis) for staging.
    - [x] Create **Security Runbook** for incident response.

---

## 🏗️ Phase 4: Infrastructure & DevOps

### 33. CI/CD Pipeline Enhancements
- [x] **Tekton Pipeline Tasks**:
    - [x] Create **Build Pipeline** for all services.
    - [x] Create **Test Pipeline** with parallel execution.
    - [x] Create **Deploy Pipeline** with blue-green deployment.
    - [x] Add **Rollback Pipeline** for quick recovery.
- [x] **ArgoCD Configuration**:
    - [x] Setup **ApplicationSet** for multi-environment.
    - [x] Configure **Sync Waves** for dependency order.
    - [x] Implement **PR Preview Environments**.
    - [x] Add **Drift Detection** alerts.
- [x] **Quality Gates**:
    - [x] Add **SonarQube** integration for code quality.
    - [x] Implement **Test Coverage** thresholds (80%+).
    - [x] Add **Security Scanning** (Trivy/Snyk).
    - [x] Block deploys on **Critical Vulnerabilities**.

### 34. Monitoring & Alerting
- [x] **Grafana Dashboards**:
    - [x] Create **Business Metrics** dashboard (TPV, conversions).
    - [x] Create **SLA Dashboard** with uptime tracking.
    - [x] Create **Cost Dashboard** for resource optimization.
    - [x] Create **User Journey** dashboard with funnel analysis.
- [x] **Alerting Rules**:
    - [x] Define **SLO-based Alerts** (99.9% availability).
    - [x] Add **Error Budget** tracking.
    - [x] Implement **PagerDuty/OpsGenie** integration.
    - [x] Create **Runbooks** for each alert type.
- [x] **Log Management**:
    - [x] Implement **Log Correlation** with trace IDs.
    - [x] Add **Log Sampling** for high-volume services.
    - [x] Create **Log-based Alerts** for critical errors.
    - [x] Implement **Log Export** to S3 for compliance.

### 35. Cost Optimization
- [x] **Resource Right-sizing**:
    - [x] Analyze **CPU/Memory Usage** patterns.
    - [x] Implement **VPA** (Vertical Pod Autoscaler).
    - [x] Configure **HPA** thresholds per service.
    - [x] Add **Cluster Autoscaler** for nodes.
- [x] **Cost Visibility**:
    - [x] Implement **Cost Allocation** by namespace.
    - [x] Create **Cost Reports** for stakeholders.
    - [x] Set **Budget Alerts** for cost overruns.
    - [x] Identify **Idle Resources** for cleanup.

---

## 📋 Backlog & Future Ideas

### Ideas for Future Consideration
- [ ] **Voice Banking**: Implement voice commands for balance inquiry.
- [ ] **Chatbot Integration**: AI-powered customer support chatbot.
- [ ] **Crypto Wallet**: Support for cryptocurrency holdings.
- [ ] **Insurance Marketplace**: Integration with insurance providers.
- [ ] **Travel Wallet**: Multi-currency prepaid travel card.
- [ ] **Carbon Footprint**: Track environmental impact of spending.
- [ ] **Family Banking**: Shared accounts for family members.
- [ ] **Business Banking**: SME-focused features and dashboards.
- [ ] **Open Banking APIs**: PSD2/Open Banking compliance.
- [ ] **White-label SDK**: Embeddable banking widget for partners.

