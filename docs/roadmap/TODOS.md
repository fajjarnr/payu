# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| **Open Bugs** | 0 | ✅ FE-107/108/109/110 + CROSS-074 + AUTH-035 all closed (May 5, 2026) |
| **Open Epics** | 0 | 24/24 fully done |
| **Open Stories** | 0 | 109 done, 265/265 SP delivered |

> **Completed work**: See [`CHANGELOG.md`](../../CHANGELOG.md) for bug fix history and [`PROGRESS.md`](./PROGRESS.md) for epics & DORA metrics.

---

## 🐞 Open Bugs

> **Current open bug count: 0**. All 6 bugs resolved (May 5, 2026).  
> Historical bug details archived to [`CHANGELOG.md`](../../CHANGELOG.md).

---

## 🚀 Framework & Infrastructure Upgrades (May 5, 2026)

> All upgrades completed except mobile. Details archived to [`CHANGELOG.md`](../../CHANGELOG.md).

| Key           | Priority | Category       | Summary                                                                                                                                                                 | Status   |
| :------------ | :------: | :------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| UPGRADE-012   |    P2    | Mobile         | **Modernize Mobile App**: Upgrade to **Expo SDK 55** and **React Native 0.85** for performance and New Architecture support.                                            | ⏸️ Skipped |

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                                                                                                | Impact                               | Status   |
| :------- | :---- | :-------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------- | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                                                                                                  | Scope `kyc-service`                  | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?                                                                                   | Output format `statement-service`    | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client?                                                                                      | Multi-tenancy `support-service`      | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client?                                                                               | Multi-tenant mode `cms-service`      | 📋 To Do |
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection                                                   | ADR-0015, `rules-starter` shared lib | 📋 To Do |
| ARCH-006 | Spike | Spring Boot 4.0 & Jakarta EE 11 Migration Strategy: Audit Spring Cloud compatibility (specifically Vault) before platform-wide rollout. | Oakwood Release Train                | 📋 To Do |

---

## 🔮 Deferred (Icebox)

| Key       | Type  | Summary                                                           | Notes                                            |
| :-------- | :---- | :---------------------------------------------------------------- | :----------------------------------------------- |
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN)                               | ❄️ Deferred                                      |
| OCP-007   | Story | Service Mesh mTLS enforcement                                     | ❄️ Planned                                       |
| OCP-010   | Story | API versioning headers                                            | ❄️ Planned                                       |
| DR-001    | Story | Disaster Recovery live test execution                             | ❄️ Scripts ready                                 |
| DEFER-001 | Story | Card Tokenization & 3DS                                           | ❄️ Requires PCI-DSS scope + card network kontrak |
| RHPAM-001 | Story | Phase 1: Create `shared/rules-starter` (Drools 9.x embedded)      | ❄️ Depends on ARCH-005 PoC. See ADR-0015         |
| RHPAM-002 | Story | Phase 2: Migrate `lending-service` credit scoring ke DRL rules    | ❄️ Depends on RHPAM-001                          |
| RHPAM-003 | Story | Phase 3: Payment routing DMN decision tables di `gateway-service` | ❄️ Depends on RHPAM-001                          |
| RHPAM-004 | Story | Phase 4: Lending workflow + KYC/AML BPMN orchestration (Kogito)   | ❄️ Depends on RHPAM-002, evaluasi Q3 2026        |

---

## ⏭️ Operational Follow-Up (Resume Checklist)

