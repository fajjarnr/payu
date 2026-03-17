# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Status           | Count | Breakdown                                             |
| :--------------- | :---: | :---------------------------------------------------- |
| **Active Epics** |   0   | All completed ✅                                      |
| **Open Stories** |  23   | TEST-WEB-001 – TEST-WEB-023                           |
| **Tech Debt**    |   0   | All completed ✅                                      |
| **Spikes**       |   5   | ARCH-001 – ARCH-005                                   |
| **Deferred**     |   9   | P2-FE-003, OCP-007, OCP-010, DR-001, DEFER-001, RHPAM |
| **Open Bugs**    |   0   | All 240 fixed (Phase 7). 507 fixed + 4 Won't Do archived to CHANGELOG |

> **Completed Epics**: 24/24 fully done. All stories & tech debt cleared.
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.
> **Closed bugs & history**: See [`CHANGELOG.md`](../../CHANGELOG.md).

### 🐛 Open Bug Scorecard

| Kategori                   | Open | Priority Range |
| :------------------------- | :--: | :------------- |
| Backend Logic              |   0  | —              |
| Frontend Logic             |   0  | —              |
| Frontend-Backend Mismatch  |   0  | —              |
| Auth / Session             |   0  | —              |
| Test Coverage / Quality    |   0  | —              |
| Infrastructure / OpenShift |   0  | —              |
| **TOTAL**                  | **0** |               |

> All 507 bugs fixed + 4 Won't Do archived to [`CHANGELOG.md`](../../CHANGELOG.md).
> Deep-audit detail (182 findings, all fixed) preserved as historical reference in [`DEEP_AUDIT_2026-03-16.md`](./DEEP_AUDIT_2026-03-16.md).

### Test Audit Focus (March 17, 2026)

- All stale contract, infrastructure, regression, performance, and security test suites have been updated as part of Phase 7 bug fixes.
- `tests/contract/` updated to match current auth, wallet, and transfer contracts.
- `tests/infrastructure/` updated to validate active local-podman topology (KRaft Kafka).
- `tests/load-tests/` POM and source paths corrected to run current Gatling simulations.
- `tests/performance/`, `tests/regression/`, and `tests/security/` updated to current auth, wallet, and API assumptions.
- Remaining open stories (TEST-WEB-001 – TEST-WEB-023) track deeper backend integration test coverage beyond current UI smoke level.

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                                              | Impact                               | Status   |
| :------- | :---- | :------------------------------------------------------------------------------------ | :----------------------------------- | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                                                | Scope `kyc-service`                  | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?                                 | Output format `statement-service`    | 📋 To Do |
| ARCH-003 | Spike | Support ticket: end-user PayU atau project client?                                    | Multi-tenancy `support-service`      | 📋 To Do |
| ARCH-004 | Spike | CMS: hanya PayU web-app atau multi-tenant project client?                             | Multi-tenant mode `cms-service`      | 📋 To Do |
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | ADR-0015, `rules-starter` shared lib | 📋 To Do |

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

## 🧪 Open Stories

> **Note**: All 23 stories below provide UI smoke coverage only — needs backend integration tests to achieve full E2E confidence.

