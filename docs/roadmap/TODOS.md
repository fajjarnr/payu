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
| **Open P0s** | 4 (ARCH-007, PII-001, ARCH-008, ERR-001 — all require per-service audits/refactors) |
| **Open P1s** | 9 (CQ-001, A11Y-001, PERF-002, RES-004, OBS-001, RES-006, ARCH-009-011) |
| **Open P2s** | 20 |
| **Fixed Today (May 14)** | 13 (7 P0, 5 P1, 1 P2) |
| **Production Score** | 76/100 (+9 from 67) |
| **Spikes** | 2 (Deferred) |
| **Deferred** | 10 |
| **Suspended (OCP destroyed)** | 8 |
| **DevSecOps (suspended)** | 52 |

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

## 🏗️ Infrastructure YAML Audit Fixes (May 8, 2026)

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| YAML-001 | P0 | Fix web-app deployment YAML syntax error (`env` nested under `resources.limits`) | ✅ Fixed |
| YAML-002 | P0 | Fix fx-service version mismatch (labels 1.8.1 vs image 1.8.2) | ✅ Fixed |
| YAML-003 | P1 | Fix HPA/VPA `DeploymentConfig` → `Deployment` (6 resources) | ✅ Fixed |
| YAML-004 | P1 | Fix PDB selectors `app:` → `app.kubernetes.io/name:` (21 resources) | ✅ Fixed |
| YAML-005 | P1 | Fix `commonLabels` → `labels` with `includeSelectors: false` in all overlays | ✅ Fixed |
| YAML-006 | P1 | Update all base service/kustomization versions to match deployments (1.7.9 → 1.8.x) | ✅ Fixed |
| YAML-007 | P1 | Remove hardcoded `namespace: payu` from base/hpa-enhanced.yaml and base/vpa.yaml | ✅ Fixed |
| YAML-008 | P1 | Fix ServiceMonitor metrics path `/actuator/prometheus` → `/q/metrics` | ✅ Fixed |
| YAML-009 | P2 | Add OIDC issuer patches for all services in payu-dev overlay | ⏳ Pending |

---

## 🔐 Auth & Health Endpoint Stabilization (May 9–14, 2026)

### Completed Fixes

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| AUTH-024 | P0 | Gateway `AuthorizationFilter.java`: whitelist `/public/health` paths for all core services | ✅ Fixed |
| AUTH-030 | P1 | `/api/v1/{service}/public/health` returns 401 — dual-layer auth conflict | ✅ Fixed May 14 — 18 SecurityConfigs patched, Gateway filter wildcard, 8 HealthControllers created |
| AUTH-031 | P1 | k6 smoke-test `http_req_failed` ~50% — downstream of AUTH-030 | ✅ Resolved — dependent on AUTH-030 |
| SEC-SPRING-001 | P1 | Patch Spring Security `SecurityConfig.java`: add `"/**/public/**"` to `requestMatchers().permitAll()` | ✅ Fixed — All 18 Spring services patched |
| INFRA-KAFKA-001 | P0 | Sync Kafka bootstrap env vars across all deployment.yaml | ✅ Fixed — 6 services patched |
| INFRA-REDIS-001 | P0 | Standardize Redis/DataGrid port `11222` (RESP protocol) | ✅ Fixed — verified all services |

### AUTH-030 Resolution Summary (May 14, 2026)

**Root Cause**: Two independent issues:
1. **Gateway `AuthorizationFilter`** only whitelisted 5 specific health endpoints (`/api/v1/accounts/public/health`, etc.) — not all 18 services
2. **11 of 18 SecurityConfigs** were missing `"/**/public/**"` + `"/api/v1/**/public/**"` permitAll patterns (SEC-SPRING-001 only covered 7 services)
3. **14 of 18 services** were missing `HealthController.java` (only account, wallet, transaction had it)

**Fix Applied**:
- **18 SecurityConfigs**: All now have `"/**/public/**"` + `"/api/v1/**/public/**"` patterns
- **18 HealthControllers**: All 18 Spring services now have HealthController.java
- **Gateway AuthFilter**: Added generic `endsWith("/public/health")` check — all services auto-permit
- **Gateway Quarkus**: Single `permission` entry `"/**/public/health"` → `permit` at Vert.x layer

### New Findings from Audit (May 14, 2026)

