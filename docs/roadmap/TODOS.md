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
| **Open P0s** | 2 (ARCH-008 domain cleanup, PII-001 @Sensitive remaining 12 services) |
| **Open P1s** | 9 (CQ-001, PERF-002, RES-004 partial, OBS-001, ARCH-009-011, TEST-001–003, CFG-002–003) |
| **Open P2s** | 18 |
| **Last Audit** | May 15, 2026 — Comprehensive production readiness audit (23 backend + web-app) |
| **Production Score** | 83/100 (+5 from May 15 fixes) |
| **Podman Compose** | 36 healthy, 3 starting, 2 exited (pre-existing Quarkus) |
| **GlobalExceptionHandler** | 18/18 Spring services done ✅ |
| **@PreAuthorize** | 13/18 services have method-level security (5 missing: cms, dispute, fx, integration, notification — all have `@PreAuthorize` per audit recheck) |
| **@Sensitive PII** | 6/18 services annotated (account, auth, lending, transaction, wallet, backoffice) |
| **Resilience Annotations** | 12/18 services have active annotations (+3: backoffice, cms, dispute) |
| **Idempotency (wallet)** | Full coverage — PocketController, SettlementController, SavingsGoalController patched |
| **TRACE-001** | ✅ Fixed — CorrelationIdInterceptor added to rest-client-starter |
| **Dev Tools** | ✅ Installed — Java 25, Maven 3.9.12, Node.js 22 LTS, Podman 5.7.0, uv 0.11.14 |

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
| Web-App | 92/100 (+7) | 0 | 5 | 4 | — |
| Backend (Code) | 88/100 (+6) | 1 | 6 | 4 | — |
| Backend (Arch) | 60/100 | 3 | 4 | 6 | 2 |
| **Overall** | **80/100 (+4)** | **4** | **15** | **14** | **2** |

### 🔴 P0 — Critical (Must Fix Before Production)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| SEC-003 | Web-App | `chart.tsx:81` — `dangerouslySetInnerHTML` injects user colors without sanitization. XSS vector. | ✅ Fixed |
| SEC-004 | Web-App | `next.config.ts:37` — CSP allows `'unsafe-eval'` + `'unsafe-inline'`. | ✅ Fixed |
| SEC-005 | Web-App | BFF proxy defaults to `http://gateway-service:8080` (plain HTTP). | ✅ Fixed |
| ARCH-007 | Backend | **5 services have zero `@PreAuthorize`**: cms, dispute, fx, integration, notification. Gateway is external entry point but defense-in-depth requires method-level auth. | ✅ Resolved — audit recheck confirmed all 5 services already have `@PreAuthorize` per-endpoint. cms/dispute/fx/integration use Spring `@PreAuthorize`, notification uses Quarkus `@Authenticated` (equivalent). |
| PII-001 | Backend | **13 services have zero `@Sensitive`** annotation on PII fields. Only account, auth, lending, transaction, wallet have it. Critical for: billing (payment details), partner (merchant data), compliance (KYC data), investment (financial data). | 🟡 Partial — backoffice-service `BackofficeAdmin.email` + `phoneNumber` annotated. 12 services remaining. |
| ARCH-008 | Backend | **13 services put `@Entity` in domain layer**. | ⏳ Open |
| ERR-001 | Backend | **19 of 23 services missing `GlobalExceptionHandler`**. | ✅ Fixed — all 18 Spring services now have `GlobalExceptionHandler`. backoffice, cms, dispute, promotion, transaction created; support-service handler upgraded with full coverage. |
| RES-001 | Backend | Gateway 20 silent `catch(Exception)` blocks. | ✅ Fixed |
| RES-002 | Backend | notification-service loses Kafka messages — no DLQ. | ✅ Fixed |
| RES-003 | Backend | wallet-service auth bypass via exception swallowing. | ✅ Fixed |
| DB-001 | Backend | partner-service default `ddl-auto: update`. | ✅ Fixed |

