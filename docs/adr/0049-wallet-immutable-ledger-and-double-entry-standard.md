# ADR-0049: Wallet Immutable Ledger and Double-Entry Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk & Compliance  
**Relates to**: ADR-0022 (Money & Idempotency), ADR-0029 (ISO20022 Clearing), AGENTS.md #1-2, CONTEXT.md §Wallet

---

## Context

`backend/wallet-service` is the source of truth for balances. Current code (`LedgerEntry.java:10`, `Wallet.java:11`) has:

* `LedgerEntry` with `transactionId, journalEntryId, accountId, coaCode, entryType, amount, balanceAfter` — append-only intent, but no explicit `JournalEntry` header entity and no DB-level immutability guard.
* `Wallet` with mutable `balance` + `reservedBalance` (`reserve/commit/release` in `Wallet.java:55/66/78`) — materialized view updated in-place with optimistic locking `version`.
* Money handling uses `BigDecimal` but `balanceAfter` reconciliation against sum of `LedgerEntry` not enforced; risk of drift between `wallet.balance` and ledger sum.
* Industry (BI/OJK, PCI-DSS, double-entry bookkeeping) requires: no UPDATE/DELETE on financial facts, corrections via reversal entries, atomic journal (debits=credits), idempotency, audit trail.

`AGENTS.md:1` mandates `BigDecimal HALF_EVEN` `DECIMAL(19,4)`; `AGENTS.md:2` mandates immutable ledger. No dedicated ADR existed — only implicit via `ADR-0022` and `ADR-0029`.

## Decision Drivers

* **Immutability audit**: OJK/BI require tamper-evident ledger — no UPDATE/DELETE, only append + reversal.
* **Double-entry invariant**: every business transaction must have balanced debits=credits (journal `isBalanced()`).
* **Idempotency**: `X-Idempotency-Key = transactionId` replay returns original `JournalEntry` without double posting.
* **Separation**: `Wallet` = mutable read-model for fast `availableBalance` checks; `ledger_entries` = immutable source of truth.
* **Zero surprise**: CoA codes explicit, not magic strings.

## Considered Options

### Option A — JournalEntry header + append-only LedgerEntry + materialized Wallet (chosen)

* **Pros**: `JournalEntry` header gives atomicity (one `transactionId` → one journal, 2+ entries), DB `CHECK`/`trigger` prevents UPDATE/DELETE on `ledger_entries`, `Wallet` stays mutable with `version` for fast holds but reconciled via scheduled `SUM(ledger)`. Aligns with `ADR-0022` idempotency + `ADR-0029` CoA.
* **Cons**: extra table `journal_entries`; need `JournalPersistenceAdapter` transactional write (journal+entries+outbox same TX).

### Option B — LedgerEntry only, no header, Wallet as source of truth

* **Pros**: fewer tables.
* **Cons**: no atomic grouping, invariant must be checked across arbitrary rows, harder to reverse whole transaction, audit trail ambiguous — rejected per BI clearing requirement.

### Option C — Event sourcing ledger (event store, not RDBMS journal)

* **Pros**: strong audit.
* **Cons**: heavy migration, CQRS read-model complexity, team unfamiliar, no BI requirement — overkill for current scale.

## Decision

**JournalEntry + immutable LedgerEntry + materialized Wallet.**

* `JournalEntry` entity (id UUID, transactionId UUID unique, createdAt) — persisted first.
* `LedgerEntry` (id, transactionId, journalEntryId FK, accountId, coaCode enum, entryType DEBIT/CREDIT, amount DECIMAL(19,4) HALF_EVEN, currency, balanceAfter, referenceType/Id, createdAt) — `INSERT` only; Flyway `REVOKE UPDATE,DELETE ON ledger_entries FROM payu_app`; `FORCE ROW LEVEL SECURITY` per `ADR-0033`.
* `Wallet` (accountId PK, balance, reservedBalance, currency, status, version, updatedAt) — updated inside same TX as journal write; `hasSufficientBalance()` checks `balance - reservedBalance`; `reserve/commit/release` mutate wallet but each mutation appends corresponding ledger lines.
* Invariant: `Journal.isBalanced()` in domain + DB `CHECK`/`DEFERRABLE` (sum debits = credits per `journalEntryId`) + ArchUnit test.
* Idempotency: `journal_entries.transactionId` unique; replay returns existing journal without new entries (port `IdempotencyStore` via `X-Idempotency-Key`).
* Reversal: new `JournalEntry` with opposite `entryType`, `referenceType=REVERSAL`, `referenceId=originalJournalId` — never `DELETE`.
* Topic `payu.wallet.balance-reserved.v1` / `balance-committed` / `balance-released` via `outbox-starter` CloudEvents `payu.<domain>.<event>.v<n>` + `.dlq`.
* CoA enum top-level file `CoaCode.java` (ASSET_WALLET, ASSET_RESERVED, LIABILITY_CLEARING, etc.) per `ADR-0029`.

## Rationale

Maps to drivers: A gives audit-ready immutability (BI/OJK) with minimal extra complexity vs C, and true double-entry vs B. Wallet stays fast for `availableBalance` but remains derivable from ledger for reconciliation (`SELECT SUM`). Reuses existing `outbox-starter` and `saga-starter` compensation via reversal (immutable). BigDecimal `19,4` `HALF_EVEN` satisfies `AGENTS.md:1`.

## Consequences

**Positive**:

* Ledger tamper-evident, OJK audit pass; reversal trail complete.
* `Journal.isBalanced()` catches mismatched posts at service layer + DB.
* Idempotent replay safe for retries and `ShedLock` jobs.

**Negative**:

* Flyway `REVOKE` needs `payu_migrator` vs `payu_app` role split per `ADR-0033`.
* Extra write (journal header) ~5% latency — mitigated by single TX and batch insert.
* Scheduled reconciler needed (`wallet.balance` vs `SUM(ledger)`) — add `WalletReconciliationJob` with `ShedLock`.

## Implementation Notes

| Step | Target | File / Action |
|---|---|---|
| 1 | Glossary | `CONTEXT.md` §Wallet (done) |
| 2 | Backlog | `docs/roadmap/TODOS.md` WALLET-001 ref this ADR |
| 3 | CoA | `wallet/domain/model/CoaCode.java` enum top-level |
| 4 | Journal | `wallet/domain/model/JournalEntry.java` + `Journal.java` aggregate `isBalanced()` |
| 5 | Ledger | `ledger_entries` Flyway Vxx `DECIMAL(19,4)`, `REVOKE UPDATE,DELETE`, `CHECK entryType`, index `(accountId, createdAt)` |
| 6 | Persistence | `JournalPersistenceAdapter` save journal+entries+outbox in `@Transactional`; `LedgerEntryMapper` |
| 7 | Wallet | keep `Wallet.java` `reserve/commit/release` but call from `JournalService` after ledger append, versioned |
| 8 | Idempotency | unique `transactionId`, `WalletServiceIdempotencyTest` |
| 9 | Tests | `JournalServiceTest.isBalanced`, `WalletReconciliationTest`, `ArchUnit` no `UPDATE ledger_entries` |
| 10 | Outbox | `payu.wallet.*.v1` CloudEvents via `outbox-starter` |

**Verification**: `Journal.isBalanced` green, `ledger_entries` `UPDATE` fails for `payu_app`, idempotent replay returns same `journalId`, p95 `<50ms` for journal write.

---
*Created for WALLET-001 — implementasi wajib refer ADR ini + ADR-0022/0029.*