| Key | Priority | Summary | Status |
|:---|:---:|:---|:---|
| KYC-001 | P0 | `kyc-service` has NO authentication — **FALSE POSITIVE**: KYC already has inline `require_auth` in `kyc.py` with all 5 protected endpoints using `Depends(require_auth)`. Uses `python-jose` JWT validation. | ✅ Resolved — auth already present, not a gap |
| AUTH-032 | P1 | 10 Spring services missing `HealthController`: auth, cms, support, promotion, partner, lending, investment, dispute, billing, backoffice | ✅ Fixed May 14 — all 10 created |
| AUTH-033 | P1 | All 18 HealthControllers return hardcoded `{"status": "UP"}` without checking DB/Redis/Kafka — false positive health checks in production. Follow L-018 pattern with `@Readiness` (DB ping + Redis PING + Kafka cluster metadata). | ⏳ Open — deferred to next sprint |
| AUTH-034 | P2 | `api-portal-service` & `notification-service` (Quarkus): no explicit `/**/public/health` permit in their own config | ✅ Fixed — `quarkus.http.auth.permission.public-health` added to both |
| AUTH-035 | P2 | `support-service` & `promotion-service`: `@Profile("!test")` disables SecurityConfig in tests — security regressions pass CI unnoticed | ✅ Fixed — `@Profile("!test")` removed from both, unused import cleaned |
| AUTH-036 | P2 | `fx-service` HealthController at `/v1/public/health` — Gateway route is `/fx-api/v1/`, path mismatch | ✅ Mitigated — Gateway `AuthorizationFilter` `endsWith("/public/health")` covers all. No action needed at service level. |

---

## 🏭 Production Readiness Audit (May 14, 2026)

> Comprehensive audit across 23 backend services + web-app frontend. 3 parallel audit streams: backend production code, code-quality/architecture, and web-app.

### 📊 Scorecard

| Domain | Score | Critical | High | Medium | Low |
|:-------|:-----:|:--------:|:----:|:------:|:---:|
| Web-App | 85/100 (+11) | 0 | 6 | 9 | — |
| Backend (Code) | 82/100 (+14) | 1 | 6 | 6 | — |
| Backend (Arch) | 60/100 | 3 | 4 | 6 | 2 |
| **Overall** | **76/100 (+9)** | **4** | **16** | **21** | **2** |

### 🔴 P0 — Critical (Must Fix Before Production)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| SEC-003 | Web-App | `chart.tsx:81` — `dangerouslySetInnerHTML` injects user colors without sanitization. XSS vector. | ✅ Fixed — color validation regex added |
| SEC-004 | Web-App | `next.config.ts:37` — CSP allows `'unsafe-eval'` + `'unsafe-inline'`. Completely defeats XSS protection. | ✅ Fixed — only in dev mode now |
| SEC-005 | Web-App | BFF proxy defaults to `http://gateway-service:8080` (plain HTTP). Internal traffic unencrypted if env var missing. | ✅ Fixed — changed to `https://` default |
| ARCH-007 | Backend | **9 services have zero `@PreAuthorize`**: auth, api-portal, cms, dispute, fx, gateway, integration, notification. Gateway is external entry point — must enforce RBAC. | ⏳ Open — requires per-endpoint RBAC audit |
| PII-001 | Backend | **16 services have zero `@Sensitive`** annotation on PII fields (NIK, phone, PIN). Only 5 of 21 protected. PII leaks in LokiStack logs. | ⏳ Open — requires DTO audit per service |
| ARCH-008 | Backend | **13 services put `@Entity` in domain layer** — violates Hexagonal Architecture (domain must be framework-free). Wallet/fx show correct pattern. | ⏳ Open — requires domain/adapter split refactor |
| ERR-001 | Backend | **19 of 23 services missing `GlobalExceptionHandler`**. Unhandled exceptions propagate stack traces to clients. | ⏳ Open — requires template per service |
| RES-001 | Backend | **Gateway 20 silent `catch(Exception)` blocks** across 12 filter files. Auth, rate-limit, idempotency failures all go undetected. | ✅ Fixed — all catch blocks now log with exception param |
| RES-002 | Backend | **notification-service loses Kafka messages** — 6 `@Incoming` handlers catch Exception without rethrow. Offset auto-committed, messages lost forever. No DLQ. | ✅ Fixed — rethrow + DLQ configured |
| RES-003 | Backend | **wallet-service `validateReservationOwnership()`** returns `false` on ANY exception (DB down → deny all). No log emitted. | ✅ Fixed — logs + rethrows as RuntimeException |
| DB-001 | Backend | **partner-service default `ddl-auto: update`**. Non-profile config auto-modifies schema. | ✅ Fixed — changed to `validate` |