| Key               | Type | Summary                                                                                                                                                                                                                                      | Notes / Current State                                                                                                                                                                                                                              | Status   |
| :---------------- | :--- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| OPS-2026-04-08-01 | Task | Validate that the new `wallet-service` rollout no longer emits `DistributedCacheService` wallet cache deserialization warnings.                                                                                                              | `cache-starter` compatibility fix added and `wallet-service` image rolled out; post-rollout in-cluster probe was interrupted before verification.                                                                                                  | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-08-02 | Task | Re-run the full 40-minute `tests/performance/k6/crud-stress-test.js` job via k6 Operator using `payu-crud-load` TestRun.                                                                                                                     | k6 Operator installed (April 9). Use: `kubectl apply -f infrastructure/openshift/infra/base/k6/crud-load-testrun.yaml -n payu-k6`. ClusterAutoscaler + MachineAutoscalers now configured to scale up to 5 workers in us-east-2a + 4 each in 2b/2c. | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-08-03 | Task | If full stress still breaches `http_req_duration p(99) < 10s`, isolate the slow endpoint from gateway, wallet, and account logs during the same run window.                                                                                  | k6 Operator runner logs available via `kubectl logs -n payu-k6 -l runner=payu-crud-stress`. Auth/cache instability improved after `payu-datagrid` 512Mi→1Gi fix.                                                                                   | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-08-04 | Task | Re-run `tests/performance/k6/crud-data-consistency-test.js` after stress revalidation.                                                                                                                                                       | Use: `kubectl apply -f infrastructure/openshift/infra/base/k6/crud-consistency-testrun.yaml -n payu-k6`. Consistency canary already passed with test-mode/bypass flow.                                                                             | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-08-05 | Task | Decide whether to disable `GATEWAY_RATE_LIMIT_TEST_MODE` in `payu-dev` after final validation, then record the final cluster/test outcome in roadmap docs.                                                                                   | Test mode still enabled for controlled k6 validation. After final k6 Operator run, update this item and CHANGELOG.                                                                                                                                 | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-09-01 | Task | k6 Operator smoke test validated — runner pod executed 30 iterations/1 VU. HTTP failures expected (public DNS not reachable from pod network). Re-run with in-cluster service URLs or after confirming Istio ingress gateway routes.         | k6 Operator lifecycle verified: initializer → starter → runner → finished. ClusterAutoscaler live. Nodes: 7 (3 master, 2 infra, 2 worker).                                                                                                         | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-09-06 | Task | Transaction-service Redis/DataGrid connection issue — `ScheduledTransferScheduler` cannot connect to DataGrid RESP on port 11222. Affects Split Bill list (HTTP 500) and scheduled transfers.                                                | Lower priority — does not block core CRUD. May need DataGrid RESP config or NetworkPolicy fix for port 11222.                                                                                                                                      | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-04-09-07 | Task | Admin-only endpoints (GL, Settlement, Journal, ChartOfAccounts, Escrow, SplitPayment) require `ROLE_ADMIN` or `ROLE_BACKOFFICE`. Need to create admin Keycloak user or add realm roles for testing.                                          | Smart Routing also returns 404 — gateway doesn't route `/transfers/routes`.                                                                                                                                                                        | ⏸️ Suspended (OCP destroyed May 2) |
| OPS-2026-05-02-02 | Task | Run realistic k6 E2E CRUD test again after all services are healthy. Target: >95% checks pass, `http_req_failed` < 5%, all endpoints return 200.                                                                                         | ✅ **Done**: `podman compose -f infrastructure/local/podman/podman-compose.yml --profile devsecops run --rm k6` → 918/918 passed, 0% failure, p(95) 1.1ms. Gateway health endpoint fully healthy.                                                       | ✅ Done |
| OPS-2026-05-02-05 | Task | Document Tekton pipeline fix patterns in `docs/guides/LESSONS.md`: `onError: continue` Tekton v1.9 limitation, registry auth `unused:<token>` format, license compliance purl filtering.                                                  | ✅ **Done**: Lessons L-027, L-028, L-029, L-030 added to `docs/guides/LESSONS.md` with detailed patterns and code examples.                                                                                                            | ✅ Done |
| TEST-INFRA-006     |    P2    | Task | Playwright Chromium via snap execution ~2x lebih lambat dari native binary. | ✅ **Obsolete**: Switched to Google Chrome 147 via `channel: 'chrome'`. Full 652 suite verified, all spec files pass. | ✅ Done |
| OPS-2026-05-06-01 |    P0    | Task | **Real API validation — test all 23 services via curl.** Loop endpoints gateway: account, auth, tx, wallet, billing, notification, kyc, analytics, compliance, investment, lending, fx, statement, backoffice, partner, promotion, support, cms, product-catalog, dispute, integration, api-portal, gateway. | Token: `curl -s http://localhost:8080/api/v1/auth/login ...`. For each service, test GET/POST endpoints. Document which return 200 vs 401/403/500. Target: >80% endpoint pass rate. | 📋 To Do |

---

---

## 🔑 Account ID Mismatch Fix (2026-05-06)

> **Root cause**: 3 bugs causing transfer/wallet API failures. Wallet now works (balance 10M IDR). Transfer authorized but fails on Transaction entity optimistic locking.