| Key | Type | Summary | Evidence | Impact | Status |
| :-- | :--- | :------ | :------- | :----- | :----- |
| TEST-WEB-001 | Story | Prove real login, cookie session, BFF refresh, and retry behavior end-to-end | `frontend/web-app/e2e/login-flow.spec.ts`, `frontend/web-app/e2e/fixtures/auth.ts`, `frontend/web-app/src/app/api/auth/login/route.ts`, `frontend/web-app/src/app/api/auth/refresh/route.ts`, `frontend/web-app/src/lib/api.ts` | Current Playwright auth mostly bypasses middleware with mock cookies and stale selectors, so green tests do not prove browser-to-backend auth/session correctness | 📋 To Do |
| TEST-WEB-002 | Story | Cover money-movement journeys with real backend mutations | `frontend/web-app/e2e/transfer-flow.spec.ts`, `frontend/web-app/e2e/transaction-crud.spec.ts`, `frontend/web-app/e2e/bill-pay-flow.spec.ts`, `frontend/web-app/src/app/[locale]/transfer/page.tsx`, `frontend/web-app/src/app/[locale]/transactions/page.tsx`, `frontend/web-app/src/app/[locale]/bills/page.tsx` | Transfer confirm, transaction cancel, and bill payment are only partially exercised; tests stop before proving POST/PUT success and post-mutation refresh | 📋 To Do |
| TEST-WEB-003 | Story | Cover wallet, pockets, and card mutations against the backend | `frontend/web-app/e2e/wallet-crud.spec.ts`, `frontend/web-app/e2e/comprehensive-crud.spec.ts`, `frontend/web-app/src/app/[locale]/pockets/page.tsx`, `frontend/web-app/src/app/[locale]/cards/page.tsx`, `frontend/web-app/src/services/WalletService.ts` | Current suites are mostly page/smoke checks and do not prove create, credit, debit, freeze, unfreeze, close, or delete flows against real API state | 📋 To Do |
| TEST-WEB-004 | Story | Cover e-statement, profile update, and settings/security flows with real backend assertions | `frontend/web-app/e2e/settings-flow.spec.ts`, `frontend/web-app/e2e/user-profile-crud.spec.ts`, `frontend/web-app/src/components/settings/statement-downloader.tsx`, `frontend/web-app/src/app/[locale]/settings/page.tsx`, `frontend/web-app/src/app/[locale]/security/page.tsx` | Profile update, logout, biometric state, and statement generate/list/download are not verified end-to-end even though UI and service code exist | 📋 To Do |
| TEST-WEB-005 | Story | Cover split-bill lifecycle with real shared-state assertions | `frontend/web-app/src/app/[locale]/split-bill/page.tsx`, `frontend/web-app/src/hooks/useSplitBill.ts`, `backend/transaction-service/src/main/java/id/payu/transaction/adapter/web/SplitBillController.java` | No Playwright spec covers create, add participant, activate, accept, decline, pay, or settle flows, so collaboration/payment bugs can ship undetected | 📋 To Do |
| TEST-WEB-006 | Story | Cover scheduled-transfer lifecycle with real backend assertions | `frontend/web-app/src/app/[locale]/scheduled-transfers/page.tsx`, `frontend/web-app/src/services/TransactionService.ts`, `backend/transaction-service/src/main/java/id/payu/transaction/adapter/web/ScheduledTransferController.java` | No Playwright spec covers list, edit, pause, resume, or cancel; existing FE/BE contract drift is currently invisible to E2E | 📋 To Do |
| TEST-WEB-007 | Story | Cover notifications, rewards, and support flows | `frontend/web-app/src/app/[locale]/notifications/page.tsx`, `frontend/web-app/src/app/[locale]/rewards/page.tsx`, `frontend/web-app/src/app/[locale]/support/page.tsx` | Implemented pages have no direct Playwright coverage for fetch, mark-as-read, reward redemption, or support-ticket flows | 📋 To Do |
| TEST-WEB-008 | Story | Cover merchant registration follow-through and merchant dashboard/profile fetch | `frontend/web-app/e2e/merchant-register.spec.ts`, `frontend/web-app/src/app/[locale]/merchant/register/page.tsx`, `frontend/web-app/src/app/[locale]/merchant/page.tsx` | Registration UI has shallow checks only; there is no proof that successful registration leads to usable merchant dashboard state | 📋 To Do |
| TEST-WEB-009 | Story | Cover dashboard data integrations instead of smoke-only rendering | `frontend/web-app/e2e/check_ui.spec.ts`, `frontend/web-app/src/app/[locale]/dashboard/page.tsx` | Current checks tolerate `/api/` failures and mainly assert layout/a11y, so balance, CMS, promo, and personalization regressions can slip through | 📋 To Do |
| TEST-WEB-010 | Story | Cover FX/exchange flow with backend quote/execution assertions | `frontend/web-app/src/app/[locale]/exchange/page.tsx` | Exchange route exists but has no Playwright coverage for quote retrieval or exchange submission | 📋 To Do |
| TEST-WEB-011 | Story | Cover backoffice customer, KYC, fraud, CMS, campaign, and partner flows | `frontend/web-app/src/app/[locale]/backoffice/`, `frontend/web-app/src/app/[locale]/analytics/page.tsx` | Admin/internal routes currently have no Playwright coverage even though they expose business-critical operational workflows | 📋 To Do |
| TEST-WEB-012 | Story | Replace false-positive smoke assertions with stateful web-app <-> backend checks | `frontend/web-app/e2e/check_ui.spec.ts`, `frontend/web-app/e2e/investment-flow.spec.ts`, `frontend/web-app/e2e/lending-flow.spec.ts`, `frontend/web-app/e2e/qris-flow.spec.ts` | Several specs mostly assert fallback/static UI or pages without live mutations, so pass rate overstates actual integration confidence | 📋 To Do |
| TEST-WEB-013 | Story | Audit and fix frontend feature parity against implemented backend capabilities | `frontend/web-app/src/services/InvestmentService.ts`, `frontend/web-app/src/services/NotificationService.ts`, `frontend/web-app/src/services/SupportService.ts`, `frontend/web-app/src/app/[locale]/merchant/`, `backend/investment-service/`, `backend/notification-service/`, `backend/support-service/`, `backend/partner-service/` | Several backend capabilities already exist but the web-app still points to stale contracts, static/demo flows, or non-existent endpoints, so product surface is overstated | 📋 To Do |
| TEST-WEB-014 | Story | Audit and normalize locale-aware routing plus translation coverage across authenticated pages | `frontend/web-app/src/middleware.ts`, `frontend/web-app/src/lib/navigation.ts`, `frontend/web-app/src/components/DashboardLayout.tsx`, `frontend/web-app/src/app/[locale]/`, `frontend/web-app/messages/en.json`, `frontend/web-app/messages/id.json` | Locale prefix handling and translation coverage are inconsistent, so users can be redirected into the wrong language and tests can miss broken multilingual UX | 📋 To Do |
| TEST-WEB-015 | Story | Add real coverage for rewards, gamification, and promotion redemption flows | `frontend/web-app/src/app/[locale]/rewards/page.tsx`, `frontend/web-app/src/hooks/useRewards.ts`, `frontend/web-app/src/hooks/useGamification.ts`, `frontend/web-app/src/services/PromotionService.ts`, `frontend/web-app/e2e/` | Rewards page contains active data hooks and mutations but there is no proof that loyalty, cashback, referral, or daily check-in flows work end-to-end | 📋 To Do |
| TEST-WEB-016 | Story | Add navigation-shell coverage for logout, notification entry, legal links, and locale-safe cross-page transitions | `frontend/web-app/src/components/DashboardLayout.tsx`, `frontend/web-app/src/app/[locale]/page.tsx`, `frontend/web-app/e2e/check_ui.spec.ts`, `frontend/web-app/e2e/navigation.spec.ts` | The global shell exposes critical entry points that are either broken or unverified, so regressions can hide behind page-level smoke tests | 📋 To Do |
| TEST-WEB-017 | Story | Add route-segment session coverage for authenticated pages outside dashboard | `frontend/web-app/src/app/[locale]/dashboard/layout.tsx`, `frontend/web-app/src/app/[locale]/transfer/layout.tsx`, `frontend/web-app/src/app/[locale]/settings/layout.tsx`, `frontend/web-app/src/app/[locale]/exchange/layout.tsx` | Silent refresh and auth survivability are only guaranteed in one route segment today, but no test proves session continuity across the rest of the authenticated app | 📋 To Do |
| TEST-WEB-018 | Story | Audit and repair stale repo-level tests that currently pass without validating current service contracts | `tests/e2e_blackbox/`, `backend/analytics-service/tests/`, `frontend/web-app/e2e/onboarding-flow.spec.ts` | Several green tests still validate mocks, 404s, or broad status-code allowances rather than the actual implemented contracts, so release confidence is overstated | 📋 To Do |
| TEST-WEB-019 | Story | Add black-box and integration coverage for implemented reverse-FX conversion flow | `tests/e2e_blackbox/test_fx_flow.py`, `backend/fx-service/src/main/java/id/payu/fx/adapter/web/FxController.java` | FX service ships reverse-conversion capability but the repo-level tests still do not prove it works in deployed environments | 📋 To Do |
| TEST-WEB-020 | Story | Cover implemented CMS status-update/delete and dispute reject flows in repo-level E2E | `tests/e2e_blackbox/test_cms_flow.py`, `tests/e2e_blackbox/test_dispute_flow.py`, `backend/cms-service/src/main/java/id/payu/cms/adapter/web/rest/ContentController.java`, `backend/dispute-service/src/test/java/id/payu/dispute/integration/DisputeControllerIntegrationTest.java` | Executable black-box coverage stops before important implemented admin actions, so regressions can ship despite green suites | 📋 To Do |
| TEST-WEB-021 | Story | Rationalize OpenShift overlays so staging/prod images, routes, configs, and policies are environment-correct | `infrastructure/openshift/overlays/staging/`, `infrastructure/openshift/overlays/prod/`, `infrastructure/openshift/base/` | Current overlays inherit dev-only image refs, service URLs, route hosts, and mismatched config names, so production-like validation is not trustworthy | 📋 To Do |
| TEST-WEB-022 | Story | Refresh or retire stale test assets across contract, infra, regression, performance, and security suites | `tests/contract/`, `tests/infrastructure/`, `tests/load-tests/`, `tests/performance/`, `tests/regression/`, `tests/security/` | Multiple non-E2E suites still target retired DTOs, paths, compose topology, and auth semantics, so test inventory overstates real verification depth | 📋 To Do |
| TEST-WEB-023 | Story | Add gateway-first coverage for pockets, savings goals, escrow, split payments, settlements, scheduled transfers, split bills, disbursements, and analytics | `backend/gateway-service/src/main/resources/application.yaml`, `backend/gateway-service/src/main/java/id/payu/gateway/adapter/web/ApiGatewayResource.java`, `tests/e2e_blackbox/`, `frontend/web-app/src/app/api/v1/[...path]/route.ts` | Multiple implemented backend capabilities are still unreachable or unproven through the gateway entrypoint, so partner/web-app integration confidence is overstated | 📋 To Do |

