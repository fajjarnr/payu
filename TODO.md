# Project Roadmap & Todo List

## 🚀 Next Priorities

### 1. Compliance Service Implementation
- [ ] Implement `compliance-service` (currently empty directory).
- [ ] Focus on AML (Anti-Money Laundering) and CFT (Combating the Financing of Terrorism) procedures as per PRD.
- [ ] Integrate with `transaction-service` for real-time monitoring.

### 2. Integration & E2E Testing
- [ ] Create a holistic End-to-End test suite covering the full user journey:
  - Frontend -> Gateway -> Auth -> Account -> Wallet -> Transaction.
- [ ] Implement Cypress or Playwright tests in the frontend repository.

### 3. Load Testing & Performance
- [ ] Implement load testing scripts (k6, Gatling, or JMeter).
- [ ] Target: Verify system stability under "100K concurrent users" load.
- [ ] Focus on `gateway-service` and `transaction-service` throughput (TPS).

### 4. Security Hardening
- [ ] Run SAST (Static Application Security Testing) on all services.
- [ ] Run DAST (Dynamic Application Security Testing).
- [ ] Verify secret management via HashiCorp Vault (ensure no hardcoded secrets).

### 5. Deployment (OpenShift)
- [ ] Create/Verify Helm Charts or Kustomize templates for all services.
- [ ] Test deployment pipeline to a Red Hat OpenShift environment.
