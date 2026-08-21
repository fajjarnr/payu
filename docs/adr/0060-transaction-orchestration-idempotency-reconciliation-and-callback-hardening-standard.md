# ADR-0060: Transaction Orchestration — Idempotency, Reconciliation & Callback Hardening Standard

**Status**: Proposed  
**Date**: 2026-08-22  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk & Compliance  
**Relates to**: ADR-0022 (Money & Idempotency), ADR-0041 (Transactional Outbox), ADR-0042 (ShedLock), ADR-0049 (Wallet Immutable Ledger), ADR-0029 (ISO20022), ADR-0025 (SNAP-BI), PADG 14/2025 BI-FAST

---

## Context

`backend/transaction-service` (`TransactionServiceApplication.java`, `TransactionEntity.java:34`, `InitiateTransferCommandHandler.java:58`, `DisbursementService.java:64`) saat ini:

* Money: ✅ `Money.java:55` `HALF_EVEN` `SCALE 4` + `V22 DECIMAL(19,4)` — best practice.
* Ledger truth: ✅ di `wallet-service` (`LedgerEntry` append-only, `JournalEntry.isBalanced()`), `transaction-service` hanya orchestration state machine (mutable `transactions.status`) — sudah benar per CONTEXT.md, tapi tidak ada ADR yang mengikat.
* Idempotency: ⚠️ `X-Idempotency-Key` ada di `TransactionController` & `DisbursementControllerIdempotencyTest`, lookup `findByIdempotencyKey` di TX (`InitiateTransferCommandHandler.java:76`) dan `DisbursementService.java:77`, tapi `V14__add_transaction_idempotency_key.sql` hanya `CREATE INDEX` bukan `UNIQUE`; `disbursements` & `batch_disbursements` sudah `UNIQUE(idempotency_key)` (`V13`). Race 2 req concurrent masih bisa duplikat payout/transfer. Global best practice (Wise, JPM, BI-FAST guide, xvica/idempotency, hashnode exactly-once) wajib `UNIQUE(tenant_id, idempotency_key)` + interceptor `IdempotencyInterceptor` dengan `INSERT ... ON CONFLICT DO NOTHING`.
* Outbox: ✅ `outbox-starter` + `events-starter` CloudEvents `payu.<domain>.<event>.v1` via `TransactionEventPublisherAdapter` — benar. Namun internal transfer sudah atomik 1-hop (`transferBalance`), interbank masih manual `reserve → external call → settleInterbankTransfer FOR UPDATE` tanpa `saga-starter` persistent state + tanpa result-table untuk retry.
* Reconciliation: ❌ belum ada. PADG 14/2025 pasal 18 mewajibkan rekonsiliasi BI-FAST periodik (core vs member statement ≥1×/hari). `PaymentExpiryScheduler` & `TransactionArchivalScheduler` ada tapi tidak polling `PENDING >5m → inquiry BI-FAST status`. Three-way reconciliation (PSP/ledger/bank) per `rzifi reconciliation-as-product` belum ada.
* Callback: ⚠️ `CallbackSignatureFilter` HMAC ada, `settleInterbankTransfer` pakai `findByReferenceNumberForUpdate` (row lock, race-free), tapi belum ada inbox table untuk replay protection + belum mTLS untuk BI-FAST prod. `V27 RLS` + tenant sudah ada.
* Hexagonal: ❌ `TransactionEntity` dipakai sebagai JPA `@Entity` + domain model langsung di `TransactionService`/`TransactionUseCase` — TODO `BUG-ARCH-003` di `TransactionEntity.java:12`. `Money` duplikat antara `transaction-service` & `api-commons`.

Industry gap: 2025-2026 sources (pratikdhanave stored-value ledger, dev.to multi-channel, rzifi three-way reconciliation, hashnode exactly-once, exirom outbox, rockthejvm never-call-APIs-inside-TX) semua converge: **idempotency DB + outbox + inbox + reconciliation + saga/result-table** adalah paket, bukan opsi.

## Decision Drivers

