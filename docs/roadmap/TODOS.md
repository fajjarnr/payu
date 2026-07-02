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
| **Open P0s** | **0** |
| **Open P1s** | 1 (READY-076) |
| **Open P2s** | 0 |
| **Open P3s** | 4 (AUDIT-096, 098, 101) |
| **Production Score** | **payu-dev: 46/46 pods Ready, 0 Not-Ready, 0 CrashLoop, 0 ImagePullBackOff (100% healthy)** |
| **Last Release** | `:1.8.77` — ops framework, security headers, TODOS consolidation |
| **Last Audit** | July 2, 2026 — Ponytail deep audit. 33 findings (5 P1, 13 P2, 15 P3). ~3,000 lines dead code, ~8 unused npm deps, ~95 single-impl ports, 7 orphaned ports, ~58 duplicate configs. |
| **Last Updated** | July 2, 2026 (PON-033 closed) |

---

## 🐛 Active Tickets

- **READY-076** Postgres HA — image registry blocked (Crunchy `ubi8-2.50.1` etc missing)

---

## 🚀 Framework & Infrastructure Upgrades

| Key | Priority | Category | Summary | Status |
|:---|:---:|:---|:---|:---|
| UPGRADE-012 | P2 | Mobile | Modernize Mobile App: Upgrade to Expo SDK 55 and React Native 0.85 | ⏸️ Skipped |
| UPGRADE-014 | P2 | Frontend | Next.js 16.2.9 Upgrade (Performance & Turbopack default) | ⏳ Planned |

---

## 🔍 Spikes

| Key | Type | Question | Status |
|:---|:---|:---|:---|
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC | ❄️ Deferred |

---

## 🔮 Deferred (Icebox)

| Key | Type | Summary | Notes |
|:---|:---|:---|:---|
| OCP-007 | Story | Service Mesh mTLS enforcement | ❄️ Planned |
| OCP-010 | Story | API versioning headers | ❄️ Planned |
| DR-001 | Story | Disaster Recovery live test execution | ❄️ Scripts ready |
| DEFER-001 | Story | Card Tokenization & 3DS | ❄️ Requires PCI-DSS scope |
| RHPAM-001..004 | Story | Rules engine + Kogito BPMN | ❄️ Depends on ARCH-005 PoC |

---

## ⏸️ Suspended — Needs OpenShift Cluster

> Semua item di bawah memerlukan OpenShift cluster (destroyed May 2, menunggu restorasi).

### Operations (k6, OCP)

| Key | Summary |
|:---|:---|
| OPS-2026-04-08-01 | Validate wallet-service cache rollout (probe interrupted) |
| OPS-2026-04-08-02 | Re-run k6 crud-stress-test.js via k6 Operator |
| OPS-2026-04-08-04 | Re-run k6 crud-data-consistency-test.js |
| OPS-2026-04-09-07 | Create admin Keycloak user |

### DevSecOps Architecture

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh |
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift |
| INFRA-010 | P1 | Configure ComplianceOperator CIS scan |
| INFRA-011 | P1 | Deploy Wazuh manager + agent for SIEM |
| INFRA-013 | P1 | Enable Tekton Chains for SLSA provenance |
| INFRA-014 | P1 | Configure Tekton Results for audit trail |
| INFRA-015 | P1 | Deploy Coraza WAF with OWASP CRS v4.x |
| INFRA-022 | P1 | Setup PagerDuty/Opsgenie for P1/P2 alerting |
| INFRA-018 | P2 | Setup registry GC policy |
| INFRA-019 | P2 | Configure Quay.io auto-prune policy |

### DevSecOps Gaps

| Key | Priority | § | Summary |
|:---|:---:|:---:|:---|
| DEVSECOPS-001 | P1 | §9.2 | Vault Raft auto-snapshot → S3 |
| DEVSECOPS-002 | P1 | §9.2 | Vault auto-unseal via Transit/KMS |
| DEVSECOPS-003 | P1 | §14.3 | Global rate limit 1000 req/s per IP |
| DEVSECOPS-005 | P2 | §13.2 | EgressNetworkPolicy + Istio egress gateway |
| DEVSECOPS-006 | P2 | §13.3 | DNS query logging + blok DNS tunneling |
| DEVSECOPS-007 | P2 | §16.2 | LUKS encryption PV + Vault DEK rotation |
| DEVSECOPS-008 | P2 | §16.3 | Wazuh rule data egress detection |
| DEVSECOPS-011 | P2 | §4.1.4 | Renovate Bot deployment |
| DEVSECOPS-012 | P2 | §10.2 | Monthly cost report workflow |
| DEVSECOPS-014 | P3 | §21.2 | Local Pipeline Simulation |
| DEVSECOPS-015 | P3 | §21.2 | Security Findings Dashboard Grafana |
| DEVSECOPS-016 | P3 | §21.3 | Service template scaffolder |

