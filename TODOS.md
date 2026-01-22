# Project Roadmap & Todo List

## 🚀 Next Priorities

### 1. Compliance Service Implementation
- [x] Implement `compliance-service` (currently empty directory).
- [x] Focus on AML (Anti-Money Laundering) and CFT (Combating the Financing of Terrorism) procedures as per PRD.
- [x] Integrate with `transaction-service` for real-time monitoring.

### 2. Frontend-Backend API Integration
- [x] Connect `frontend/web-app` with `gateway-service` for all core flows.
- [x] Implement API client/services in the frontend using the defined backend specifications.
- [x] Handle JWT authentication and refresh tokens in the web-app.
- [x] Implement real-time updates via WebSocket/Kafka if necessary for dashboards.

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
- [x] Complete UI design system (Premium Emerald rules in `GEMINI.md`).
- [ ] Implement missing pages with consistent UI:
    - [x] **QRIS Payments** (`/qris`) -> Integrate with `transaction-service`.
    - [x] **Virtual Card** (`/cards`) -> Integrate with `account-service` / `wallet-service`.
    - [x] **Investments** (`/investments`) -> Integrate with `investment-service`. (Implemented via teaser/pockets logic update)
    - [x] **Financial Analytics** (`/analytics`) -> Integrate with `analytics-service`.
    - [x] **Security & MFA** (`/security`) -> Integrate with `auth-service`.
    - [x] **Account Settings** (`/settings`) -> Integrate with `account-service`.
    - [x] **Help & Support** (`/support`) -> Integrate with `support-service`.
- [ ] Implement full API integration via `gateway-service` for all core flows.
- [ ] Optimize state management (Zustand) and Data fetching (React Query).
- [ ] Implement real-time updates via WebSocket/Kafka for dashboards.

### 9. Mobile App Support
- [ ] Prepare mobile-responsive views for web-app.
- [ ] [Planned] Native mobile boilerplate (iOS/Android).

## 🛡️ Backend Technical Debt & Hardening

### 10. Security Hardening
- [ ] Replace `permitAll()` with production-ready OAuth2/Keycloak configuration in all services.
- [ ] Implement strict CORS and rate limiting in `gateway-service`.
- [ ] Audit user data access patterns for GDPR compliance.

### 11. SNAP BI Full Integration
- [ ] Transition `partner-service` from mock to real SNAP BI standard implementation.
- [ ] Implement signature validation and certificate management for partners.

### 12. Testing & Performance
- [ ] **Cross-Service Integration Tests**: Implement holistic End-to-End test suite covering full user journeys.
- [ ] **Load Testing**: Establish performance baseline for 100K concurrent users.
- [ ] **Security Testing**: Run SAST/DAST and verify secret management via Vault.
