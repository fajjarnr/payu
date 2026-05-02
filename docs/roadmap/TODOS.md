# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| **Open Bugs** | 9 | 🟠 Frontend/Auth: 6 bugs + Infrastructure/Redis: 3 new bugs (May 2, 2026) |

> **Completed Epics**: 24/24 fully done. All stories & tech debt cleared.
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.
> **Closed bugs, stories & history**: See [`CHANGELOG.md`](../../CHANGELOG.md).

### 🐛 Open Bug Scorecard

| Kategori                   | Open  | Priority Range |
| :------------------------- | :---: | :------------- |
| Backend Logic              |   0   | —              |
| Frontend Logic             |   4   | P1–P2          |
| Frontend-Backend Mismatch  |   1   | P1             |
| Auth / Session             |   1   | P1             |
| Shared Libraries           |   0   | —              |
| Test Coverage / Quality    |   0   | —              |
| Infrastructure / OpenShift |   3   | P0–P1          |
| Architecture               |   0   | —              |
| Security (PII/IDOR)        |   0   | —              |
| **TOTAL**                  | **9** |                |

> 🚨 Audit update (April 15, 2026): 6 new frontend/auth regressions and product-flow bugs were identified below. Historical “all bugs resolved” notes remain valid for the April 7 milestone only, not for the current state.
> All 702 bugs fixed + 4 Won't Do archived to [`CHANGELOG.md`](../../CHANGELOG.md).
> **Phase 15 Final Remediation**: All 12 remaining findings (BUG-SECURITY-027, 008, 009, 022-025, BUG-LOGIC-013, 016, BUG-ARCH-002, BUG-FE-007-011) resolved — security hardening, access control, promo validation, exception architecture.
> **Phase 14 Frontend Remediation**: All 42 findings (BUG-FE-001–BUG-FE-040 + BUG-CROSS-033–039) resolved — i18n, design system, and backoffice connectivity.
> **Phase 12 E2E Coverage Gaps Closed**: All 27 findings (BUG-TEST-090–116) resolved — 10 new Playwright specs, 2 backend fixes, 12 xfail markers removed.
> **Phase 11 E2E Coverage Gap Analysis**: 27 findings identified (BUG-TEST-090–116).
> **Phase 10 Shared Library Audit**: 31 findings — all fixed.
> **Phase 9 Infrastructure Audit Phase 2**: 44 findings — all fixed.
> **Phase 8 Test Quality Audit**: 39 findings — all fixed.
> ℹ️ Open bug count is now `6`. The operational carry-over items below remain validation/resume tasks from the April 8, 2026 k6 cluster run and are not counted as bug backlog items.

---

## 🐞 Open Bugs

| Key           | Priority | Category                  | Summary                                                                                                                                                                 | Status   |
| :------------ | :------: | :------------------------ | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :------- |
| BUG-CROSS-074 |    P1    | Frontend-Backend Mismatch | Login page still stores `user.id` as `accountId`, bypassing the BFF/account-claim fix and breaking account-scoped queries for users where `sub != account_id`.          | 📋 To Do |
| BUG-AUTH-035  |    P1    | Auth / Session            | Cookie-restored sessions only rehydrate `isAuthenticated`/expiry, not `user` and `accountId`, leaving protected pages authenticated-but-empty after local storage loss. | 📋 To Do |
| BUG-FE-107    |    P1    | Frontend Logic            | Onboarding step 1 lets users continue without uploading KTP or calling any KYC verification endpoint.                                                                   | 📋 To Do |
| BUG-FE-108    |    P2    | Frontend Logic            | “Lupa password?” is a dead `#` link, so there is no recoverable password-reset path from the login screen.                                                              | 📋 To Do |
| BUG-FE-109    |    P2    | Frontend Logic            | Mobile onboarding step 1 has no visible in-app back/exit control because the only back link lives inside a desktop-only aside.                                          | 📋 To Do |
| BUG-FE-110    |    P2    | Frontend Logic            | Onboarding password visibility toggles are removed from keyboard tab order, making them inaccessible for keyboard-only users.                                           | 📋 To Do |
| BUG-INFRA-088 |    P0    | Infrastructure / OpenShift | Redis connectivity failure — all Spring Boot services connect to `payu-datagrid:11222` without password / incompatible auth, causing health check 503 and circuit breaker OPEN. | 📋 To Do |
| BUG-INFRA-089 |    P1    | Infrastructure / OpenShift | `auth-service` Redis still connects to `localhost:6379` despite env vars patched to `payu-cache:6379` — property override or stale JAR build suspected.                  | 📋 To Do |
| BUG-INFRA-090 |    P1    | Infrastructure / OpenShift | `DB_PASSWORD` and `ENCRYPTION_KEY` env vars are empty in many service deployments (inherited from base YAMLs with no secretRef).                                        | 📋 To Do |