| Key               | Priority | Summary | Status |
| :---------------- | :------: | :------ | :----- |
| ACC-ID-001 | P0 | Role case-sensitivity: SecurityConfig checks `contains("user")` but Keycloak role is `"USER"`. Fixed 3 services (transaction, wallet, account). | ✅ Fixed |
| ACC-ID-002 | P0 | Gateway `AuthorizationFilter.extractAccountId()` fallback `"account-" + sub` non-functional. Changed to return `sub` directly. | ✅ Fixed |
| ACC-ID-003 | P0 | `AuthorizationService.verifySenderAccountOwnership()` compares KC UUID against internal Account UUIDs (cross-type). Added direct KC UUID match. | ✅ Fixed |
| ACC-ID-004 | P1 | `customer1` seed data: `external_id="EXT-CUST-001"` ≠ KC UUID. No wallet. Fixed via SQL: update external_id, create wallet (10M IDR). | ✅ Fixed |
| ACC-ID-005 | P1 | Transfer fails on `ObjectOptimisticLockingFailureException` — Transaction entity pre-assigned UUID triggers `merge()` not `persist()`. Also: metadata jsonb mismatch, amount null, gRPC→REST adapter, ApiResponse parsing. | ✅ **Fixed**: `Persistable<UUID>` + builder fix + REST adapter + DB schema + wallet e2e verified (status: COMPLETED). |

---

## 🔌 OpenAPI / Swagger Audit (2026-05-06)

> **Audit**: 23 services checked. Only 3 fully functional (gateway, notification, api-portal). 8 blocked by Spring Security. 9 return 500. 3 not found. API-First is a platform requirement.

| Key               | Priority | Summary | Status |
| :---------------- | :------: | :------ | :----- |
| API-OPENAPI-001 | P0 | **Unblock Spring Security** for `/v3/api-docs`, `/swagger-ui/**` di 8 Spring Boot services (account, auth, tx, wallet, statement, compliance, integration, product-catalog). | 📋 To Do |
| API-OPENAPI-002 | P1 | **Fix 500 errors** on `/v3/api-docs` di 9 services (billing, investment, lending, cms, backoffice, partner, promotion, support, dispute). Likely dependency/startup issues. | 📋 To Do |
| API-OPENAPI-003 | P2 | **Add OpenAPI** to fx-service, dukcapil-simulator, qris-simulator (currently return 404). | 📋 To Do |
| API-OPENAPI-004 | P1 | **Gateway OpenAPI aggregation** — expose unified OpenAPI spec at `/q/openapi` combining all service schemas. | 📋 To Do |

---

## 🏗️ DevSecOps Architecture Implementation

> **Sumber**: [`infrastructure/DEVSECOPS_ARCHITECTURE.md`](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) v1.3.1 (Phase 1–4)
> Phase 1: ✅ COMPLETE (kecuali 3 DR/backup items). Phase 2: 🔄 IN PROGRESS. Phase 3–4: 📋 Belum dimulai.

### Phase 1 — Foundation (Sisa Tasks)

| Key       | Priority | Badge | Summary                                                                                          | Notes / Current State                                                                                                                              | Status   |
| :-------- | :------: | :---- | :----------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| INFRA-005 |    P0    | 🔵    | Configure Vault Raft auto-snapshot (1h interval) to encrypted S3 bucket                          | Phase 1 DR. Vault dev mode (`inmem`) confirmed data loss on pod restart. Need persistent snapshot strategy before production.                     | 📋 To Do |
| INFRA-006 |    P0    | 🔵    | Configure Vault auto-unseal (Transit or KMS)                                                     | Phase 1 DR. Currently manual unseal after restart. Auto-unseal needed for HA.                                                                      | 📋 To Do |
| INFRA-007 |    P1    | 🔵    | Document DR runbook for all critical components (Vault, ArgoCD, ACS, Wazuh)                    | Phase 1 DR. Vault DR script `scripts/vault-dr-restore.sh` exists; need runbooks for ArgoCD, ACS, Wazuh.                                           | 📋 To Do |

### Phase 2 — Hardening (In Progress)

