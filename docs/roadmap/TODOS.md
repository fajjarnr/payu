# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Metric | Value |
|:---|:---|
| **Open Bugs** | 0 |
| **Open Epics** | 0 |
| **Open Stories** | 0 |
| **Spikes** | 6 |
| **Deferred** | 9 |
| **Suspended (OCP destroyed)** | 8 |
| **DevSecOps (suspended)** | 52 |

---

## 🚀 Framework & Infrastructure Upgrades

| Key | Priority | Category | Summary | Status |
|:---|:---:|:---|:---|:---|
| UPGRADE-012 | P2 | Mobile | Modernize Mobile App: Upgrade to Expo SDK 55 and React Native 0.85 | ⏸️ Skipped |

---

## 🔍 Spikes (Research / Architecture Decision)

| Key | Type | Question | Impact | Status |
|:---|:---|:---|:---|:---|
| ARCH-001 | Spike | KYC di level PayU atau project client? | Scope `kyc-service` | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client? | Output format `statement-service` | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client? | Multi-tenancy `support-service` | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client? | Multi-tenant mode `cms-service` | 📋 To Do |
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | ADR-0015, `rules-starter` shared lib | 📋 To Do |
| ARCH-006 | Spike | Spring Boot 4.0 & Jakarta EE 11 Migration Strategy: Audit Spring Cloud compatibility before platform-wide rollout | Oakwood Release Train | 📋 To Do |

---

## 🔮 Deferred (Icebox)

| Key | Type | Summary | Notes |
|:---|:---|:---|:---|
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN) | ❄️ Deferred |
| OCP-007 | Story | Service Mesh mTLS enforcement | ❄️ Planned |
| OCP-010 | Story | API versioning headers | ❄️ Planned |
| DR-001 | Story | Disaster Recovery live test execution | ❄️ Scripts ready |
| DEFER-001 | Story | Card Tokenization & 3DS | ❄️ Requires PCI-DSS scope + card network kontrak |
| RHPAM-001 | Story | Create `shared/rules-starter` (Drools 9.x embedded) | ❄️ Depends on ARCH-005 PoC |
| RHPAM-002 | Story | Migrate `lending-service` credit scoring ke DRL rules | ❄️ Depends on RHPAM-001 |
| RHPAM-003 | Story | Payment routing DMN decision tables di `gateway-service` | ❄️ Depends on RHPAM-001 |
| RHPAM-004 | Story | Lending workflow + KYC/AML BPMN orchestration (Kogito) | ❄️ Depends on RHPAM-002 |

---

## ⏸️ Suspended — Operational Follow-Up (OpenShift Destroyed May 2)

| Key | Summary | Notes |
|:---|:---|:---|
| OPS-2026-04-08-01 | Validate wallet-service cache rollout — no more DistributedCacheService warnings | Cache-starter compatibility fix applied; probe interrupted |
| OPS-2026-04-08-02 | Re-run k6 crud-stress-test.js (40 min) via k6 Operator | Need `kubectl apply` TestRun CRD |
| OPS-2026-04-08-03 | If stress breaches p(99) < 10s — isolate slow endpoint | k6 Operator runner logs available |
| OPS-2026-04-08-04 | Re-run k6 crud-data-consistency-test.js after stress revalidation | Use TestRun CRD |
| OPS-2026-04-08-05 | Decide GATEWAY_RATE_LIMIT_TEST_MODE on/off after validation | Test mode currently enabled |
| OPS-2026-04-09-01 | Re-run k6 with in-cluster service URLs | k6 Operator lifecycle verified |
| OPS-2026-04-09-06 | Fix transaction-service Redis/DataGrid RESP connection (port 11222) | Affects Split Bill list (HTTP 500) |
| OPS-2026-04-09-07 | Create admin Keycloak user for admin-only endpoints | Smart Routing returns 404 |

---

## 🏗️ DevSecOps Architecture (Suspended — OCP Destroyed)

> Lihat [`infrastructure/DEVSECOPS_ARCHITECTURE.md`](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) v1.3.1
> Phase 1: ✅ COMPLETE (except 3 DR items). Phase 2–4: 📋 Suspended.

### Phase 1 — Remaining DR Tasks

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-005 | P0 | Configure Vault Raft auto-snapshot to encrypted S3 |
| INFRA-006 | P0 | Configure Vault auto-unseal (Transit or KMS) |
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh |

### Phase 2 — Hardening (Paused)

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift |
| INFRA-002 | P0 | Build container images for 22 remaining services via Tekton |
| INFRA-003 | P0 | Deploy all 23 services to payu-dev |
| INFRA-004 | P0 | Create ArgoCD ApplicationSet for all services |
| INFRA-008 | P0 | Integrate OWASP ZAP + Schemathesis into Tekton pipeline |
| INFRA-009 | P0 | Implement OSSM Istio PeerAuthentication STRICT |
| INFRA-012 | P0 | Complete ArgoCD Image Updater setup |
| INFRA-016 | P0 | Configure rate limiting (global 1000 req/s per IP) |
| INFRA-017 | P0 | Enforce API security headers (HSTS, CSP, X-Frame-Options) |
| INFRA-021 | P0 | Configure ArgoCD auto-rollback on health check failure |
| INFRA-010 | P1 | Configure ComplianceOperator CIS scan |
| INFRA-011 | P1 | Deploy Wazuh manager + agent for SIEM |
| INFRA-013 | P1 | Enable Tekton Chains for SLSA provenance |
| INFRA-014 | P1 | Configure Tekton Results for audit trail |
| INFRA-015 | P1 | Deploy Coraza WAF with OWASP CRS v4.x |
| INFRA-020 | P1 | Define severity P1-P4 + escalation path |
| INFRA-022 | P1 | Setup PagerDuty/Opsgenie for P1/P2 alerting |
| INFRA-018 | P2 | Setup registry GC policy |
| INFRA-019 | P2 | Configure Quay.io auto-prune policy |

---

_Last Updated: May 6, 2026 — Cleaned up all completed items (archived to CHANGELOG.md)._
_⚠️ OpenShift Cluster Destroyed (May 2, 2026): All OpenShift-dependent tasks suspended. Local podman environment is primary target._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
