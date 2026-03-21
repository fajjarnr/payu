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
| **Open Bugs**    |  15   | 🟢 Logical Inspection Tahap Akhir (March 2026)          |

> **Closed bugs, stories & history**: See [`CHANGELOG.md`](../../CHANGELOG.md).

### 🐛 Bug Scorecard

| Kategori                   | Open | Closed | Priority Range |
| :------------------------- | :--: | :----: | :------------- |
| Backend Logic              |   4  |   12   | P0-P2          |
| Frontend Logic             |   5  |    7   | P1-P2          |
| Infrastructure / OpenShift |   0  |    1   | —              |
| Architecture               |   3  |    7   | P1-P2          |
| Security (PII/IDOR)        |   3  |   15   | P0-P1          |
| **TOTAL**                  | **15** | **42** |               |

#### 🔴 Priority 0 (Critical)

- [x] **[BUG-SECURITY-001]** ~~Hardcoded default passwords/secrets~~ (FIXED: Phase 13)
- [x] **[BUG-SECURITY-006]** ~~AB Testing cache state leak~~ (FIXED: Phase 13)
- [x] **[BUG-SECURITY-002]** ~~Celah IDOR pada TopUpController/SubscriptionController~~ (FIXED: Phase 13)
- [x] **[BUG-SECURITY-003]** ~~Defisiensi Validasi Payload API (Missing @Valid)~~ (FIXED: Phase 13)
- [x] **[BUG-SECURITY-004]** ~~Kebocoran PII (Nomor Telepon) ke Log~~ (FIXED: Phase 13)
- [x] **[BUG-SECURITY-005]** ~~AuditLogAspect PII leakage massal~~ (FIXED: Phase 13)
- [ ] **[BUG-SECURITY-027]** Broken Access Control pada `promotion-service` (Missing @PreAuthorize).
- [ ] **[BUG-SECURITY-008]** Account Lockout Bypass akibat Hardcoded Cache TTL (Redis 15m vs Config 60m).
- [ ] **[BUG-SECURITY-009]** Race Condition Brute-Force (Read-Modify-Write in Redis).
- [x] **[BUG-LOGIC-002]** ~~Missing Idempotency pada Transfer endpoint~~ (FIXED: Phase 13)
- [x] **[BUG-LOGIC-010]** ~~Absennya Hak Akses Inbound Transaction~~ (FIXED: Phase 13)
- [ ] **[BUG-LOGIC-011]** Mengeksploitasi Kuota Promosi Tak Terbatas (Unsaved Entity State in PromoRedemptionService).

#### 🟠 Priority 1 (High)

- [x] **[BUG-LOGIC-001]** ~~Floating point precision in financial calc~~ (FIXED: Phase 13)
- [x] **[BUG-LOGIC-003]** ~~Unbounded Pagination DoS~~ (FIXED: Phase 13)
- [x] **[BUG-LOGIC-004]** ~~Manual JSON Serializer (Vulnerable to injection)~~ (FIXED: Phase 13)
- [ ] **[BUG-LOGIC-014]** Metadata Paginasi Korup di `TransactionController.java`.
- [ ] **[BUG-LOGIC-015]** Pelanggaran Imutabilitas Audit Log di `DataAccessAuditService.java`.
- [x] **[BUG-ARCH-001]** ~~Inner enum placement violation~~ (FIXED: Phase 13)
- [x] **[BUG-ARCH-003]** ~~Hexagonal isolation violation (JPA on domain)~~ (FIXED: Phase 13)
- [x] **[BUG-ARCH-004]** ~~LocalDateTime usage (Migration to OffsetDateTime needed)~~ (FIXED: Phase 13)
- [x] **[BUG-ARCH-005]** ~~Lombok @Data on JPA entities~~ (FIXED: Phase 13)
- [x] **[BUG-ARCH-006]** ~~Bare new RestTemplate() without timeouts~~ (FIXED: Phase 13)
- [x] **[BUG-ARCH-007]** ~~Fallback throws RuntimeException (CF.failedFuture needed)~~ (FIXED: Phase 13)
- [ ] **[BUG-ARCH-008]** Eksekusi Destruktif O(N) `redisTemplate.keys()` di `SnapBiTokenService`.
- [ ] **[BUG-ARCH-009]** In-Memory Idempotency Store di `kyc-service` (Reset on Pod Restart).

#### 🟡 Priority 2 (Medium)

- [x] **[BUG-FE-001]** ~~Hardcoded colors (Tailwind emerald tokens migration)~~ (FIXED: Phase 13)
- [x] **[BUG-FE-002]** ~~MobileNav double locale / aria-label~~ (FIXED: Phase 13)
- [x] **[BUG-FE-003]** ~~Landing page double locale~~ (FIXED: Phase 13)
- [x] **[BUG-FE-004]** ~~Hardcoded Indonesian error messages~~ (FIXED: Phase 13)
- [x] **[BUG-FE-005]** ~~Hardcoded PII name on bank card mock~~ (FIXED: Phase 13)
- [x] **[BUG-FE-006]** ~~Missing error.tsx / global-error.tsx~~ (FIXED: Phase 13)
- [x] **[BUG-FE-007]** ~~Missing loading.tsx skeletons~~ (FIXED: Phase 13)
- [ ] **[BUG-FE-013]** Halaman notifikasi menavigasi ke route detail yang tidak ada.
- [ ] **[BUG-FE-015]** Beberapa halaman utama (QRIS, Security) masih hardcoded Indonesia.

---

## 🔍 Spikes (Research / Architecture Decision)

| Key      | Type  | Question                                                                              | Status   |
| :------- | :---- | :------------------------------------------------------------------------------------ | :------- |
| ARCH-001 | Spike | KYC di level PayU atau project client?                                                | 📋 To Do |
| ARCH-002 | Spike | Statement: PDF end-user atau JSON/CSV project client?                                 | 📋 To Do |
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | 📋 To Do |

---

_Last Updated: March 21, 2026 | 0 Active Epics · 0 Open Stories · 15 Open Bugs · 0 Tech Debt · 5 Spikes · 9 Deferred_
_All 678 original bugs fixed + 15 new audit findings pending._
_Phase 13 Final Bug Audit Sweep: ✅ COMPLETE (30/30 closed) — March 21, 2026_
