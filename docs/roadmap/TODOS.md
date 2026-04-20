# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| **Open Bugs** | 6 | 🟠 Frontend/Auth audit reopened 6 actionable bugs (April 15, 2026) |

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
| Infrastructure / OpenShift |   0   | —              |
| Architecture               |   0   | —              |
| Security (PII/IDOR)        |   0   | —              |
| **TOTAL**                  | **6** |                |

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

---

## 📊 Metrics

### Current State

| Metric                 | Value                                                                     |
| :--------------------- | :------------------------------------------------------------------------ |
| Completed Epics        | 24/24 fully done (see PROGRESS.md)                                        |
| Completed Stories      | 109 done (86 + 23 test stories archived)                                  |
| Completed SP           | 265/265                                                                   |
| Bugs Fixed             | 702 done + 4 Won't Do (archived to CHANGELOG)                             |
| Open Bugs              | 6 — Frontend/Auth audit findings reopened on April 15, 2026               |
| Tech Debt              | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)                              |
| Operational Follow-Ups | 12 carry-over validation tasks (April 9, 2026 — k6 Operator + CRUD fixes) |

---

_Last Updated: April 15, 2026 | 0 Active Epics · 0 Open Stories · 6 Open Bugs · 0 Tech Debt · 12 Operational Follow-Ups · 6 Spikes · 9 Deferred_
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
_Phase 1 E2E Stabilization: ✅ COMPLETE (544 Playwright + 159 Pytest = 703 tests, 0 failures) — March 15, 2026_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
