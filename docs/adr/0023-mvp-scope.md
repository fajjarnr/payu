# ADR-0023: MVP Scope Definition (Core Banking)

**Status**: Accepted
**Date**: 2026-08-11
**Deciders**: Product Owner, Principal Architect, Platform Engineer

## Context

PRD Phase 1 (Month 1-6) mendefinisikan MVP sebagai: basic account opening & eKYC,
transfer (antar PayU & BI-FAST), basic bill payment, single pocket system, virtual
debit card, dan TokoBapak integration. Namun audit 2026-08-11 menunjukkan: account &
auth **blocked** oleh P0 (ACCOUNT-001..004, LOGIN-001..006), wallet/transaction
money-flow live, dan partner production gate masih 11 item open. Mengejar seluruh
PRD Phase 1 sekaligus = beberapa bulan kerja pada feature yang belum tentu jadi
kriteria go-live partner. Perlu scope MVP yang realistis dan terukur.

## Decision Drivers

- **Time-to-value**: partner (TokoBapak) dan customer web bisa pakai produk paling cepat.
- **Money safety**: jalur uang (transfer + SNAP) harus bebas P0 sebelum apa pun.
- **Dependency**: onboarding & login adalah prasyarat semua fitur customer-facing.
- **Daya dukung**: menghindari fitur Phase 2-3 (PRD) masuk gate MVP.
- **Terukur**: kriteria selesai eksplisit (E2E + coverage + no-P0), bukan "semua sudah jalan".

## Considered Options

### Option A: Full PRD Phase 1 (account+eKYC, transfer+BI-FAST, bill, pocket, virtual card, TokoBapak)

- **Pros**: Sesuai PRD literal; coverage fitur lengkap.
- **Cons**: Account/auth blocker + bill payment + virtual card menambah bulan kerja;
  BI-FAST butuh jaringan nyata (simulator tidak membuktikan interop); risiko scope
  creep menunda go-live partner.

### Option B: Narrow MVP — Onboarding + Login Web + Transfer Internal + SNAP Payment/Refund (TokoBapak)

- **Pros**: Semua jalur uang esensial dalam satu alur customer→partner; auth/account
  P0 wajib dibereskan (dependency sehat); simulasi BI-FAST tetap di-test (parity),
  bukan gate; realistis 1-2 sprint per P0.
- **Cons**: Bill payment, virtual card, BI-FAST nyata keluar dari gate MVP —
  butuh ekspektasi stakeholder yang disepakati.

### Option C: Money-only (transfer internal + SNAP, tanpa onboarding/login baru)

- **Pros**: Tercepat (pakai seed user).
- **Cons**: Bukan produk utuh — customer web tidak bisa register/login; account/auth
  P0 tertunda (risiko keamanan tetap terbuka); menunda dependency paling penting.

## Decision

**Scope MVP = Option B**: alur utuh **onboarding → login web → transfer internal →
SNAP-BI payment/refund (TokoBapak)** dengan ledger double-entry exact + reconciliation.
Seluruh gate MVP wajib bebas P0.

### In-Scope (gate MVP)

| Area | Kondisi wajib |
|:---|:---|
| Onboarding + eKYC (account) | ACCOUNT-001..004 closed; DTO tanpa PII penuh; duplicate ditolak |
| Login web (auth) | LOGIN-002/003/004 closed: PKCE + revoke + rate-limit fail-safe; E2E browser |
| Transfer internal | scale 4 (CB-003), kompensasi reversal (CB-014), fee dipungut (CB-020), timeout adapter (CB-021) |
| SNAP-BI payment/refund | PARTNER-PROD-001..006 evidence (sudah live di sandbox), reconciliation 0 unmatched |
| Ledger | double-entry exact, idempotency natural key (ADR-0022), immutability DB (CB-012) |
| Web-app | money decimal string (PROD-043); referral contract fix (PROD-046) hanya jika fitur masuk web |
| Notifikasi dasar | PROD-045 (PII) wajib; provider nyata (CB-029) P1 dalam scope |
| Cashback SNAP | dedup (CB-026) — hanya jalur yang menyentuh SNAP payment |

