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

### 9. Mobile App Support
- [x] Prepare mobile-responsive views for web-app.
- [x] [Planned] Native mobile boilerplate (iOS/Android).

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

### 15. Testing Suite
- [x] **Cross-Service Integration Tests**: Implement holistic End-to-End test suite covering full user journeys.
- [x] **TDD Hardening**: Increase test coverage to >80% for all core banking services.
- [x] **Frontend Quality**:
    *   [x] Setup **Playwright** for E2E testing of critical financial flows (KYC, Transfer, Bill Pay).
    *   [x] Setup **Vitest** for unit testing critical logic in `src/services` and `src/stores`.
- [x] [SKIP] Load Testing: (Postponed as per user request).
- [x] [SKIP] Security Testing: (Postponed as per user request).

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
- [ ] Implement **SaaS Multitenancy** (Tenant ID isolation at DB and Gateway levels).
- [ ] Implement **Universal Search** in Backoffice for cross-service data lookup.
- [ ] Implement **Data Archival Strategy** for handling billions of transaction records.

### 20. Advanced Security & AI
- [ ] Implement **Real-time AI Fraud Detection** Scoring for all transactions.
- [ ] Implement **Biometric Edge Authentication** bridge for Mobile App.
- [ ] Implement **Dynamic Risk-based MFA** (trigger MFA only for suspicious login patterns).

### 21. Global Readiness
- [ ] Implement **Internationalization (i18n)** support for English.
- [ ] Support for **Multi-currency Pockets** and real-time FX (Foreign Exchange) engine.

### 22. Developer Experience & Partner Portal
- [ ] Implement **Centralized API Portal** (Swagger/OpenAPI aggregator for all services).
- [ ] Implement **Partner Sandbox Environment** with mock data and simulated latencies.
- [ ] Build **Developer Documentation Site** with integration guides and SDK examples.

### 23. Advanced Customer Services
- [ ] Implement **E-Statement Engine** (Generate monthly transaction PDFs).
- [ ] Implement **Web Accessibility (A11y)** compliance as per OJK/WCAG standards.
- [ ] Implement **In-app Feedback System** with screenshot/log attachment capabilities.

### 24. CMS & Marketing Automation
- [ ] Implement **Dynamic Content Management (CMS)** for banners, promos, and alerts.
- [ ] Implement **A/B Testing Framework** for UI features and promotional offers.
- [ ] Integrate **Customer Segmentation Engine** for personalized notification campaigns.

## 🛠️ Operational Polish & Stability

### 22. Infrastructure Hardening
- [ ] Optimize **Docker Resource Limits** (CPU/RAM) across all 20+ containers.
- [ ] Implement **Automated Regression Testing** in CI/CD for critical financial logic.
- [ ] Fine-tune **Inter-service Health Checks** for faster recovery in OpenShift.
