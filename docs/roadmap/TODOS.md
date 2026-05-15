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
| **Open P1s** | 1 (ARCH-010) |
| **Open P2s** | 22 |
| **Last Audit** | May 15, 2026 — Sprint 2: ARCH-008/009, AUTH-033, TEST-001/2/3 resolved. All P0 Backend items cleared. Score: 98→99/100. |
| **Production Score** | 97/100 |

---

## 🚀 Framework & Infrastructure Upgrades

| Key | Priority | Category | Summary | Status |
|:---|:---:|:---|:---|:---|
| UPGRADE-012 | P2 | Mobile | Modernize Mobile App: Upgrade to Expo SDK 55 and React Native 0.85 | ⏸️ Skipped |

---

## 🔍 Spikes (Research / Architecture Decision)

> ✅ ARCH-001 through ARCH-004 resolved May 7, 2026 — decisions archived to [`docs/adr/`](../../docs/adr/) and [`CHANGELOG.md`](../../CHANGELOG.md).

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

## 🔐 Auth & Health Endpoint Stabilization (May 9–14, 2026)

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| AUTH-033 | P1 | All 18 HealthControllers return hardcoded `{"status": "UP"}` without checking DB/Redis/Kafka — false positive health checks in production. Follow L-018 pattern with `@Readiness` (DB ping + Redis PING + Kafka cluster metadata). | ✅ Done (May 15) — All 18 services + api-commons updated: DB `SELECT 1`, Redis `PING` (if available), Kafka listener status (if available). Graceful fallback with `@Autowired(required = false)`. Structured JSON response with latency metrics. |

---

## 🏭 Production Readiness Audit (May 14, 2026)

### 🔴 P0 — Critical

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| ARCH-008 | Backend | **13 services put `@Entity` in domain layer**. | ✅ Done (May 15) — All 46 @Entity classes moved to `adapter/persistence/entity/` with `Entity` suffix. 13 services refactored: account, cms, compliance, integration, notification, statement, auth, backoffice, billing, support, promotion, transaction, partner. Hexagonal Architecture compliant: zero JPA annotations in domain layer. |

### 🟠 P1 — High

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| ARCH-009 | Backend | **~70+ inner-class enums** across all services. | ✅ Done (May 15) — 144 inner-class enums extracted to top-level `.java` files. 250+ reference files updated. 15/18 services compile. 3 services fixed (wallet, investment, transaction: FQN type conflicts resolved). |
| ARCH-010 | Backend | **Quarkus services missing all shared starters**. | ⏳ Open |

### 🟡 P2 — Medium

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| DX-001 | Web-App | 4 barrel export `index.ts` files bypass tree-shaking. | ⏳ Open |
| CFG-001 | Backend | 21 services hardcode `localhost` as fallback. | ⏳ Open |
| ARCH-012 | Backend | `BaseController` duplicated across 11 services. | ⏳ Open |
| ARCH-013 | Backend | `SecurityConfig` size range 32–244 lines. | ⏳ Open |
| ARCH-014 | Backend | `CorrelationIdFilter` duplicated in 3 services. | ⏳ Open |
| ARCH-015 | Backend | `RateLimitV2Filter` uses `synchronized` bottleneck. | ⏳ Open |
| ARCH-016 | Backend | `@Service` in adapter layer (3 services). | ⏳ Open |
| DEP-001 | Backend | Mixed `${project.version}` vs hardcoded `1.0.0-SNAPSHOT`. | ⏳ Open |

---

## 🏗️ DevSecOps Architecture (Suspended — OCP Destroyed)

> Lihat [`infrastructure/DEVSECOPS_ARCHITECTURE.md`](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) v1.3.1
> Phase 1: ✅ COMPLETE (except 3 DR items). Phase 2–4: 📋 Suspended.

### Phase 1 — Remaining DR Tasks

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-005 | P0 | Configure Vault Raft auto-snapshot to encrypted S3 | ✅ Done (May 15) — Production Vault StatefulSet (3 replicas, Raft, AWS KMS auto-unseal) + CronJob snapshot to S3 every 6h created at `infrastructure/platform/security/vault/vault-production.yaml` |
| INFRA-006 | P0 | Configure Vault auto-unseal (Transit or KMS) | ✅ Done (May 15) — Combined with INFRA-005 above |
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh | ⏳ Open |

### Phase 2 — Hardening (Paused)

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift | ⏳ Open — Trivy task exists in build-pipeline, blocked by registry.redhat.io pull secret for pilot image. gate is RHACS (step 6), trivy is warn-only |
| INFRA-008 | P0 | Integrate OWASP ZAP + Schemathesis into Tekton pipeline | ✅ Done (May 15) — ZAP & Schemathesis tasks wired in deploy-pipeline.yaml for DEV→SIT→UAT stages |
| INFRA-009 | P0 | Implement OSSM Istio PeerAuthentication STRICT | ✅ Done (May 15) — Istio control plane (Sail operator v3.3.3) running: istiod + IstioCNI Healthy. PeerAuthentication mesh-wide STRICT + per-namespace (payu-dev PERMISSIVE, prod STRICT). AuthorizationPolicy, RequestAuthentication, DestinationRules deployed. Security headers EnvoyFilter (HSTS, CSP, XFO) deployed. Ingress gateway deployed via manual Deployment (pending tuning). |
| INFRA-012 | P0 | Complete ArgoCD Image Updater setup | ✅ Done (May 15) — Image Updater deployed (deployment + RBAC + ConfigMap), ApplicationSets created (5 envs), auto-rollback CronJob active, drift detection configured, 22 applications synced |
| INFRA-016 | P0 | Configure rate limiting (global 1000 req/s per IP) | ✅ Done (May 15) — Rate limit service (envoyproxy/ratelimit) deployed with Redis backend, ConfigMap with per-IP/per-API-key rules, EnvoyFilter wiring at ingress gateway |
| INFRA-017 | P0 | Enforce API security headers (HSTS, CSP, X-Frame-Options) | ✅ Done (May 15) — EnvoyFilter deployed: HSTS (max-age=1y), X-Frame-Options: DENY, X-Content-Type-Options: nosniff, Referrer-Policy, Permissions-Policy, CSP, Cache-Control |
| INFRA-021 | P0 | Configure ArgoCD auto-rollback on health check failure | ✅ Done (May 15) — CronJob every 2 min + auto-rollback-policy.yaml with retry (3x, exponential backoff), Slack notifications |
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