* **BI/OJK compliance**: PADG 14/2025 rekonsiliasi harian, SNAP-BI `transfer/status` inquiry after timeout (BRIAPI docs), PCI-DSS tamper-evident ledger.
* **Money safety**: exactly-once effect (ledger once, rail once, bank reconciled once) — no double debit/credit.
* **Operational**: `PENDING` stuck handling tanpa manual ops; callback replay & network 504 after rail accepted.
* **Hexagonal purity**: domain vs persistence separation (`BUG-ARCH-003`).
* **Scope clarity**: `billing-service::BillPayment` vs `transaction-service::Disbursement` vs `loan-origination::Disbursement` boundary sudah ada via `billing-service` + simulators (`biller-simulator`, `va-simulator`, `bi-fast-simulator`).

## Considered Options

### Option A — Harden in-place: modular monolith + DB UNIQUE + inbox + reconciler + outbox (chosen)

* **Pros**: 1 service tetap, deploy sederhana, 1 DB + RLS; `V14` tinggal upgrade ke `UNIQUE(tenant_id, idempotency_key)`; `Disbursement` idempotency sudah benar tinggal tiru; reconciler reuse `ShedLock` + `AdaptiveBatchDisbursementJob` pattern; inbox table kecil; tidak perlu pecah service.
* **Cons**: `transactions` table tetap shared untuk 7 use-case (transfer/VA/split/bill batch) — contention masih possible di >500 TPS; butuh ArchUnit guard paket `transfer/`, `disbursement/`, `va/`, `splitbill`.

### Option B — Pecah fisik: `transaction-service` (transfer+QRIS) vs `payout-collection-service` (disbursement+VA)

* **Pros**: isolate batch spike, scale independent, clear bounded context seperti Wise.
* **Cons**: tambah Kafka topics `payu.disbursement.*`, saga cross-service, 2× DB + 2× RLS + 2× ShedLock; overhead besar untuk lab scale; butuh migrasi data `disbursements`/`virtual_accounts`.

### Option C — Event sourcing full (event store, bukan RDBMS journal)

* **Pros**: audit kuat.
* **Cons**: heavy, team unfamiliar, no BI requirement — overkill.

## Decision

**Option A — Harden in-place (modular monolith).**

1. **Idempotency DB**: `V28` add `UNIQUE(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL` pada `transactions` (mirip `disbursements` `V13`); enable `IdempotencyInterceptor` (sudah proven di `TransactionControllerConcurrencyIdempotencyTest`) untuk `/api/v1/transactions/transfer`, `/disbursements`, `/va`, `/split-bills`; `findByIdempotencyKey` tetap sebagai fast-path, DB sebagai final guard. Header `X-Idempotency-Key` per SNAP-BI.

2. **Domain vs Entity**: pisah `Transaction` domain (immutable VO, package `domain/model`) dari `TransactionEntity` JPA (adapter); port `TransactionPersistencePort` return domain, bukan entity. Lombok fallback jika gagal >2× per AGENTS.md #7. `Money` single source via `api-commons`.

3. **Orchestration**:
   * Internal transfer: tetap atomik `walletServicePort.transferBalance()` (no saga).
   * Interbank (BI-FAST/SKN/RTGS): `reserveBalance` (hold) → write `TransactionEntity(PENDING)` + `outbox` row **same TX** → async dispatch via `outbox-starter` (polling `SKIP LOCKED`, interval + `ShedLock` `usingDbTime()`, `lockAtMostFor` per Context7 `/lukas-krecan/shedlock`) → rail call **outside DB TX** (never inside, per rockthejvm) → callback `settleInterbankTransfer` (`FOR UPDATE` + inbox dedup `inbox_events` table keyed by rail `referenceNo`).
   * Compensasi: `releaseBalance` on rail failure + DLQ `payu.transaction.failed.v1.dlq`.
   * Resilience: `Resilience4j` per-rail `CircuitBreaker`/`Retry`/`Bulkhead` instance terisolasi (payment vs inventory analogy), aspect order `CircuitBreaker(1) < Retry(2)` — lihat Context7 `/resilience4j/resilience4j`.

4. **Inbox + Result Table**: `inbox_events` (rail callback dedup) + `aggregate_results` (simpan `referenceNumber`, rail response, `fanout_order`) untuk LIFO compensation — pattern `outbox + result table + saga` (rockthejvm).