| Key       | Priority | Badge | Summary                                                                                          | Notes / Current State                                                                                                                                            | Status   |
| :-------- | :------: | :---- | :----------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| INFRA-001 |    P0    | 🔵    | Fix `trivy-image-scan` registry auth for OpenShift internal registry                             | Dockerconfig workspace mounted but trivy image lacks `jq`; registry credential parsing fails. Blocks full pipeline green.                                       | 🔄 In Progress |
| INFRA-002 |    P0    | 🔵    | Build container images for remaining 22 services via Tekton PipelineRun                          | `account-service` image pushed successfully. Need 22 more services built and pushed to `image-registry.openshift-image-registry.svc:5000/payu-dev/`.            | 🔄 In Progress |
| INFRA-003 |    P0    | 🔵    | Deploy all 23 services to `payu-dev` and verify pods Running                                     | Kustomize manifests ready (`infrastructure/workloads/base/` + `overlays/payu-dev/`). Pending image builds.                                                       | 📋 To Do |
| INFRA-004 |    P0    | 🔵    | Create ArgoCD ApplicationSet for all 23 services across environments                             | Manifests exist in `infrastructure/workloads/`. Need ApplicationSet CR to auto-generate Applications per service/environment.                                    | 📋 To Do |
| INFRA-008 |    P0    | 🔵    | Integrate OWASP ZAP headless + Schemathesis into Tekton task for every `payu-dev` deploy        | Tekton tasks for ZAP and Schemathesis exist but not wired into deploy pipeline. Need quality gate: no high/critical findings to promote to `payu-sit`.        | 📋 To Do |
| INFRA-009 |    P0    | 🔵    | Implement OSSM (Istio) with `PeerAuthentication: STRICT` in `payu-uat` and above                 | Service Mesh operator installed. Need `PeerAuthentication` STRICT + `AuthorizationPolicy` deny-by-default in `payu-uat`, `payu-preprod`, `payu`.               | 📋 To Do |
| INFRA-012 |    P0    | 🔵    | Complete ArgoCD Image Updater setup — add SSH public key to GitHub deploy keys for write-back   | Ed25519 key generated. Public key must be added to GitHub repo deploy keys to enable digest-based promotion via Git write-back.                               | 📋 To Do |
| INFRA-016 |    P0    | 🔵    | Configure rate limiting (global 1000 req/s per IP) via API Gateway                               | §14.3 requirement. No rate limiting configured yet at ingress or gateway level.                                                                                  | 📋 To Do |
| INFRA-017 |    P0    | 🔵    | Enforce API security headers (HSTS, CSP, X-Frame-Options) in all responses                       | §14.4 requirement. Headers not yet enforced globally. Need Gateway/WAF layer or Istio EnvoyFilter.                                                             | 📋 To Do |
| INFRA-021 |    P0    | 🔵    | Configure ArgoCD auto-rollback on health check failure (5 min window)                            | §18.2 requirement. ArgoCD rollback is manual today. Need automated health check + rollback config.                                                            | 📋 To Do |
| INFRA-010 |    P1    | 🟡    | Configure ComplianceOperator for CIS Kubernetes Benchmark scan + forward to Wazuh                | ComplianceOperator installed but not configured for scheduled CIS scan. Wazuh not deployed yet (dependency INFRA-011).                                         | 📋 To Do |
| INFRA-011 |    P1    | 🟡    | Deploy Wazuh manager + agent for SIEM/compliance dashboard (PCI-DSS v4.0 ready)                  | §4.6.1 requirement. Wazuh is key for PCI-DSS Req 10 (logging) and compliance reporting. Not yet deployed.                                                      | 📋 To Do |
| INFRA-013 |    P1    | 🟡    | Enable Tekton Chains for SLSA provenance attestation auto-generation                             | §4.4.1 requirement. Critical for SLSA Level 3 target. Chains not yet enabled in `openshift-pipelines`.                                                         | 📋 To Do |
| INFRA-014 |    P1    | 🟡    | Configure Tekton Results for audit trail (12-month retention)                                    | §4.4.1 requirement. PCI-DSS Req 10 needs pipeline audit trail. Results not configured.                                                                         | 📋 To Do |
| INFRA-015 |    P1    | 🟡    | Deploy Coraza WAF with OWASP CRS v4.x at ingress layer                                           | §14.2 requirement. No WAF deployed yet. Coraza or ModSecurity needed for OWASP CRS enforcement.                                                                 | 📋 To Do |
| INFRA-020 |    P1    | 🔵    | Define severity P1-P4 + escalation path and socialize to all teams                               | §18.1 definitions exist in document but not formally adopted. Need incident response playbook distribution.                                                    | 📋 To Do |
| INFRA-022 |    P1    | 🟠    | Setup PagerDuty/Opsgenie integration for P1/P2 alerting                                          | §18.3 requirement. No on-call rotation or paging integration exists yet.                                                                                       | 📋 To Do |
| INFRA-018 |    P2    | 🟡    | Setup registry GC policy (7 days non-prod, 30 days prod)                                         | §12.3 requirement. OpenShift internal registry has default GC but not tuned per environment.                                                                   | 📋 To Do |
| INFRA-019 |    P2    | 🟡    | Configure Quay.io auto-prune policy                                                              | §12.3 requirement. If Quay.io used as primary registry, needs auto-prune by tag age/count.                                                                     | 📋 To Do |

