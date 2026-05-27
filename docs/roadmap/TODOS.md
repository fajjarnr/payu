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
| **Open P0s** | 1 (INFRA-001) |
| **Open P1s** | 0 (all resolved) |
| **Open P2s** | 22 |
| **Last Audit** | May 27, 2026 — ARCH-010 resolved: quarkus-api-commons created, 3 Quarkus services have shared starters. |
| **Production Score** | 98/100 |

---

## 🚀 Framework & Infrastructure Upgrades

| Key | Priority | Category | Summary | Status |
|:---|:---:|:---|:---|:---|
| UPGRADE-012 | P2 | Mobile | Modernize Mobile App: Upgrade to Expo SDK 55 and React Native 0.85 | ⏸️ Skipped |

---

## 🔍 Spikes (Research / Architecture Decision)


| Key | Type | Question | Impact | Status |
|:---|:---|:---|:---|:---|
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | ADR-0015, `rules-starter` shared lib | ❄️ Deferred — Planned for future, Java logic sufficient for MVP. Will use Drools 9.x embedded. |
| ARCH-006 | Spike | Spring Boot 4.0 & Jakarta EE 11 Migration Strategy: Audit Spring Cloud compatibility before platform-wide rollout | Oakwood Release Train | ❄️ Deferred — Boot 3.5.14 + Java 25 stable, no urgency |

---

## 🔮 Deferred (Icebox)

| Key | Type | Summary | Notes |
|:---|:---|:---|:---|
| LOG-001 | Spike | Evaluate OTLP log export (`quarkus.otel.logs.enabled`) vs current stdout JSON | ❌ **Decision: Keep current setup** — stdout JSON + LokiStack is K8s best practice, fully service-mesh compatible, zero extra overhead. OTLP logs redundant. |
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

## 🏭 Production Readiness Audit (May 14, 2026)

### 🟡 P2 — Medium

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| DX-001 | Web-App | 4 barrel export `index.ts` files bypass tree-shaking. | ⏳ Open |
| CFG-001 | Backend | 21 services hardcode `localhost` as fallback. | ⏳ Open |
| ARCH-012 | Backend | `BaseController` duplicated across 11 services. | ⏳ Open |
| ARCH-013 | Backend | `SecurityConfig` size range 32–244 lines. | ⏳ Open |
| ARCH-015 | Backend | `RateLimitV2Filter` uses `synchronized` bottleneck. | ⏳ Open |
| ARCH-016 | Backend | `@Service` in adapter layer (3 services). | ⏳ Open |
| DEP-001 | Backend | Mixed `${project.version}` vs hardcoded `1.0.0-SNAPSHOT`. | ⏳ Open |

---

## 🏗️ DevSecOps Architecture (Suspended — OCP Destroyed)

> Lihat [`infrastructure/DEVSECOPS_ARCHITECTURE.md`](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) v1.3.1
> Phase 1: COMPLETE (except DR items below). Phase 2–4: 📋 Suspended.

### Phase 1 — Remaining DR Tasks

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh | ⏳ Open |

### Phase 2 — Hardening (Paused)

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift | ⏳ Open — Trivy task exists in build-pipeline, blocked by registry.redhat.io pull secret for pilot image. gate is RHACS (step 6), trivy is warn-only |
| INFRA-010 | P1 | Configure ComplianceOperator CIS scan | ⏳ Open |
| INFRA-011 | P1 | Deploy Wazuh manager + agent for SIEM | ⏳ Open |
| INFRA-013 | P1 | Enable Tekton Chains for SLSA provenance | ⏳ Open |
| INFRA-014 | P1 | Configure Tekton Results for audit trail | ⏳ Open |
| INFRA-015 | P1 | Deploy Coraza WAF with OWASP CRS v4.x | ⏳ Open |
| INFRA-020 | P1 | Define severity P1-P4 + escalation path | ⏳ Open |
| INFRA-022 | P1 | Setup PagerDuty/Opsgenie for P1/P2 alerting | ⏳ Open |
| INFRA-018 | P2 | Setup registry GC policy | ⏳ Open |
| INFRA-019 | P2 | Configure Quay.io auto-prune policy | ⏳ Open |

