# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> **Aturan Pengembang**: Langsung hapus (delete) task list dari file ini jika sudah selesai dikerjakan (tidak perlu menandainya sebagai `CLOSED`).
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)
> 📋 Incident response → [`../operations/INCIDENT_RESPONSE.md`](../operations/INCIDENT_RESPONSE.md)
> 🤖 ChatOps → [`../operations/CHATOPS.md`](../operations/CHATOPS.md)
> 🔐 Pen test schedule → [`../security/PENTEST_SCHEDULE.md`](../security/PENTEST_SCHEDULE.md)

---

## 📊 Board Summary

| Metric | Value |
|:---|:---|
| **Cluster Status** | 🟢 45/45 pods Ready, OCP 4.21, payu-dev. SSO, Kafka, PostgreSQL HA, Infinispan (DataGrid 8.6), Artemis AMQ 2-node active/passive — all connected. |
| **Last Release** | `:1.9.0` — loan-origination-process BPMN + backoffice task inbox (KOGITO-001 Phase 1-2) |
| **Last Updated** | 2026-07-03 (loan-origination-process compiled, full backend BUILD SUCCESS 56/56) |

---

## 🐛 Active Tickets

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift | ⬜ Open |
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh | ⬜ Open |
| DEVSECOPS-003 | P1 | Global rate limit 1000 req/s per IP | ⬜ Open |

---

## 🚀 Platform Deploy Queue

| Key | Priority | Category | Summary |
|:---|:---:|:---|:---|
| DEPLOY-006 | P1 | Security | Deploy Coraza WAF (INFRA-015) + ComplianceOperator CIS scan (INFRA-010) + Wazuh SIEM (INFRA-011) |
| DEPLOY-007 | P1 | Observability | OTel→Tempo (READY-019) + Loki (READY-020) + Prometheus alerts (READY-021) |
| DEPLOY-008 | P1 | DR/Security | Vault auto-snapshot (DEVSECOPS-001) + auto-unseal (DEVSECOPS-002) + DR runbook (INFRA-007) |
| DEPLOY-009 | P2 | CI/CD | Tekton Chains (INFRA-013) + Results (INFRA-014) + Renovate (DEVSECOPS-011) |
| OPS-2026-04-08-01 | P2 | Ops | Validate wallet-service cache rollout |
| OPS-2026-04-08-02 | P2 | Ops | Re-run k6 crud-stress-test.js via k6 Operator |
| READY-029 | P2 | Performance | Gatling load test: 1000 concurrent users |
| READY-030 | P2 | Performance | Stress: SOAK test 24h |
| READY-022 | P2 | Test | Unit test coverage 80%+ core domain |
| READY-023 | P2 | Test | Contract tests (Pact/SCC) |
| READY-060 | P3 | Card | Card tokenization + 3DS |
| READY-061 | P3 | Mobile | Expo SDK 55 + RN 0.85 upgrade |
| READY-062 | P3 | ML | ONNX fraud detection model |
| DEVSECOPS-014 | P3 | DevSecOps | Local Pipeline Simulation |
| DEVSECOPS-015 | P3 | DevSecOps | Security Findings Dashboard Grafana |
| DEVSECOPS-016 | P3 | DevSecOps | Service template scaffolder |
| INFRA-018 | P3 | Registry | Setup registry GC policy |
| INFRA-019 | P3 | Registry | Configure Quay.io auto-prune policy |
| DEVSECOPS-005 | P3 | Network | EgressNetworkPolicy + Istio egress gateway |
| DEVSECOPS-007 | P3 | Security | LUKS encryption PV + Vault DEK rotation |
| DEVSECOPS-012 | P3 | Cost | Monthly cost report workflow |

---

## 🔍 Ponytail Audit — Over-Engineering & Dead Code (2026-07-02)

| # | Key | Category | Summary |
|:---:|:---|:---|:---|
| AUDIT-096 | **PON-019** | arch | ~95 single-implementation hexagonal ports across 21 services. Consolidate when refactoring |