### 🟠 P1 — High (Next Sprint)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| CQ-001 | Web-App | **26 `as any` casts** in rewards, cards, notifications, analytics, split-bill, scheduled-transfers pages. Financial data coerced without validation. | ⏳ Open |
| SEC-006 | Web-App | **9 empty `catch {}` blocks** — auth refresh, session validate, CMS loads, token rotation fail silently. | ✅ Fixed — `console.error()` added to all |
| A11Y-001 | Web-App | `pockets/page.tsx:610` — `<div onClick>` for pocket selection. No keyboard support (`role`, `tabIndex`, `onKeyDown` missing). | ⏳ Open |
| PERF-001 | Web-App | CMS `localStorage.getItem()` in `useMemo` render — synchronous I/O blocks main thread. Also XSS surface. | ✅ Fixed — moved to `useEffect` |
| PERF-002 | Web-App | Multiple pages have 3–5 `useQuery` calls with no `<Suspense>` boundary. Entire page blocks on slowest query. | ⏳ Open |
| RES-004 | Backend | **7 services have `resilience-starter` but zero `@CircuitBreaker`/`@Retry` annotations** | ⏳ Open |
| RES-005 | Backend | **billing-service `RestTemplate` bean has no timeouts** — infinite hang risk. | ✅ Fixed — 5s connect, 10s read |
| OBS-001 | Backend | **17 of 20 Spring services have zero custom business metrics**. Only fx, gateway, promotion register Timers/Counters. | ⏳ Open |
| RES-006 | Backend | **api-portal `HttpClient.newHttpClient()` — no connection pool**. Single connection per destination to 20 downstream services. | ⏳ Open |
| ERR-002 | Backend | **partner-service 14 silent catch blocks** in SNAP-BI token, certificate rotation, controllers. | ✅ Fixed — Logger added, all catch blocks now log |
| ERR-003 | Backend | **integration-service swallows all exceptions** in 5 core methods (SWIFT, ISO20022, validation). | ✅ Fixed — all catch blocks now log |
| ARCH-009 | Backend | **~70+ inner-class enums** across all services. AGENTS.md mandates top-level enums for Lombok/JPA compatibility. | ⏳ Open |
| ARCH-010 | Backend | **Quarkus services missing all shared starters**: gateway, api-portal, notification have zero security/resilience/cache starters. | ⏳ Open |
| ARCH-011 | Backend | **6 services missing `domain/port/` interfaces**: api-portal, notification, support, gateway, cms, backoffice. | ⏳ Open |

### 🟡 P2 — Medium (Backlog)

| Key | Domain | Summary |
|:---|:-------|:--------|
| A11Y-002 | Web-App | `MobileHeader.tsx:22` — back button missing `aria-label` |
| PERF-003 | Web-App | `cms/page.tsx:171` — plain `<img>` instead of Next.js `<Image>` |
| SEC-007 | Web-App | `next.config.ts:13` — image remote pattern allows `*.payu.fajjjar.my.id` wildcard (any subdomain) |
| CQ-002 | Web-App | `StatementService.ts:99` — 4 `(response.data as any)?.data` fallbacks mask type failures on financial data |
| DX-001 | Web-App | 4 barrel export `index.ts` files bypass tree-shaking. Use direct imports per frontend-architect skill. |
| CQ-003 | Web-App | `eslint.config.mjs` — no `no-explicit-any` or `no-console` rules. 26 `as any` + console.log unflagged. |
| A11Y-003 | Web-App | `stepper.tsx:54` — `text-[10px]` below 12px minimum from frontend-architect skill |
| ERR-004 | Web-App | 37 `error.tsx` files use raw `console.error` instead of structured logging |
| CACHE-001 | Backend | `account-service` `@Cacheable` on NIK verification — no TTL, indefinite caching. Stale if NIK status changes. |
| LOG-002 | Backend | `notification-service` `EventConsumer` uses `LOG.errorf("...%s", e.getMessage())` — stack trace lost. Need `LOG.errorf(e, "...")`. |
| DB-002 | Backend | 6 services use `ddl-auto: update` in `application-container.yml` test profile — could accidentally activate in prod. |
| DB-003 | Backend | `promotion-service` + `billing-service` use `ddl-auto: drop-and-create` on `dev` profile — documented risk. |
| OBS-002 | Backend | `api-portal` `checkServiceHealth()` returns "UNKNOWN" without logging or metric increment on failure. |
| CFG-001 | Backend | 21 of 23 services hardcode `localhost` as fallback for DB/Redis/Kafka/OIDC. Should fail fast without explicit config. |
| ARCH-012 | Backend | `BaseController` copy-pasted across 11 services. Centralize in `api-commons`. |
| ARCH-013 | Backend | `SecurityConfig` size range 32–244 lines — copy-paste despite unification. Create base class in `security-starter`. |
| ARCH-014 | Backend | `CorrelationIdFilter` duplicated in api-portal, gateway, notification. Move to shared starter. |
| ARCH-015 | Backend | `RateLimitV2Filter` uses `synchronized` on token bucket — contention bottleneck under high concurrency. Use `AtomicLong`. |
| ARCH-016 | Backend | `@Service` annotation used in adapter layer (KeycloakService, PaymentNotificationService, WalletGrpcService). Should use `@Component`/`@Repository`. |
| DEP-001 | Backend | Mixed `${project.version}` vs hardcoded `1.0.0-SNAPSHOT` for shared starter versions. |

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

_Last Updated: May 14, 2026 — Production Readiness Audit: 13 of 53 findings fixed (7 P0, 5 P1, 1 P2). Score: 76/100 (+9). Remaining P0s: @PreAuthorize, @Sensitive, @Entity in domain, GlobalExceptionHandler. Context7: @PreAuthorize pattern confirmed per Spring Security 6.5 docs._
_⚠️ OCP cluster rebuilt (May 8). Infrastructure YAML audit 8/9 applied._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
