# ADR-0047: Frontend Nominal Branded Types & Strict Financial Money Precision Standard

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Frontend Architect  
**Relates to**: DX-TS-BRANDED-001, BE-PARTNER-001, AGENTS.md #1 (BigDecimal)  

---

## Context

`frontend/web-app/src/types/index.ts:21-80` uses `string` for `AccountId/UserId/TransactionId/Money` → `BE-PARTNER-001` `Number(user.id) → NaN`, `PartnerController` `Long` mismatch. `AGENTS.md:1` mandates `BigDecimal HALF_EVEN` `DECIMAL(19,4)` DB; frontend `number` (`IEEE 754`) corrupts `0.1+0.2`. Need `Money` as `string` at JSON boundary + branded `Id` types.

## Decision Drivers

* **No float money** — `Money.amount` `string` `"100000.00"`, arithmetic via `currency.ts` `BigInt`/`Decimal` lib.
* **Type safety** — `AccountId` ≠ `UserId` at compile.
* **Single source** — `gRPC common.proto Money` already `string amount` (ADR-0037).

## Considered Options

### Option 1 — Branded `type AccountId = string & {__brand:'AccountId'}` + `Money={amount:string,currency:string}` (dipilih)

Pros: zero runtime, `tsc` catches `Number(user.id)`. Cons: need helper `asAccountId`.

### Option 2 — Keep plain `string`/`number`

Pros: no change. Cons: `DX-TS-BRANDED-001` stays `OPEN` — ditolak.

## Decision

**Branded `Id` + `Money string` standard.**

* `frontend/web-app/src/types/index.ts`:
  ```ts
  export type AccountId = string & {__brand:'AccountId'};
  export type UserId = string & {__brand:'UserId'};
  export type TransactionId = string & {__brand:'TransactionId'};
  export interface Money { amount: string; currency: string; } // amount "0.00" HALF_EVEN
  ```
* `lib/currency.ts` `formatExactDecimal` `addMoney` etc use `BigDecimal` string math, never `number` for `Money`.
* `ArchUnit` `TS` lint: `no `Number(id)`, no `parseFloat` on `Money.amount` — use `Money` helpers.
* `gRPC` `Money` `string` governs; REST DTO `Money` mirrors it.

## Consequences

**Positive**: `tsc` catches `Id` mix, no `float` ledger error.

**Negative**: `asAccountId` cast — mitigasi ` zod` ` .brand`.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Types | `frontend/web-app/src/types/index.ts:21-80` |
| 2 | Currency | `frontend/web-app/src/lib/currency.ts` |
| 3 | Lint | `eslint` `no-restricted-syntax` `Number(` on branded |

**Verification**: `tsc --noEmit` catches `getPartnerById(Number(user.id))`; `Money` test `"0.1"+"0.2"="0.30"`.

---
*Created for DX-TS-BRANDED-001 — implementasi wajib refer ADR ini.*