### 🟠 P1 — High

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| TEST-001 | Backend | **cms-service has only 2 test files** (1 ArchUnit, 1 unit test, 0 integration tests). Critical content management service with near-zero coverage. | ✅ Done (May 15) — 71 tests: ContentServiceTest(40), ContentControllerTest(18), ContentRepositoryIntegrationTest(20), ContentSchedulerTest(6). Fixed inner enum extraction. BUILD SUCCESS. |
| TEST-002 | Backend | **api-portal-service has only 4 test files** (2 controller, 2 service, 0 integration tests). Partner-facing API portal with minimal coverage. | ✅ Done (May 15) — 76 tests: ApiPortalIntegrationTest(15), ApiPortalResourceTest(15), SandboxResourceTest(14), ApiPortalServiceTest(10), SandboxServiceTest(7), ArchitectureTest(8), CorrelationIdFilterTest(7). BUILD SUCCESS. |
| TEST-003 | Backend | **product-catalog-service has only 4 test files** (1 integration test). Core catalog service undercovered. | ✅ Done (May 15) — 60 tests: PublicProductControllerTest(10), ProductCatalogPersistenceAdapterTest(9), AdminProductControllerTest(4), HealthControllerTest(3), GlobalExceptionHandlerTest(4), ProductDefinitionEntityTest(4) + existing. Fixed pre-existing compilation bug in AdminProductController. BUILD SUCCESS. |

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
| SEC-BACKEND-002 | Backend | **`keycloakGrantedAuthoritiesConverter()` duplicated across 15+ services** — identical 80-line method copy-pasted in every SecurityConfig. Should be extracted to `security-starter` shared library. Maintenance nightmare and inconsistency risk. | ⏳ Open |
| SEC-BACKEND-003 | Backend | **CORS configuration inconsistent across services** — account-service uses env-var-driven origins, partner-service hardcodes production domains, wallet/transaction have no CORS config. Gateway handles CORS centrally but services should have consistent fallback. | ⏳ Open |
| CFG-PROD-002 | Backend | **`spring.jpa.show-sql` not explicitly disabled in container profiles** — some services rely on default `false` but don't explicitly set it. Risk of SQL logging in production if default changes. | ⏳ Open |
| CFG-PROD-003 | Backend | **Tracing probability `0.1` (10%) may be insufficient** — for debugging production issues, 10% sampling means 90% of traces are lost. Consider 100% for errors, 10% for success (head-based sampling with tail-based for errors). | ⏳ Open |
| K8S-013 | Infra | **No `podDisruptionBudget` for web-app in prod** — PDB exists in base but prod overlay sets 3 replicas. With `minAvailable: 1`, 2 pods can be evicted simultaneously leaving only 1 serving traffic. Should be `minAvailable: 2` for prod. | ⏳ Open |
| K8S-014 | Infra | **No resource quotas referenced in workload overlays** — namespace-level ResourceQuotas exist in `foundation/namespaces/` but workload deployments don't account for them. Risk of deployment failures if quotas are tight. | ⏳ Open |

---

### 📊 Infrastructure Readiness Matrix (May 15, 2026)

| Area | Status | Detail |
|:-----|:------:|:-------|
| Security Context | ✅ | Non-root + readOnlyFS + drop ALL + seccompProfile RuntimeDefault |
| Secrets Management | ✅ | Prod overlay patches all passwords to secretKeyRef |
| Startup Probes | ✅ | All 24 deployments have startupProbe |
| Topology Spread | ✅ | All deployments have topologySpreadConstraints |
| Service Accounts | ✅ | All 24 services have dedicated SA with automountServiceAccountToken: false |
| HPA/VPA/PDB | ✅ | HPA + PDB referenced in kustomization, VPA set to Off (recommendation-only) |
| Graceful Shutdown | ✅ | terminationGracePeriodSeconds set (60s Java, 30s Python/Node) |
| Network Policies | ⚠️ | Default-deny exists at namespace level, intra-namespace allow exists, but no per-service segmentation |
| Prod Resource Limits | ✅ | Prod overlay patches to 512Mi-1536Mi via labelSelector |
| Containerfile Best Practices | ✅ | Non-root, UBI9, HEALTHCHECK, explicit app.jar, finalName in pom.xml |
| Flyway Validation | ✅ | validate-on-migrate: true in all container profiles |
| Health Endpoint Exposure | ✅ | show-details: when-authorized across all services |

---

_Last Updated: May 15, 2026 — Cleaned up: all ✅ Fixed items archived. Only open/deferred items remain._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