5. **Reconciliation**: `ReconciliationScheduler` (`@Scheduled` + `@SchedulerLock(name="biFastReconciliation", lockAtMostFor="9m")` via `ShedLockConfig` JDBC) tiap 5m (intra-day) + T+1 full (member statement vs core vs bank). Match key `referenceNumber` (propagated ke rail metadata), tolerance `IDR ±0.00` (strict), exception taxonomy 9 type (PSP-only, ledger-only, amount/status mismatch, timing, bank shortfall, etc. per rzifi), watermark, dashboard. Auto-heal: `PENDING >5m` → `GET /snap/v1.0/transfer/status` (BRIAPI `latestTransactionStatus 00/01/03/06`) → auto `commit/release`.

6. **Callback hardening**: HMAC + `X-Idempotency-Key` + `FOR UPDATE` tetap; prod BI-FAST wajib mTLS (Vault cert via `security-starter`) + rotation; inbox dedup makes replay no-op.

7. **Modularity**: package `id.payu.transaction.transfer`, `disbursement`, `va`, `splitbill`, `routing` + `ArchitectureTest` forbids cross-import; `SmartRoutingService` stays but per-rail rate table externalized.

## Rationale

* PADG 14/2025 & SNAP-BI mengikat reconciler + status inquiry — bukan optional.
* Global tier-1 (JPM, Wise, GoPay) semua pakai `append-only ledger + UNIQUE idempotency + outbox + inbox + reconciliation` sebagai paket.
* `billing-service` & `loan-origination` sudah prove pemisahan domain — tidak perlu pecah fisik lagi (Option B) sampai `batch_disbursements` terbukti ganggu SLO transfer (ukur `pg_stat_activity` + `pg_locks`).
* `Resilience4j` per-rail isolation + `ShedLock usingDbTime()` adalah Context7 best practice, sudah ada di `pom.xml:109/113` dan `ShedLockConfig.java`.

## Consequences

**Positive**:
* Exactly-once effect end-to-end (ledger/rail/bank) — audit BI lulus.
* No double payout dari race header.
* Stuck `PENDING` auto-heal, callback replay idempoten.
* Hexagonal clean, `Money` single source.

**Negative**:
* `V28` unique migration butuh backfill dedup check (rare duplicate `NULL` keys: `WHERE idempotency_key IS NOT NULL` mitigates).
* Inbox + reconciler tambah 2 table + 1 scheduler — observability (`BusinessMetrics`, `micrometer-tracing`) perlu dashboard baru.

## Implementation Notes

* Flyway `V28__add_unique_tenant_idempotency_to_transactions.sql`: `CREATE UNIQUE INDEX CONCURRENTLY idx_transactions_tenant_idempotency_unique ON transactions(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;`
* `SecurityConfig` enable `IdempotencyInterceptor` for transfer/disbursement/VA; `outbox-starter` topic `payu.transaction.initiated.v1` + `.dlq` per `ADR-0041` + `ADR-0026`.
* `ShedLockConfig` already uses `JdbcTemplateLockProvider` `usingDbTime()` + `forceUtcTimeZone()` — add `ReconciliationScheduler` with `lockAtMostFor="9m" lockAtLeastFor="30s"`.
* `application.yml` `resilience4j.circuitbreaker.circuitBreakerAspectOrder=1`, `resilience4j.retry.retryAspectOrder=2`, per-service instance `bifastService`, `sknService`, `rgsService`.
* Contract: `POST /api/v1/transactions/transfer` requires `X-Idempotency-Key`; callback `POST /api/v1/transactions/callback/interbank` requires HMAC + `Idempotency-Key`.

---
*Created via @principal-architect — references web research 2026-08-22 (pratikdhanave, dev.to, rzifi, hashnode, exirom, rockthejvm, PADG 14/2025, BRIAPI SNAP-BI) + Context7 ShedLock/Resilience4j + CodeGraph `TransactionEntity`, `InitiateTransferCommandHandler`, `DisbursementService`, `TransactionJpaRepository`*