### Phase 3 — Optimization

| Key       | Priority | Badge | Summary                                                                                          | Notes / Current State                                                                                                                                            | Status   |
| :-------- | :------: | :---- | :----------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| INFRA-023 |    P0    | 🔵    | Implement full OWASP Web + API Top 10 test suite in pipeline DAST                                | §5 requirement. ZAP + Schemathesis need to cover all OWASP Web Top 10 2025 + API Security Top 10 2023. Currently only basic scans.                              | 📋 To Do |
| INFRA-026 |    P0    | 🔵    | Integrate contract test as pipeline gate (break contract = PR rejected)                          | §19.2 requirement. Pact Broker deployed but not wired as gate. Need provider/consumer verification in Tekton pipeline.                                          | 📋 To Do |
| INFRA-024 |    P1    | 🟡    | Automated compliance reporting to CISO (weekly via Wazuh + ComplianceOperator)                   | §4.6.3 requirement. Depends on INFRA-010 and INFRA-011.                                                                                                         | 📋 To Do |
| INFRA-025 |    P1    | 🟡    | Setup preview environment (`payu-dev-*`) via ArgoCD ApplicationSet + auto-cleanup                | §3.1 requirement. TTL-based namespace cleanup CronJob needed. ApplicationSet cluster generator for PR branches.                                                 | 📋 To Do |
| INFRA-027 |    P1    | 🟡    | Implement signed audit logs (vector + Rekor) for PCI-DSS Req 10                                  | §15 requirement. Tamper-evident log chain needed. Wazuh FIM alone insufficient.                                                                                  | 📋 To Do |
| INFRA-028 |    P1    | 🟡    | Generate PCI-DSS v4.0 evidence report from mapping matrix §15                                    | §15 requirement. Validate all Req 1-12 covered with evidence artifacts.                                                                                         | 📋 To Do |
| INFRA-034 |    P1    | 🔵    | Validate ArgoCD recovery from Git (full re-sync test)                                            | §9.3 requirement. Git is source of truth but never tested end-to-end. Need DR validation.                                                                       | 📋 To Do |
| INFRA-029 |    P2    | 🟠    | Schedule quarterly pen test in `payu-preprod`                                                    | §15 / Phase 3 requirement. Manual or automated penetration testing schedule.                                                                                    | 📋 To Do |
| INFRA-030 |    P1    | 🟠    | Validate all data storage in-country (PostgreSQL, Vault, Wazuh, LokiStack)                       | §16 requirement. Bank Indonesia / UU PDP data residency. Need validation + documentation.                                                                      | 📋 To Do |
| INFRA-031 |    P1    | 🟠    | Implement LUKS encryption for PersistentVolumes in production                                    | §16.2 requirement. Data-at-rest encryption for all PVs in `payu` namespace.                                                                                    | 📋 To Do |
| INFRA-032 |    P1    | 🟠    | Configure Wazuh rule to detect data egress to non-Indonesia IP range                             | §16.3 requirement. Proactive monitoring for cross-border data flow violations.                                                                                 | 📋 To Do |
| INFRA-033 |    P2    | 🟡    | Setup monthly cost report dashboard in Grafana                                                   | §10.2 requirement. OpenCost deployed but no Grafana dashboard yet.                                                                                              | 📋 To Do |
| INFRA-035 |    P2    | 🟠    | Document DNS failover procedure for standby cluster                                              | §9.4 requirement. Cross-cluster DR target. Single cluster today but procedure needed for future scaling.                                                        | 📋 To Do |