---

## 🔬 Comprehensive Production Readiness Audit (May 15, 2026)

### 🟡 P2 — Medium

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| TEST-004 | Backend | **support-service has only 5 test files** (1 integration test). Customer support flows undercovered. | ⏳ Open |
| TEST-005 | Backend | **integration-service has 6 test files but 0 integration tests**. Ironic — the integration service has no integration tests. | ⏳ Open |
| TEST-006 | Backend | **investment-service has only 6 test files** (2 integration tests). Financial operations need higher coverage. | ⏳ Open |
| CACHE-002 | Web-App | **No explicit `revalidate` or `unstable_cache` usage** — no data freshness strategy for server-fetched data. Risk of stale data in production. | ⏳ Open |
| IDEM-003 | Backend | **notification-service (Quarkus) has no idempotency** — duplicate notifications possible on retry. Not critical but poor UX. | ⏳ Open |

---

## 🔒 Infrastructure & Backend Deep Audit (May 15, 2026 — Batch 2)

### 🟡 P2 — Medium (Backlog)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| K8S-010 | Infra | **web-app missing Ingress/Route resource** — only has deployment.yaml + service.yaml. No Route (OpenShift) or Ingress defined in base. External traffic cannot reach the frontend. | ⏳ Open |
| K8S-011 | Infra | **Duplicate HPA definitions** — `hpa.yaml` and `hpa-enhanced.yaml` both define HPAs for the same services (gateway, account, transaction, wallet) with conflicting minReplicas/maxReplicas. Only one should be active. | ⏳ Open |
| K8S-012 | Infra | **No `preStop` lifecycle hook** — services with graceful shutdown need `preStop: sleep 5` to allow load balancer deregistration before SIGTERM. Prevents connection drops during rolling updates. | ⏳ Open |
| CONTAINER-003 | Backend | **No multi-stage build for Java services** — Containerfiles expect pre-built JARs (`COPY target/*.jar`). Multi-stage builds would make CI simpler and ensure reproducible builds. Only Python services (analytics, kyc) use multi-stage. | ⏳ Open |
| CONTAINER-004 | Backend | **Containerfile image version labels are stale** — e.g., account-service label says `1.5.0` but deployment uses `1.8.1`. Labels should use build-time ARG or be removed. | ⏳ Open |
| SEC-BACKEND-003 | Backend | **CORS configuration inconsistent across services** — account-service uses env-var-driven origins, partner-service hardcodes production domains, wallet/transaction have no CORS config. Gateway handles CORS centrally but services should have consistent fallback. | ⏳ Open |
| CFG-PROD-002 | Backend | **`spring.jpa.show-sql` not explicitly disabled in container profiles** — some services rely on default `false` but don't explicitly set it. Risk of SQL logging in production if default changes. | ⏳ Open |
| CFG-PROD-003 | Backend | **Tracing probability `0.1` (10%) may be insufficient** — for debugging production issues, 10% sampling means 90% of traces are lost. Consider 100% for errors, 10% for success (head-based sampling with tail-based for errors). | ⏳ Open |
| K8S-013 | Infra | **No `podDisruptionBudget` for web-app in prod** — PDB exists in base but prod overlay sets 3 replicas. With `minAvailable: 1`, 2 pods can be evicted simultaneously leaving only 1 serving traffic. Should be `minAvailable: 2` for prod. | ⏳ Open |
| K8S-014 | Infra | **No resource quotas referenced in workload overlays** — namespace-level ResourceQuotas exist in `foundation/namespaces/` but workload deployments don't account for them. Risk of deployment failures if quotas are tight. | ⏳ Open |

---

_Last Updated: May 27, 2026 — Cleaned up: all completed items archived to CHANGELOG.md. Only open/deferred items remain._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