### BUG-INFRA-088 — Redis connectivity failure across all Spring Boot services

- **Symptom**: `account-service`, `wallet-service`, `auth-service`, and others return HTTP 503 on `/actuator/health` because Redis health check fails. Gateway returns `CIRCUIT_OPEN` when proxying to these services.
- **Root cause**: All service deployments use `REDIS_HOST=payu-datagrid.payu-dev.svc` and `REDIS_PORT=11222`. DataGrid Infinispan on port 11222 requires RESP3 + AUTH handshake (`developer` / `payu-cache-dev`) that Spring Boot Lettuce cannot complete. The `redis-credentials` Secret had `REDIS_PASSWORD=""` and pointed to the wrong URL.
- **Evidence**: `account-service` logs show `NOAUTH HELLO must be called...`; `auth-service` logs show `Connection refused: localhost/127.0.0.1:6379`; `infrastructure/workloads/base/*.yaml` and `overlays/dev/kustomization.yaml` all reference `payu-datagrid:11222`.
- **Fix in progress**: Patched base + overlay YAMLs to use `payu-cache.payu-dev.svc:6379` and `REDIS_PASSWORD=payu-cache-dev-password`. Patched live `redis-config` ConfigMap and `redis-credentials` Secret. All 22+ service deployments need rebuild + redeploy for the change to take effect inside containers.
- **Affected services**: `account-service`, `wallet-service`, `auth-service`, `transaction-service`, `lending-service`, `investment-service`, `statement-service`, `backoffice-service`, `partner-service`, `promotion-service`, `support-service`, `compliance-service`, `cms-service`, `product-catalog-service`, `dispute-service`, `integration-service`, `fx-service`, `billing-service`, `notification-service`, `gateway-service`.

### BUG-INFRA-089 — Auth-service Redis connects to localhost despite env var patch

- **Symptom**: `auth-service` readiness probe returns 503. Logs show `Connection refused: localhost/127.0.0.1:6379` even though `oc set env` confirms `REDIS_HOST=payu-cache.payu-dev.svc` and `REDIS_PORT=6379`.
- **Suspected cause**: Either (a) `application-container.yml` inside the running JAR still has `host: ${REDIS_HOST:localhost}` and the env var is not being picked up by the Spring Boot process, or (b) a hardcoded property elsewhere overrides the env var. The running image may also be stale (built before container env var support was added).
- **Evidence**: `oc exec` into auth-service pod shows correct env vars, but logs consistently reference `localhost:6379`. `application-container.yml` in source shows correct `${REDIS_HOST:redis}` default, but running behavior differs.
- **Next step**: Verify the effective `application-container.yml` bundled inside the running JAR, or rebuild `auth-service` image from latest source with confirmed env var support.

### BUG-INFRA-090 — Empty DB_PASSWORD and ENCRYPTION_KEY in service deployments

- **Symptom**: Multiple services may fail to connect to PostgreSQL or decrypt sensitive fields because `DB_PASSWORD` and `ENCRYPTION_KEY` env vars evaluate to empty strings.
- **Root cause**: Base deployment YAMLs (`infrastructure/workloads/base/*.yaml`) define `DB_PASSWORD` and `ENCRYPTION_KEY` with `valueFrom.secretKeyRef`, but the overlay kustomization patches sometimes override these with plain `value: ""` or omit them entirely.
- **Evidence**: `oc set env -n payu-dev deployment/account-service --list` shows `DB_PASSWORD=` and `ENCRYPTION_KEY=` with empty values. The same pattern exists in `wallet-service` and others.
- **Fix needed**: Audit all base service YAMLs and overlay patches to ensure `DB_PASSWORD` and `ENCRYPTION_KEY` always reference the correct secrets (`db-credentials`, `encryption-keys`).

### BUG-CROSS-074 — Login page stores `user.id` as `accountId` (regression of prior account-claim fix)