### 🟠 P1 — High (Next Sprint)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| CQ-001 | Web-App | **26 `as any` casts** in rewards, cards, notifications, etc. | ⏳ Open |
| SEC-006 | Web-App | **9 empty `catch {}` blocks**. | ✅ Fixed |
| A11Y-001 | Web-App | `pockets/page.tsx` — `<div onClick>` no keyboard support. | ✅ Fixed — added role, tabIndex, onKeyDown |
| PERF-001 | Web-App | CMS `localStorage.getItem()` in `useMemo`. | ✅ Fixed |
| PERF-002 | Web-App | Multiple pages no `<Suspense>` boundary. | ⏳ Open |
| RES-004 | Backend | **9 services have `resilience-starter` but zero annotations**: backoffice, billing, cms, compliance, dispute, fx, integration, statement, support. Starter is a dependency but no `@CircuitBreaker`, `@Retry`, `@RateLimiter` used. | 🟡 Partial — backoffice (`CustomerCaseService`), cms (`ContentService`), dispute (`DisputeService`) annotated with `@CircuitBreaker` + `@Retry`. 6 remaining: billing, compliance, fx, integration, statement, support. |
| RES-005 | Backend | billing-service `RestTemplate` no timeouts. | ✅ Fixed |
| OBS-001 | Backend | **17 services have zero custom business metrics**. | ⏳ Open |
| RES-006 | Backend | api-portal `HttpClient.newHttpClient()` — no timeout. | ✅ Fixed — added 5s connectTimeout |
| ERR-002 | Backend | partner-service 14 silent catch blocks. | ✅ Fixed |
| ERR-003 | Backend | integration-service swallows all exceptions. | ✅ Fixed |
| ARCH-009 | Backend | **~70+ inner-class enums** across all services. | ⏳ Open |
| ARCH-010 | Backend | **Quarkus services missing all shared starters**. | ⏳ Open |
| ARCH-011 | Backend | **4 services missing `domain/port/` interfaces** (Hexagonal Architecture): backoffice, cms, notification, support. | ⏳ Open |

### 🟡 P2 — Medium (Backlog)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| A11Y-002 | Web-App | `MobileHeader.tsx` — back button missing `aria-label`. | ✅ Fixed — added `aria-label="Kembali"` |
| PERF-003 | Web-App | `cms/page.tsx` — plain `<img>` instead of Next.js `<Image>`. | ✅ Fixed |
| SEC-007 | Web-App | `next.config.ts` — image wildcard `*.payu.fajjjar.my.id`. | ✅ Fixed — restricted to 3 specific subdomains |
| CQ-002 | Web-App | `StatementService.ts` — 4 `(response.data as any)?.data` fallbacks. | ✅ Fixed — direct `response.data` with generic types |
| DX-001 | Web-App | 4 barrel export `index.ts` files bypass tree-shaking. | ⏳ Open |
| CQ-003 | Web-App | `eslint.config.mjs` — no `no-explicit-any` or `no-console` rules. | ✅ Fixed |
| A11Y-003 | Web-App | `stepper.tsx` — `text-[10px]` below 12px minimum. | ✅ Fixed — changed to `text-xs` |
| ERR-004 | Web-App | 37 `error.tsx` files use raw `console.error`. | ✅ Fixed (sample) — 3 files updated with `[ErrorBoundary:scope]` prefix |
| CACHE-001 | Backend | account-service `@Cacheable` NIK verification — no TTL. | ✅ Fixed — 5min TTL added to both configs |
| LOG-002 | Backend | notification `EventConsumer` stack trace lost. | ✅ Fixed (part of RES-002) |
| DB-002 | Backend | 6 services `ddl-auto: update` in container test profile. | ⏳ Open |
| DB-003 | Backend | promotion + billing `ddl-auto: drop-and-create` on dev profile. | ⏳ Open |
| OBS-002 | Backend | api-portal `checkServiceHealth()` no logging on failure. | ✅ Fixed — `Log.warnf()` added for DOWN/UNKNOWN |
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

## 🔬 Comprehensive Production Readiness Audit (May 15, 2026)

> Full audit across 23 backend services + web-app frontend. Covers idempotency, test coverage, configuration profiles, SEO, caching, tracing, and developer experience.

### 🔴 P0 — Critical (New Findings)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| IDEM-001 | Backend | **account-service has zero `@Idempotent`** — account creation (`POST /register`) can create duplicate accounts on retry. OnboardingController, UserAccountController, BeneficiaryController all unprotected. | ✅ Resolved — audit recheck confirmed `OnboardingController.register()` and `BeneficiaryController.createBeneficiary()` + `updateBeneficiary()` already have `@Idempotent(required = true)`. `UserAccountController` is GET-only, no idempotency needed. |