### Phase 4 — Continuous Improvement

| Key       | Priority | Badge | Summary                                                                                          | Notes / Current State                                                                                                                                            | Status   |
| :-------- | :------: | :---- | :----------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| INFRA-038 |    P1    | 🟠    | Target SLSA Level 3 — hermetic builds, provenance attestation, build isolation                   | §4.2.1 / §4.4.1 requirement. Depends on Tekton Chains (INFRA-013) + hermetic build NetworkPolicy.                                                              | 📋 To Do |
| INFRA-048 |    P1    | 🟡    | Quarterly DR drill (Vault, ArgoCD, Wazuh) — automated test script                                | §9.2 requirement. Vault DR script exists (INFRA-005/006 needed first). Expand to quarterly automated drills.                                                   | 📋 To Do |
| INFRA-036 |    P2    | 🔵    | Evaluate and tune tools based on metrics, incident reports, and false positive rate              | §4 / §21 requirement. Need baseline metrics first (pipeline duration, scan accuracy, developer feedback).                                                       | 📋 To Do |
| INFRA-040 |    P2    | 🔵    | Review and update OWASP compliance matrix every 6 months                                         | §5 requirement. First review due 6 months after v1.3.0 baseline.                                                                                                | 📋 To Do |
| INFRA-041 |    P2    | 🔵    | Developer feedback loop — DevEx survey, pipeline speed optimization, friction reduction          | §21 requirement. Target: pipeline feedback loop < 15 min, local setup < 30 min.                                                                                | 📋 To Do |
| INFRA-037 |    P2    | 🟠    | Implement scheduled pen testing in `payu-preprod` (quarterly) with report to CAB                 | §4.6.3 / §20 requirement. Depends on INFRA-029.                                                                                                                | 📋 To Do |
| INFRA-039 |    P2    | 🟠    | Annual red team exercise for end-to-end security posture validation                              | §4.6.3 requirement. Enterprise maturity target.                                                                                                                  | 📋 To Do |
| INFRA-042 |    P2    | 🟠    | Pilot migration 1-2 services from Jenkins/GitLab CI to Tekton in `payu-dev`                      | §17 requirement. Brownfield adoption. No Jenkins/GitLab currently used; task reserved for future external integrations.                                         | 📋 To Do |
| INFRA-043 |    P2    | 🟠    | Bulk import legacy K8s secrets to Vault (dry-run → execute)                                      | §17.2 requirement. No legacy secrets today; reserved for brownfield migration.                                                                                 | 📋 To Do |
| INFRA-044 |    P2    | 🟠    | Cutover per-namespace per strangler fig strategy §17.3                                           | §17.3 requirement. Reserved for future CI migration.                                                                                                            | 📋 To Do |
| INFRA-045 |    P2    | 🟠    | Evaluate hub-spoke model needs based on scale                                                    | §11 requirement. Single cluster sufficient for lab. Evaluate when scaling beyond 50 services or multi-region.                                                  | 📋 To Do |
| INFRA-046 |    P2    | 🟠    | Setup ArgoCD ApplicationSet cluster generator (if multi-cluster adopted)                         | §11.2 requirement. Depends on INFRA-045.                                                                                                                       | 📋 To Do |
| INFRA-047 |    P2    | 🟠    | Implement image mirroring across clusters via Skopeo + Cosign verify                             | §11.4 requirement. Depends on multi-cluster adoption.                                                                                                          | 📋 To Do |
| INFRA-049 |    P2    | 🟠    | Validate cross-cluster failover < 5 minutes via DNS health check                                 | §9.4 / §11 requirement. Enterprise DR target.                                                                                                                  | 📋 To Do |
| INFRA-050 |    P2    | 🟠    | Annual full-scale DR exercise with post-mortem report                                            | §9.4 requirement. Enterprise maturity target.                                                                                                                  | 📋 To Do |
| INFRA-051 |    P2    | 🟠    | Setup `oc-mirror` for operator catalog mirroring (if required)                                   | §12.2 requirement. Air-gapped readiness for financial services.                                                                                                 | 📋 To Do |
| INFRA-052 |    P2    | 🟠    | Document air-gapped deployment procedure                                                         | §12.2 requirement. Depends on INFRA-051.                                                                                                                       | 📋 To Do |