- **Symptom**: Setelah login sukses, layar yang bergantung pada `accountId` dapat memanggil API dengan JWT `sub` alih-alih `account_id`, sehingga saldo, kartu, dan data wallet bisa kosong atau salah target.
- **Evidence**: `frontend/web-app/src/app/[locale]/login/page.tsx` masih memanggil `setAuth(user, user.id)`, sementara `frontend/web-app/src/app/api/auth/login/route.ts` sudah membangun `user.accountId` terpisah dari `user.id`, dan `frontend/web-app/src/hooks/useAuth.ts` sudah mengasumsikan `user.accountId || user.id`.
- **Affected flows**: Login via halaman `/login`, kemudian dashboard/wallet/cards/history yang bergantung pada `accountId`.
- **Repro**: Gunakan akun dengan token yang memiliki `account_id` berbeda dari `sub`, login lewat halaman web, lalu buka dashboard dan cek request account-scoped yang memakai identifier salah.

### BUG-AUTH-035 — Cookie-restored session tidak memulihkan context user/account

- **Symptom**: User dengan cookie sesi valid bisa lolos middleware dan dianggap login, tetapi halaman terproteksi tetap kosong karena store klien kehilangan `user` dan `accountId`.
- **Evidence**: `frontend/web-app/src/components/SessionBootstrap.tsx` hanya memanggil `setAuthenticated(true)` dan `setTokenExpiry(...)`; `frontend/web-app/src/app/api/auth/refresh/route.ts` juga hanya mengembalikan `expiresIn`; query saldo di `frontend/web-app/src/app/[locale]/dashboard/page.tsx` dan `frontend/web-app/src/hooks/useWallet.ts` tetap menunggu `accountId`.
- **Affected flows**: Reload hard refresh, restore tab, atau revisit app ketika cookie masih valid tetapi `payu-auth-storage` kosong/stale.
- **Repro**: Pertahankan cookie auth valid, hapus local storage auth store, buka `/dashboard`, lalu amati state menjadi authenticated tanpa data account-scoped.

### BUG-FE-107 — KYC onboarding step 1 dapat dilewati tanpa upload dokumen

- **Symptom**: User bisa lanjut ke langkah profil tanpa memilih file KTP, tanpa ada upload state, dan tanpa request ke endpoint verifikasi KYC.
- **Evidence**: `frontend/web-app/src/app/[locale]/onboarding/page.tsx` hanya menampilkan `div` fokusabel untuk area upload dan tombol `setStep(2)` tanpa validasi; halaman ini tidak memakai `frontend/web-app/src/services/KYCService.ts`; test `frontend/web-app/e2e/kyc-flow.spec.ts` juga langsung mengklik “Lanjut ke Profil Data” tanpa upload.
- **Affected flows**: Onboarding / registrasi web.
- **Repro**: Buka `/onboarding`, klik tombol lanjut pada step 1 tanpa interaksi upload, lalu verifikasi bahwa form step 2 terbuka tanpa request `/kyc/verify/*`.
- **Notes**: Ini juga berarti coverage E2E saat ini menormalisasi bypass, bukan menangkap regression.

### BUG-FE-108 — Link reset password mati di halaman login

- **Symptom**: CTA “Lupa password?” tidak memulai flow recovery apa pun.
- **Evidence**: `frontend/web-app/src/app/[locale]/login/page.tsx` menggunakan `Link href="#"`; `frontend/web-app/e2e/login-flow.spec.ts` saat ini justru menganggap `href="#"` sebagai expected behavior.
- **Affected flows**: Password recovery dari halaman login.
- **Repro**: Buka `/login`, klik “Lupa password?”, dan lihat bahwa browser hanya berpindah ke hash kosong pada halaman yang sama.

### BUG-FE-109 — Mobile onboarding step 1 tidak punya jalur keluar in-app

- **Symptom**: Pada viewport mobile, user tidak mendapat tombol kembali/batal di dalam UI onboarding step 1.
- **Evidence**: Satu-satunya link “Kembali” berada di `aside` dengan class `hidden lg:flex` pada `frontend/web-app/src/app/[locale]/onboarding/page.tsx`, sehingga tidak tampil di mobile.
- **Affected flows**: Onboarding di perangkat mobile dan viewport kecil.
- **Repro**: Emulasikan lebar 375px, buka `/onboarding`, dan pastikan tidak ada kontrol kembali selain browser/system back.