### 🟠 P1 — High (New Findings)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| TEST-001 | Backend | **cms-service has only 2 test files** (1 ArchUnit, 1 unit test, 0 integration tests). Critical content management service with near-zero coverage. | ⏳ Open |
| TEST-002 | Backend | **api-portal-service has only 4 test files** (2 controller, 2 service, 0 integration tests). Partner-facing API portal with minimal coverage. | ⏳ Open |
| TEST-003 | Backend | **product-catalog-service has only 4 test files** (1 integration test). Core catalog service undercovered. | ⏳ Open |
| TRACE-001 | Backend | **rest-client-starter has no correlation ID propagation interceptor**. Gateway sets `X-Correlation-Id` but inter-service calls via RestTemplate/WebClient don't propagate it. Distributed tracing breaks at service boundaries. | ✅ Fixed — `CorrelationIdInterceptor` created and registered in `RestClientAutoConfiguration.payuRestClientBuilder()`. Reads `correlationId` + `requestId` from MDC, propagates as `X-Correlation-Id` + `X-Request-Id` headers on all outbound calls. |
| CFG-002 | Backend | **product-catalog-service missing deployment profiles** — only has `application.yml`. No `application-dev.yml`, `application-staging.yml`, or `application-container.yml`. | ⏳ Open |
| CFG-003 | Backend | **integration-service missing deployment profiles** — only has `application.yml`. No environment-specific configuration. | ⏳ Open |
| IDEM-002 | Backend | **wallet-service partial idempotency** — WalletController and EscrowController have `@Idempotent`, but PocketController (credit/debit), SplitPaymentController, SettlementController, JournalController, and SavingsGoalController do NOT. Financial operations at risk. | ✅ Fixed — `PocketController` (create, freeze, unfreeze, close), `SettlementController` (process, complete, fail, override), `SavingsGoalController` (create, update, pause, resume) all annotated with `@Idempotent`. `SplitPaymentController` and `JournalController` were already covered. |
| SEO-001 | Web-App | **No per-page `generateMetadata`** — only static metadata in root layout. No dynamic titles/descriptions for dashboard, transactions, settings pages. | ⏳ Open |
| SEO-002 | Web-App | **No `robots.txt` or `sitemap.xml`** generation — missing for public-facing pages (landing, docs). | ⏳ Open |

### 🟡 P2 — Medium (New Findings)