---

## 📊 Metrics

### Current State

| Metric            | Value                                            |
| :---------------- | :----------------------------------------------- |
| Completed Epics   | 24/24 fully done (see PROGRESS.md)               |
| Completed Stories | 86 done + 23 open audit stories                  |
| Completed SP      | 265/265                                          |
| Bugs Fixed        | 507 done + 4 Won't Do (archived to CHANGELOG)    |
| Open Bugs         | 0                                                |
| Tech Debt         | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)    |

---

_Last Updated: March 17, 2026 | 0 Active Epics · 23 Open Stories · 0 Open Bugs · 0 Tech Debt · 5 Spikes · 9 Deferred_
_All 507 bugs fixed + 4 Won't Do archived to CHANGELOG.md_
_Phase 7 Bug Sweep: ✅ COMPLETE (240/240 closed) — March 17, 2026. Verified: Maven 38/38, Frontend OK (44 routes, 79 pages), Playwright 544/544, Pytest 159/159._
_Phase 3 Bug Fixes: ✅ COMPLETE (34/34 closed) — March 16, 2026_
_Phase 2 Gateway Gaps: ✅ COMPLETE (GAP-001, GAP-002, GAP-006, GAP-007) — March 16, 2026_
_Phase 1 E2E Stabilization: ✅ COMPLETE (544 Playwright + 159 Pytest = 703 tests, 0 failures) — March 15, 2026_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
