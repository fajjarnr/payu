# ADR-0022: Money & Idempotency Standard (Financial Integrity Baseline)

**Status**: Accepted
**Date**: 2026-08-11
**Deciders**: Principal Architect, Quality Engineer, Platform Engineer

## Context

Audit per fitur 2026-08-11 (7 pass, berbasis source code) menemukan 25+ inkonsistensi
financial-integrity yang tersebar: `Money` scale 2 di transaction-service vs
`DECIMAL(19,4)` di DB (PROD-047, FX-001); idempotency DB di transfer tapi cache-only
fail-open di QRIS (QRIS-001); fee di-response tapi tidak dipungut (FEE-001);
kompensasi release setelah commit menghilangkan dana (TX-003); sell/credit dengan
reference random → double payout (INVEST-001); dan pola dedup yang hilang
(cashback/loyalty/referral). Tanpa standard yang mengunci, 13 P0 akan di-fix
inconsisten dan audit berikutnya menemukan bug kelas yang sama.

## Decision Drivers

- **Money safety** (utama): tidak ada data loss/precision loss, tidak ada double post/payout.
- **Konsistensi lintas service**: satu aturan untuk semua 23 service.
- **Auditability**: setiap mutasi finansial bisa di-reconcile (ledger immutable, traceable).
- **Biaya migrasi minimal**: pilih standard yang paling dekat dengan kondisi mayoritas code saat ini.

## Considered Options

### Option 1: Scale 2 (ISO 4217 minor unit) — status quo transaction-service

- **Pros**: Cocok untuk IDR (tidak ada pecahan sen resmi); serialisasi pendek.
- **Cons**: **Menghilangkan 2 digit** sebelum persistence untuk FX/fee/amount 4-desimal
  (kolom DB sudah `DECIMAL(19,4)` di semua migration terbaru V22/V104/V5); sudah
  terbukti bug di 2 service; menciptakan dua standard (app 2 vs DB 4).

### Option 2: Scale 4 (`DECIMAL(19,4)`, `HALF_EVEN`) — standard platform existing

- **Pros**: Menyamakan app dengan DB (19,4 sudah ada di V3 ledger, V22 transaction,
  V104 wallet, V5 fx); presisi cukup untuk FX (rate 19,8 → hasil konversi 4);
  HALF_EVEN mencegah bias rounding; mayoritas domain sudah `BigDecimal`.
- **Cons**: Nilai tampilan perlu formatting ke 2 digit (presentation concern, bukan domain).

### Option 3: Idempotency cache-only (Hot Rod, TTL 24h) — status quo QRIS/VA

- **Pros**: Latency rendah, tanpa kolom baru.
- **Cons**: **Fail-open saat cache down** (bug nyata LOGIN-004/QRIS-001); window setelah
  TTL → replay double-charge; tidak ada audit trail natural key.

### Option 4: Idempotency natural key di DB (unique constraint + replay validation) — status quo transfer/wallet/billing/lending

- **Pros**: Fail-closed; permanen (tanpa TTL); natural key (`referenceId`/`partnerRefundNo`/
  `idempotencyKey`) bisa di-reconcile; sudah dipakai pola terbaik di repo (WalletService,
  PaymentService checkpoint, LoanManagementService, SnapBiPaymentService).
- **Cons**: Kolom unique + query tambahan; TTL cache boleh tetap ada sebagai fast-path.

## Decision

1. **Semua nilai moneter memakai `BigDecimal` scale 4 dengan `RoundingMode.HALF_EVEN`**,
   dipersist ke `DECIMAL(19,4)` — tidak ada `float`/`double`, tidak ada `setScale(2)`
   di jalur money. Format tampilan (2 digit) hanya di layer presentation.
2. **Semua money write (payment, transfer, refund, settlement, credit/debit wallet,
   fee, cashback, redeem) wajib idempotent via natural key unik di DB** (unique
   constraint + replay validation mengembalikan hasil existing); cache boleh jadi
   fast-path tetapi **harus fail-closed** saat cache unavailable.