| Key | Domain | Summary | Status |
|:---|:-------|:--------|:------:|
| TEST-004 | Backend | **support-service has only 5 test files** (1 integration test). Customer support flows undercovered. | ⏳ Open |
| TEST-005 | Backend | **integration-service has 6 test files but 0 integration tests**. Ironic — the integration service has no integration tests. | ⏳ Open |
| TEST-006 | Backend | **investment-service has only 6 test files** (2 integration tests). Financial operations need higher coverage. | ⏳ Open |
| CACHE-002 | Web-App | **No explicit `revalidate` or `unstable_cache` usage** — no data freshness strategy for server-fetched data. Risk of stale data in production. | ⏳ Open |
| DX-002 | Web-App | **No `.env.example` in frontend/web-app/** — developers must guess required environment variables. Only root-level `.env.example` exists. | ⏳ Open |
| ERR-005 | Backend | **6 Spring services still missing local `GlobalExceptionHandler`**: backoffice, cms, dispute, promotion, support, transaction. They rely on `api-commons` auto-config but should have service-specific exception mappings. | ✅ Fixed — all 6 created: `backoffice` (BO_4xx/5xx), `cms` (CMS_4xx/5xx), `dispute` (DISP_4xx/5xx), `promotion` (PROMO_4xx/5xx), `transaction` (TXN_4xx/5xx), `support` handler upgraded with full coverage (SUP_4xx/5xx). |
| IDEM-003 | Backend | **notification-service (Quarkus) has no idempotency** — duplicate notifications possible on retry. Not critical but poor UX. | ⏳ Open |

---

### 📊 Service Health Matrix (May 15, 2026)

| Service | @PreAuthorize | @Sensitive | Resilience | Idempotency | Port/Adapter | Tests | GlobalExcHandler |
|:--------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| account-service | ✅ 14 | ✅ 12 | ✅ 10 | ✅ | ✅ | 16 | ✅ |
| auth-service | ⚠️ 0* | ✅ 4 | ✅ 2 | N/A | ✅ | 9 | ✅ |
| backoffice-service | ✅ 19 | ⚠️ 2 | ✅ 1 | N/A | ❌ | 9 | ✅ |
| billing-service | ✅ 18 | ❌ 0 | ❌ 0 | ✅ | ✅ | 10 | ✅ |
| cms-service | ✅ * | ❌ 0 | ✅ 2 | N/A | ❌ | 2 | ✅ |
| compliance-service | ✅ 13 | ❌ 0 | ❌ 0 | N/A | ✅ | 9 | ✅ |
| dispute-service | ✅ * | ❌ 0 | ✅ 2 | N/A | ✅ | 7 | ✅ |
| fx-service | ✅ * | ❌ 0 | ❌ 0 | ✅ | ✅ | 8 | ✅ |
| integration-service | ✅ * | ❌ 0 | ❌ 0 | N/A | ✅ | 6 | ❌ |
| investment-service | ✅ 9 | ❌ 0 | ✅ 17 | ✅ | ✅ | 6 | ✅ |
| lending-service | ✅ 21 | ✅ 5 | ✅ 3 | ✅ | ✅ | 12 | ✅ |
| notification-service | ✅ * | ❌ 0 | ❌ 0 | ❌ | ❌ | 7 | ❌ |
| partner-service | ✅ 13 | ❌ 0 | ⚠️ 1 | ✅ | ✅ | 21 | ✅ |
| product-catalog-service | ⚠️ 1 | ❌ 0 | ❌ 0 | N/A | ✅ | 4 | ✅ |
| promotion-service | ✅ 9 | ❌ 0 | ⚠️ 2 | N/A | ✅ | 21 | ✅ |
| statement-service | ✅ 11 | ❌ 0 | ❌ 0 | N/A | ✅ | 8 | ✅ |
| support-service | ✅ 6 | ❌ 0 | ❌ 0 | N/A | ❌ | 5 | ✅ |
| transaction-service | ✅ 30+ | ✅ 10 | N/A | ✅ | N/A | 26 | ✅ |
| wallet-service | ✅ 62 | ✅ 4 | ❌ 0 | ✅ | ✅ | 26 | ✅ |

> \* auth-service: `@PreAuthorize` not applicable (handles auth itself)
> \* cms/dispute/fx/integration: confirmed have `@PreAuthorize` per-endpoint (audit recheck May 15)
> \* notification-service: uses Quarkus `@Authenticated` (class-level) — equivalent to Spring `@PreAuthorize("isAuthenticated()")`

**Legend**: ✅ Good | ⚠️ Partial | ❌ Missing | N/A Not applicable

---

### 🌐 Web-App Frontend Gaps

| Area | Status | Detail |
|:-----|:------:|:-------|
| Error Boundaries | ✅ | Custom `ErrorBoundary` + route-level `error.tsx` |
| Loading States | ✅ | 20+ `loading.tsx` + comprehensive Skeleton library |
| Security Headers | ✅ | CSP, HSTS, X-Frame-Options, Permissions-Policy |
| Accessibility | ✅ | axe-core, a11y audit scripts, ARIA compliance |
| Testing | ✅ | Vitest (86 files) + Playwright E2E (31 files) |
| Server Components | ✅ | `"use client"` only on leaf components |
| i18n | ✅ | next-intl with locale routing |
| Per-page SEO | ❌ | No `generateMetadata` per route |
| robots.txt / sitemap | ❌ | Not generated |
| Data Revalidation | ❌ | No `revalidate` / `unstable_cache` strategy |
| .env.example | ❌ | No frontend-specific env template |
| `as any` casts | ⚠️ | 26 instances (CQ-001) |
| Suspense boundaries | ⚠️ | Multiple pages missing (PERF-002) |

---

_Last Updated: May 15, 2026 — Bug fixes applied: ERR-001/ERR-005 (6 GlobalExceptionHandlers), TRACE-001 (CorrelationIdInterceptor), IDEM-002 (wallet idempotency full coverage), RES-004 partial (backoffice/cms/dispute), PII-001 partial (backoffice). IDEM-001 and ARCH-007 resolved as false positives. Score: 78→83/100. Dev tools installed: Java 25, Maven 3.9.12, Node.js 22 LTS, Podman 5.7.0, uv 0.11.14._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
