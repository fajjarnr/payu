# ADR-0054: Dispute and Chargeback Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Risk & Compliance  
**Relates to**: ARCHITECTURE.md 3.2.21, ADR-0049 (Ledger), ADR-0038 (Saga)

---

## Context

`dispute-service` (port 8098) listed in `ARCHITECTURE.md:3.2.21` but no ADR. Need decision for refund/dispute/chargeback lifecycle, evidence, and ledger compensation. OJK requires dispute audit 1 year and reversal via ledger.

## Decision Drivers

* **Lifecycle**: OPEN→UNDER_REVIEW→RESOLVED (REFUND/REJECT)→CLOSED, chargeback via scheme.
* **Evidence**: attachment store (S3), not DB blob.
* **Ledger**: dispute refund via reversal entry per `ADR-0049`, not direct credit.
* **SLA**: 14d dispute resolution.

## Considered Options

### Option A — Dispute saga with evidence S3 + reversal journal (chosen)

* **Pros**: saga handles `HOLD` → `REFUND` with compensation, S3 evidence scalable, outbox audit.
* **Cons**: S3 infra — already have `barmanObjectStore` S3 via `ADR-0031`.

### Option B — Direct DB refund

* **Pros**: simple.
* **Cons**: no saga compensation, no evidence audit — rejected.

## Decision

**Dispute saga with S3 evidence and reversal journal.**

* `DisputeCase` (id, transactionId, userId, type `REFUND/CHARGEBACK`, status, reason, evidenceKeys S3, slaDueAt, createdAt) — `FORCE RLS` tenant.
* `POST /v1/disputes` idempotent, `POST /:id/evidence` → S3 presigned URL, `PATCH /:id/resolve` → saga `DisputeSagaOrchestrator` (steps: `HOLD_AMOUNT` → `REVERSE_JOURNAL` via `ADR-0049`).
* Outbox `payu.dispute.case-opened.v1` + `case-resolved`.
* Evidence S3 `payu-disputes/{caseId}/*` with KMS.

## Rationale

A gives audit + compensatable ledger vs B. Weighted 40% tech (saga/S3) 30% business (OJK) 30% team (reuse S3).

## Consequences

**Positive**: dispute audit 1y, reversal ledger clean.
**Negative**: S3 presigned URL expiry 1h — handle refresh.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Entity | `dispute/entity/DisputeCaseEntity.java` |
| 2 | Saga | `dispute/saga/DisputeSagaOrchestrator.java` |
| 3 | S3 | `infrastructure/helm/dispute` S3 bucket |

**Verification**: `DisputeTest` `OPEN→RESOLVED` refund creates reversal journal balanced.

---
*Created for dispute-service — implementasi wajib refer ADR ini.*