3. **Setiap credit/debit wallet wajib reference deterministik** (referenceId dari
   business key — bukan `UUID.randomUUID()`), dan wallet credit/debit idempotent
   terhadap reference tersebut.
4. **Kompensasi kegagalan transaksi finansial wajib `reversal entry`** (immutable
   ledger) pada state yang benar — dilarang `release` setelah `commit`, dilarang
   koreksi via UPDATE/DELETE.
5. **Fee wajib dipungut** (masuk amount yang di-reserve/commit + ledger entry fee)
   **atau dinyatakan 0 eksplisit** — dilarang fee hanya di response.
6. **Ledger immutability di-enforce di DB** (REVOKE UPDATE/DELETE atau trigger
   `prevent_ledger_update`) — bukan hanya konvensi aplikasi.

## Rationale

- **Scale 4 (Option 2)** menang karena standard DB sudah 19,4 di semua service dan
  `BigDecimal` sudah dominan di domain; Option 1 terbukti merusak data (PROD-047)
  dan memaksa dual standard. `HALF_EVEN` sesuai aturan repo (AGENTS.md Rule #1).
- **Idempotency DB natural key (Option 4)** menang atas cache-only (Option 3) karena
  fail-closed + auditability — dua driver utama. Pola ini sudah terbukti di
  wallet/billing/lending/partner; yang di-fix hanya service yang belum (QRIS,
  scheduled transfer, sell, cashback, loyalty, referral).
- **Reversal-based compensation** mengikuti ADR tentang immutable ledger (double-entry,
  no UPDATE/DELETE financial facts) — TX-003 adalah bukti konsekuensi melanggarnya.

## Consequences

**Positive**:

- Satu standard lintas 23 service — audit berikutnya bisa automated (grep `setScale(2)`,
  `UUID.randomUUID()` pada credit, idempotency annotation per money endpoint).
- Fix P0 jadi konsisten: CB-003, CB-010, CB-014, CB-016, CB-017, CB-020, CB-021,
  CB-022, CB-023, CB-024, CB-026..CB-031 semua tunduk pada aturan ini.
- Replay/idempotency bisa diverifikasi test: 10 concurrent → 1 mutation.

**Negative**:

- Migrasi terbatas: `Money` scale 2 → 4 di transaction-service + test update
  (`MoneyTest` yang assert scale 2 harus diubah — ini memvalidasi bug, bukan behavior).
- Kolom/constraint baru di service yang belum punya natural key (QRIS transaction,
  cashback, loyalty).
- Format tampilan perlu di-handle di presentation layer (web-app decimal string —
  selaras PROD-043).

## Implementation Notes

1. **CB-003 (transaction Money scale 4)** — ubah `SCALE`, `round()`, `normalizeAmount`;
   update `MoneyTest` (hapus assert scale 2); regression arithmetic/serialization/DB round-trip.
2. **CB-010 (fx fee scale 4)** — `FxRateService.java:108` `setScale(4, HALF_EVEN)`.
3. **CB-014 (kompensasi TX-003)** — setelah commit sukses, kegagalan credit → reversal,
   bukan release.
4. **CB-017 (QRIS idempotency DB)** — simpan idempotency key di `TransactionEntity`
   + `findByIdempotencyKey` (pola transfer); fail-closed saat cache down.
5. **CB-020 (fee dipungut)** — reserve amount+fee + ledger entry fee.
6. **CB-023 (sell reference deterministik)** — `creditBalance(userId, amount, fixedRef)`.
7. **CB-026/027/030 (dedup natural key)** — unique constraint + cek existing.
8. **CB-012 (ledger immutability DB)** — REVOKE/trigger + test migration menolak UPDATE.
9. Setiap fix wajib red-first test yang gagal pada behavior lama (TDD AGENTS.md Rule 12).

---
*Created 2026-08-11. Referensi: AGENTS.md Rule #1/#2/#3, ADR-0007 (database per service),
ADR-0013 (testing), temuan audit PROD-047/FX-001/TX-003/QRIS-001/FEE-001/INVEST-001/PROMO-001.*
