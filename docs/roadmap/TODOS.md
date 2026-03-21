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
| **Open Stories** |   0   | All completed ✅ (archived to CHANGELOG)               |
| **Tech Debt**    |   0   | All completed ✅                                      |
| **Spikes**       |   5   | ARCH-001 – ARCH-005                                   |
| **Deferred**     |   9   | P2-FE-003, OCP-007, OCP-010, DR-001, DEFER-001, RHPAM |
| **Open Bugs**    |   3   | 🟢 IDOR, Promo Kuota, & Redis Blockage (March 2026)      |

> **Completed Epics**: 24/24 fully done. All stories & tech debt cleared.
> See [`PROGRESS.md`](./PROGRESS.md) for completed Epics summary.
> **Closed bugs, stories & history**: See [`CHANGELOG.md`](../../CHANGELOG.md).

### 🐛 Bug Scorecard (3 Pending)

| Kategori                   | Open | Closed | Priority Range |
| :------------------------- | :--: | :----: | :------------- |
| Backend Logic              |   1  |    8   | P0             |
| Frontend Logic             |   0  |   11   | —              |
| Frontend-Backend Mismatch  |   0  |    0   | —              |
| Auth / Session             |   0  |    0   | —              |
| Shared Libraries           |   0  |    0   | —              |
| Test Coverage / Quality    |   0  |    0   | —              |
| Infrastructure / OpenShift |   0  |    1   | —              |
| Architecture               |   1  |    7   | P1             |
| Security (IDOR)            |   1  |    3   | P0             |
| **TOTAL**                  | **3** | **30** |               |

#### 🔴 Priority 0 (Critical)
- **[BUG-SECURITY-002]** Celah IDOR pada `TopUpController.java` dan `SubscriptionController.java` (`get` endpoint). Validasi kepemilikan (`validateOwnership`) tidak diimplementasikan, sehingga user berpotensi melihat data user lain.
- **[BUG-LOGIC-011]** Mengeksploitasi Kuota Promosi Tak Terbatas (Unsaved Entity State). Pada `PromoRedemptionService.java`, proses iterasi memotong kuota hanya di memori. Kode lupa memanggil `promoCodeRepository.save(promo)`, sehingga kuota bisa diklaim ribuan kali tanpa berkurang di DB.

#### 🟠 Priority 1 (High)
- **[BUG-ARCH-008]** Eksekusi Destruktif O(N) `redisTemplate.keys()` pada Latar Belakang. Di `SnapBiTokenService.java`, penggunaan `keys()` memblokir seluruh VM Redis di Production. Harus dimigrasi ke TTL-based management atau `SCAN`.

#### ✅ Closed in Phase 13 Audit (March 21, 2026)
- **[BUG-SECURITY-001]** ~~Hardcoded default passwords~~ → ✅ Removed.
- **[BUG-SECURITY-006]** ~~AB Testing cache leak~~ → ✅ Fixed userId-scoped keys.
- **[BUG-SECURITY-003]** ~~Missing @Valid + JSR-380~~ → ✅ Added to controllers & DTOs.
- **[BUG-LOGIC-002]** ~~Missing @Idempotent on transfer~~ → ✅ Verified fix.
- **[BUG-LOGIC-001]** ~~double for financial calc~~ → ✅ Changed to BigDecimal.
- **[BUG-ARCH-006]** ~~Bare new RestTemplate()~~ → ✅ Added proper timeouts.
- **[BUG-LOGIC-004]** ~~Manual mapToJson~~ → ✅ Migrated to ObjectMapper.
- **[BUG-LOGIC-005]** ~~@Scheduled without lock~~ → ✅ Added @SchedulerLock.
- **[BUG-LOGIC-006]** ~~@Async+@Transactional~~ → ✅ Removed async dependency.
- **[BUG-ARCH-007]** ~~Fallback throws RuntimeException~~ → ✅ Used CompletableFuture.failedFuture().

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

---

## 📊 Metrics

### Current State

| Metric            | Value                                            |
| :---------------- | :----------------------------------------------- |
| Completed Epics   | 24/24 fully done (see PROGRESS.md)               |
| Completed Stories | 109 done (86 + 23 test stories archived)          |
| Completed SP      | 265/265                                          |
| Bugs Fixed        | 678 done + 4 Won't Do                            |
| Open Bugs         | 3 — New Logical & Arch Findings (March 2026)      |
| Tech Debt         | 3/3 completed (SIMP-001, SIMP-002, SIMP-003)    |

---

_Last Updated: March 21, 2026 | 0 Active Epics · 0 Open Stories · 3 Open Bugs · 0 Tech Debt · 5 Spikes · 9 Deferred_
_All 678 bugs fixed + 4 Won't Do archived to CHANGELOG.md_
_Phase 13 Final Bug Audit Sweep: ✅ COMPLETE (30/30 closed) — March 21, 2026_
_Phase 12 E2E Coverage Gap Fixes: All 27 findings closed — March 17, 2026_
_Phase 10 Shared Lib Audit: 31 new findings — March 17, 2026_
_Phase 7 Bug Sweep: ✅ COMPLETE (240/240 closed) — March 17, 2026_
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
_Referensi: BCA Digital (blu), Xendit, Midtrans, GoPay, OVO, DANA, Flip, Jago_