### Production Readiness (Blocked by Cluster)

| Key | Category | Summary |
|:---|:---|:---|
| READY-010 | Security | Vault integration verified end-to-end |
| READY-019 | Observability | Distributed tracing (OTel → Tempo) |
| READY-020 | Observability | Loki log shipping verified |
| READY-021 | Observability | Prometheus scrape config + alerting rules |
| READY-022 | Test coverage | Unit test coverage 80%+ core domain |
| READY-023 | Test coverage | Contract tests (Pact/SCC) |
| READY-026 | HA | Kafka 3-broker cluster |
| READY-028 | HA | AMQ broker pair |
| READY-029 | Performance | Gatling load test: 1000 concurrent users |
| READY-030 | Performance | Stress: SOAK test 24h |
| READY-033 | Test infra | ThemeResolver/ContractVerifier (root cause fixed, ⚠️ re-enable after Hibernate 7 JSON migration) |
| READY-040 | Compliance | PCI-DSS audit: encryption-at-rest verified |
| READY-041 | Compliance | UU PDP: data retention + right-to-erasure |
| READY-043 | Compliance | Audit trail: append-only + actor + timestamp |
| READY-044 | CI/CD | Tekton Chains (SLSA provenance) |
| READY-045 | CI/CD | Tekton Results (audit trail) |
| READY-046 | CI/CD | ArgoCD sync verified (GitOps) |
| READY-047 | Security | Coraza WAF with OWASP CRS v4.x |
| READY-048 | Security | ComplianceOperator CIS scan |
| READY-049 | Security | Wazuh SIEM (manager + agent) |
| READY-050 | Ops | PagerDuty/Opsgenie for P1/P2 alerting |
| READY-060 | Card | Card tokenization + 3DS |
| READY-061 | Mobile | Expo SDK 55 + RN 0.85 upgrade |
| READY-062 | ML | ONNX fraud detection model |

---

## 📝 Docs Backlog (No Cluster Needed)

| Key | Doc | Status |
|:---|:---|:---|
| **DEVSECOPS-010** | ⬜ DNS failover procedure — pending cluster topology confirmation | ⬜ TBD |

---

## 🔍 Ponytail Audit — Over-Engineering & Dead Code (2026-07-02)

> Deep audit across 17 shared starters + 21 backend services + frontend web-app.
> Scope: over-engineering, dead code, stdlib duplication, single-implementation interfaces, duplicate config, unused deps.
> Correctness bugs and security holes are out of scope — routed to normal review pass.



### 🟡 P3 — Nice to Have

| # | Key | Category | Summary |
|:---:|:---|:---|:---|
| AUDIT-096 | **PON-019** | arch | ~95 single-implementation hexagonal ports across 21 services (30 inbound + 65 outbound). Interface with exactly 1 adapter — YAGNI ceremony. Consolidate when refactoring |
| AUDIT-098 | **PON-021** | dedup | 18 copies of `SecurityConfig.java` (85-127 lines each) across all services. Extract to `security-starter` auto-config |
| AUDIT-101 | **PON-024** | dedup | 8 copies of `DataSourceConfiguration.java`. Extract to shared persistence starter |

### 📊 Audit Stats

| Metric | Count |
|:---|:---:|
| Total findings | 4 |
| P1 (critical) | 0 |
| P2 (important) | 0 |
| P3 (nice-to-have) | 5 |
| Estimated dead code (web-app) | ~3,000+ lines |
| Unused npm packages | ~8 (`jest*`, `gsap`, `date-fns`, `@radix-ui/react-visually-hidden`, `@dnd-kit/*` in devDeps) |
| Duplicate config files (backend) | ~58 across services |
| Dead barrel exports (web-app) | ~40 |
| Single-implementation ports (backend) | ~95 |
| Orphaned ports (backend) | 3 |

---