### BUG-FE-110 — Toggle show/hide password tidak bisa diakses via keyboard

- **Symptom**: Keyboard-only user tidak bisa memfokuskan tombol show/hide password dan confirm password di form onboarding.
- **Evidence**: Kedua tombol visibility pada `frontend/web-app/src/app/[locale]/onboarding/page.tsx` disetel dengan `tabIndex={-1}`.
- **Affected flows**: Step 2 form onboarding / registrasi.
- **Repro**: Masuk ke step 2 onboarding lalu tekan `Tab` berulang; fokus akan melewati kedua tombol visibility.

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
| OPS-2026-04-08-01 | Task | Validate that the new `wallet-service` rollout no longer emits `DistributedCacheService` wallet cache deserialization warnings.                                                                                                              | `cache-starter` compatibility fix added and `wallet-service` image rolled out; post-rollout in-cluster probe was interrupted before verification.                                                                                                  | 📋 To Do |
| OPS-2026-04-08-02 | Task | Re-run the full 40-minute `tests/performance/k6/crud-stress-test.js` job via k6 Operator using `payu-crud-load` TestRun.                                                                                                                     | k6 Operator installed (April 9). Use: `kubectl apply -f infrastructure/openshift/infra/base/k6/crud-load-testrun.yaml -n payu-k6`. ClusterAutoscaler + MachineAutoscalers now configured to scale up to 5 workers in us-east-2a + 4 each in 2b/2c. | 📋 To Do |
| OPS-2026-04-08-03 | Task | If full stress still breaches `http_req_duration p(99) < 10s`, isolate the slow endpoint from gateway, wallet, and account logs during the same run window.                                                                                  | k6 Operator runner logs available via `kubectl logs -n payu-k6 -l runner=payu-crud-stress`. Auth/cache instability improved after `payu-datagrid` 512Mi→1Gi fix.                                                                                   | 📋 To Do |
| OPS-2026-04-08-04 | Task | Re-run `tests/performance/k6/crud-data-consistency-test.js` after stress revalidation.                                                                                                                                                       | Use: `kubectl apply -f infrastructure/openshift/infra/base/k6/crud-consistency-testrun.yaml -n payu-k6`. Consistency canary already passed with test-mode/bypass flow.                                                                             | 📋 To Do |
| OPS-2026-04-08-05 | Task | Decide whether to disable `GATEWAY_RATE_LIMIT_TEST_MODE` in `payu-dev` after final validation, then record the final cluster/test outcome in roadmap docs.                                                                                   | Test mode still enabled for controlled k6 validation. After final k6 Operator run, update this item and CHANGELOG.                                                                                                                                 | 📋 To Do |
| OPS-2026-04-09-01 | Task | k6 Operator smoke test validated — runner pod executed 30 iterations/1 VU. HTTP failures expected (public DNS not reachable from pod network). Re-run with in-cluster service URLs or after confirming Istio ingress gateway routes.         | k6 Operator lifecycle verified: initializer → starter → runner → finished. ClusterAutoscaler live. Nodes: 7 (3 master, 2 infra, 2 worker).                                                                                                         | 📋 To Do |
| OPS-2026-04-09-02 | Task | **[BLOCKER]** Add `app.kubernetes.io/part-of: payu` label to `transaction-service` deployment. NetworkPolicy `default-deny-egress` blocks egress for pods missing this label → transaction-service CANNOT reach account-service.             | Fix: `oc patch deployment transaction-service -n payu-dev --type='json' -p='[{"op":"add","path":"/spec/template/metadata/labels/app.kubernetes.io~1part-of","value":"payu"}]'`. Blocks: Transfer, Account Transactions, Disbursement auth.         | 📋 To Do |
| OPS-2026-04-09-03 | Task | Add gateway routes for Disbursement and Virtual Account. Gateway proxies `/transactions/disbursements` → `/api/v1/transactions/disbursements` but DisbursementController is at `/api/v1/disbursements`. Same for VA (`/api/v1/payments/va`). | Fix: Add `/disbursements/*` and `/payments/va/*` routes in `ApiGatewayResource.java` (before generic `/payments/*` route). Rebuild + deploy gateway-service.                                                                                       | 📋 To Do |
| OPS-2026-04-09-04 | Task | Re-test Transfer endpoint with `type: INTERNAL_TRANSFER` field after OPS-2026-04-09-02 is fixed. Transfer payload requires `type` field (not `@NotNull` but throws NPE if missing).                                                          | Payload: `{"senderAccountId":"<KC_SUB>","recipientAccountNumber":"2001001002","amount":1000,"currency":"IDR","description":"Test","type":"INTERNAL_TRANSFER"}`.                                                                                    | 📋 To Do |
| OPS-2026-04-09-05 | Task | Run full comprehensive CRUD validation across all 3 services after NetworkPolicy + gateway route fixes. 24/28 endpoints passing; 4 blocked (Transfer, Account Transactions, Disbursement, VA).                                               | See CRUD Validation Results table in session notes. Wallet (14/14 ✅), Account (4/4 ✅), Transaction (0/4 ❌ blocked).                                                                                                                             | 📋 To Do |
| OPS-2026-04-09-06 | Task | Transaction-service Redis/DataGrid connection issue — `ScheduledTransferScheduler` cannot connect to DataGrid RESP on port 11222. Affects Split Bill list (HTTP 500) and scheduled transfers.                                                | Lower priority — does not block core CRUD. May need DataGrid RESP config or NetworkPolicy fix for port 11222.                                                                                                                                      | 📋 To Do |
| OPS-2026-04-09-07 | Task | Admin-only endpoints (GL, Settlement, Journal, ChartOfAccounts, Escrow, SplitPayment) require `ROLE_ADMIN` or `ROLE_BACKOFFICE`. Need to create admin Keycloak user or add realm roles for testing.                                          | Smart Routing also returns 404 — gateway doesn't route `/transfers/routes`.                                                                                                                                                                        | 📋 To Do |
| OPS-2026-05-02-01 | Task | Rebuild and redeploy all 20+ Spring Boot/Quarkus services with updated Redis configuration (`payu-cache:6379` + password). Verify `/actuator/health` returns 200 for each.                                                              | Base YAMLs + overlay kustomization patched. Images still contain old `application-container.yml` with `payu-datagrid:11222`. Need `mvn clean package -DskipTests` + Tekton build for each service.                                                  | 📋 To Do |
| OPS-2026-05-02-02 | Task | Run realistic k6 E2E CRUD test again after all services are healthy. Target: >95% checks pass, `http_req_failed` < 5%, all endpoints return 200.                                                                                         | k6 script fixed (setup-phase login, ConfigMap + Secret config). Currently blocked by service health failures (Redis → circuit breaker OPEN → 503).                                                                                                   | 📋 To Do |
| OPS-2026-05-02-03 | Task | Fix `auth-service` Redis property resolution — either rebuild JAR with confirmed env var support or add explicit `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` env var names.                                                      | Auth-service logs still show `localhost:6379` despite `REDIS_HOST`/`REDIS_PORT` env vars set correctly. May require code change or property name alignment.                                                                                         | 📋 To Do |
| OPS-2026-05-02-04 | Task | Audit and fix empty `DB_PASSWORD`/`ENCRYPTION_KEY` across all service deployments. Ensure every service references `db-credentials` and `encryption-keys` secrets correctly.                                                              | Account-service and wallet-service currently show empty values. May cause DB connection failures or encryption pass-through mode (security risk).                                                                                                     | 📋 To Do |
| OPS-2026-05-02-05 | Task | Document Tekton pipeline fix patterns in `docs/guides/LESSONS.md`: `onError: continue` Tekton v1.9 limitation, registry auth `unused:<token>` format, license compliance purl filtering.                                                  | All patterns discovered and fixed during this session. Need to capture for future sessions and team reference.                                                                                                                                      | 📋 To Do |

---

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
| Bugs Fixed             | 702 done + 4 Won't Do (archived to CHANGELOG)                             |
| Open Bugs              | 9 — 6 Frontend/Auth (April 15) + 3 Infrastructure/Redis (May 2, 2026)      |
| Tech Debt              | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)                              |
| Operational Follow-Ups | 17 carry-over tasks (May 2, 2026 — Tekton fixes, Redis rebuilds, k6 rerun) |
| DevSecOps Tasks        | 52 tasks from `DEVSECOPS_ARCHITECTURE.md` v1.3.1 (Phase 1–4)               |

---

_Last Updated: May 2, 2026 | 0 Active Epics · 0 Open Stories · 9 Open Bugs · 0 Tech Debt · 17 Operational Follow-Ups · 52 DevSecOps Tasks · 6 Spikes · 9 Deferred_
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
