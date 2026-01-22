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
- [ ] Implement `lending-service` (Java Spring Boot - Hexagonal).
- [ ] Features: Personal Loan, PayLater (Buy Now Pay Later), automated Credit Underwriting.

### 5. Marketing & Engagement
- [ ] Implement `promotion-service` (Java Quarkus - Layered).
- [ ] Features: Rewards points, Cashback engine, Referral program.

### 6. Operational Excellence
- [ ] Implement `backoffice-service` (Java Quarkus - Layered).
- [ ] Features: Dashboard for Manual KYC review, Fraud Monitoring, and Customer Operations.

### 7. Ecosystem & Open Banking
- [ ] Implement `partner-service` (Java Quarkus - Layered).
- [ ] Features: SNAP BI Standard API integration, Merchant Portal for B2B.

## ⚙️ Testing & Infrastructure (Lower Priority)

### 8. Integration & E2E Testing [SKIPPED - Waiting for Frontend]
- [-] Create a holistic End-to-End test suite covering the full user journey:
  - Frontend -> Gateway -> Auth -> Account -> Wallet -> Transaction.
- [-] Implement Cypress or Playwright tests in the frontend repository.

### 9. Load Testing & Performance [SKIPPED]
- [-] Implement load testing scripts (k6, Gatling, or JMeter).
- [-] Target: Verify system stability under "100K concurrent users" load.
- [-] Focus on `gateway-service` and `transaction-service` throughput (TPS).

### 10. Security Hardening [SKIPPED]
- [-] Run SAST (Static Application Security Testing) on all services.
- [-] Run DAST (Dynamic Application Security Testing).
- [-] Verify secret management via HashiCorp Vault (ensure no hardcoded secrets).

### 11. Deployment (OpenShift) [SKIPPED]
- [-] Create/Verify Helm Charts or Kustomize templates for all services.
- [-] Test deployment pipeline to a Red Hat OpenShift environment.