---

## 📊 Metrics

### Current State

| Metric                 | Value                                                                     |
| :--------------------- | :------------------------------------------------------------------------ |
| Completed Epics        | 24/24 fully done (see PROGRESS.md)                                        |
| Completed Stories      | 109 done (86 + 23 test stories archived)                                  |
| Completed SP           | 265/265                                                                   |
| Bugs Fixed             | 711 done + 4 Won't Do (archived to CHANGELOG)                             |
| Open Bugs              | 0 — All resolved |
| Tech Debt              | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)                              |
| Operational Follow-Ups | 13 carry-over tasks (3 done, 1 blocked, 8 suspended, 1 todo: real API test) |
| DevSecOps Tasks        | 52 tasks from `DEVSECOPS_ARCHITECTURE.md` v1.3.1 (Phase 1–4)               |

---

_Last Updated: May 6, 2026 | 0 Active Epics · 0 Open Stories · 0 Open Bugs · 5 Account ID Items (4 fixed, 1 todo) · 13 Ops (3 done, 1 blocked, 8 suspended, 1 todo) · 52 DevSecOps · 6 Spikes · 9 Deferred_
_All 702 bugs fixed + 4 Won't Do archived to CHANGELOG.md_
_k6 Operator installed April 9: namespace payu-k6, ClusterAutoscaler (max 14 nodes), MachineAutoscalers (2a: 2-5, 2b: 1-4, 2c: 0-4). Use TestRun CRDs in infrastructure/openshift/infra/base/k6/ for distributed runs._
_CRUD Testing Sessions (April 9): 24/28 endpoints validated ✅. 4 blocked by NetworkPolicy (OPS-09-02) + gateway route mismatches (OPS-09-03). Major fixes: wallet optimistic locking, JWT authority mapping (3 services), SavingsGoal ownership, gateway schema mismatches, AccountSecurityService bean, UserAccountController, BeneficiaryController ownership, tenant_id migration, AccountType enum._
_Operational carry-over: wallet cache rollout completed, final post-rollout probe + full k6 stress/consistency reruns still pending — April 8, 2026_
_Phase 15 Final Remediation: ✅ COMPLETE (All 12 remaining bugs closed) — April 7, 2026_
_Phase 14 Frontend Remediation: ✅ COMPLETE (All 42 frontend bugs closed) — April 7, 2026_
_Phase 13 Security & Idempotency: ✅ COMPLETE (All 10 critical sec findings closed) — April 7, 2026_
_Phase 12 E2E Coverage Gap Fixes: All 27 findings (BUG-TEST-090–116) closed — 10 new Playwright specs, 2 backend routing fixes, 12 xfail markers removed. Pytest 159/159, Maven 38/38 — March 17, 2026_
_Phase 10 Shared Lib Audit: 31 new findings (BUG-SHARED-001–031) from 12 backend/shared/ modules (~170 source files) — March 17, 2026_
_Phase 9 Infra Audit Phase 2: 44 new findings (BUG-INFRA-044–087) from 50+ files across 7 infrastructure directories — March 17, 2026_
_Phase 8 Test Quality Audit: 39 new findings (BUG-TEST-051–089) from 249 test files across 20 services — March 17, 2026_
_Phase 7 Bug Sweep: ✅ COMPLETE (240/240 closed) — March 17, 2026. Verified: Maven 38/38, Frontend OK (44 routes, 79 pages), Playwright 544/544, Pytest 159/159._
_Phase 3 Bug Fixes: ✅ COMPLETE (34/34 closed) — March 16, 2026_
_Phase 2 Gateway Gaps: ✅ COMPLETE (GAP-001, GAP-002, GAP-006, GAP-007) — March 16, 2026_
_⚠️ OpenShift Cluster Destroyed (May 2, 2026): All OpenShift-dependent tasks (INFRA-001~052, OPS-2026-04-08/09 series) are suspended. Local development environment (`infrastructure/local/podman/`) is now the primary target for fixes and validation. All OpenShift-specific infrastructure configs remain in `infrastructure/` for future redeployment._
_Phase 1 E2E Stabilization: ✅ COMPLETE (544 Playwright + 159 Pytest = 703 tests, 0 failures) — March 15, 2026_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