### Out-of-Scope (defer, bukan gate MVP)

- BI-FAST/SKN/RTGS transfer nyata (parity simulator tetap di-test; CB-016 turun prioritas)
- Bill payment, top-up, subscription (CB-022 defer)
- Virtual debit card, QRIS payment (CB-017 defer — QRIS Phase 2 PRD)
- Multi-pocket (single pocket sudah ada; fitur tambahan defer)
- Investment, lending, PayLater, fx, statement (Phase 2-3 PRD; CB-009/CB-024 defer)
- Promo referral/loyalty redeem (CB-027/030 defer — non-SNAP)
- Mobile app (deferred scope eksisting)

## Gate MVP (Done criteria — semua wajib terverifikasi)

1. Semua P0 di jalur In-Scope closed (CB-001, 002, 003, 012, 014, 020, 021, 026, 029-PII).
2. E2E live di `payu-dev`: onboarding → login (browser) → transfer internal →
   SNAP payment → refund → ledger exact → replay idempotent (satu mutation).
3. Coverage ≥80% overall & 100% core domain pada service In-Scope, integration tests
   required di CI (CB-005).
4. `SERVICES.md` tidak kontradiktif dengan TODOS (CB-004).
5. No P0 open di jalur In-Scope; production gate HA/DR (PARTNER-PROD-007..011) adalah
   gate production, bukan gate MVP.

## Rationale

- **Option B** dipilih: memaksimalkan value per unit effort, memaksa dependency
  (auth/account) dibereskan, dan membatasi gate pada jalur uang yang benar-benar
  dipakai partner. Option A melanggar driver time-to-value dan daya dukung;
  Option C melanggar dependency (auth tetap rusak di produk yang diklaim MVP).
- **BI-FAST keluar** dari gate karena simulator tidak membuktikan interop jaringan
  nyata (membutuhkan sertifikasi BI) — parity lokal tetap memvalidasi logika service.
- Keputusan ini **re-map backlog CB-***: item Out-of-Scope turun prioritas (P2/P3)
  kecuali menyentuh jalur In-Scope.

## Consequences

**Positive**:

- Target terukur: "MVP done" = gate 1-5 lulus, bukan perasaan.
- Fokus: 13 P0 menyempit ke ~8 yang In-Scope; sisanya defer tanpa hilang (tetap di TODOS).
- Semua fix tunduk ADR-0022 (money & idempotency standard) sejak awal.

**Negative**:

- Stakeholder harus menerima bill payment/virtual card/BI-FAST nyata di luar MVP.
- PARI simulator untuk BI-FAST/QRIS tetap dipertahankan (biaya kecil).

**Risiko**:

- Scope creep kembali (mitigasi: gate eksplisit + review per sprint).
- SNAP production gate (PARTNER-PROD-007..011) tertunda → TokoBapak go-live
  production butuh gate tambahan (mitigasi: MVP = live di `payu-dev` + SIT green;
  production gate terpisah).

## Implementation Notes

1. Re-map prioritas di `docs/roadmap/TODOS.md`: item Out-of-Scope ditandai defer
   (BI-FAST CB-016 → P2, subscription CB-022 → P2, PayLater CB-024 → P2, QRIS CB-017 → P2,
   lending CB-009 → P3, promo CB-027/030 → P3).
2. Sprint 1 dimulai: CB-003 → CB-014 → CB-020 (money path, satu service), paralel CB-001/002 (account/auth) di worktree terpisah.
3. E2E MVP suite dibuat di `tests/e2e_blackbox` (onboarding→login→transfer→SNAP→refund).
4. Jika Product Owner mengubah scope, update ADR ini (bukan buat ADR baru) dan re-map backlog.

---
*Created 2026-08-11. Referensi: PRD.md §9 Phase 1, ADR-0022 (Money & Idempotency),
TODOS.md (CB-*), PROGRESS.md (MVP-001/003/004 live evidence).*
