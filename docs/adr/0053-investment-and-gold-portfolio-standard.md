# ADR-0053: Investment and Gold Portfolio Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk  
**Relates to**: ARCHITECTURE.md 3.2.9, ADR-0022 (Money), ADR-0038 (Saga)

---

## Context

`investment-service` (port 8009) exists in `ARCHITECTURE.md:3.2.9` but no ADR. Responsibilities: mutual funds, gold, portfolio. No decision on NAV source, gold price provider, settlement via wallet ledger, or fee. Need `BigDecimal 19,4` HALF_EVEN and saga for order → payment → settlement.

## Decision Drivers

* **NAV/gold price**: daily NAV from fund admin, gold from approved provider (similar to FX ADR-0050) — fallback last close.
* **Settlement**: wallet double-entry via `ADR-0049` journal, not direct balance update.
* **Fees**: B-Book 0.5% subscription, 0.1% redemption — config, not hardcode.
* **Idempotency**: `X-Idempotency-Key` per order.

## Considered Options

### Option A — Investment orders via saga + wallet ledger + provider price cache (chosen)

* **Pros**: reuses `saga-starter` `TransferSagaOrchestrator` pattern, `cache-starter` price TTL 1h, `outbox-starter` `payu.investment.order-*`.
* **Cons**: needs `InvestmentOrder` saga state — but already have saga infra.

### Option B — Direct wallet debit without saga

* **Pros**: simple.
* **Cons**: no compensation if NAV fetch fails — rejected for money path.

## Decision

**Saga-orchestrated investment with price cache and wallet journal.**

* `InvestmentOrder` (id, userId, productId, type `SUBSCRIPTION/REDEMPTION`, amount `DECIMAL 19,4`, status `PENDING→SETTLED|FAILED`, nav, priceAt, fee, idempotencyKey unique).
* Price: `NavProviderPort` (fund NAV) + `GoldPriceProviderPort` — cache `payu-invest-price` TTL 1h, provider via Vault, fallback last close + alert.
* Saga `InvestmentSagaOrchestrator` (steps: `RESERVE_WALLET` → `FETCH_NAV` → `SETTLE`) with compensation `RELEASE_WALLET` via reversal entry per `ADR-0049`.
* Ledger: `CoaCode.ASSET_INVESTMENT`, `LIABILITY_INVESTMENT_PAYABLE` — `JournalEntry` balanced.
* API `POST /v1/investments/orders` idempotent, `GET /v1/investments/portfolio/{userId}`.

## Rationale

A gives compensatable money path and price freshness vs B direct. Weighted 40% tech (saga/cache) 30% business (fee audit) 30% team (reuse saga).

## Consequences

**Positive**: NAV audit, saga compensation, fee configurable.
**Negative**: saga state persistence adds latency ~20ms.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Model | `investment/domain/model/InvestmentOrder.java` |
| 2 | Saga | `investment/application/saga/InvestmentSagaOrchestrator.java` |
| 3 | Cache | `cache-starter` `payu-invest-price` |
| 4 | Tests | `InvestmentSagaTest` `PENDING→SETTLED` + compensation |

**Verification**: `InvestmentServiceTest` order idempotent, `Journal.isBalanced` green, cache hit p95 <10ms.

---
*Created for investment-service — implementasi wajib refer ADR ini.*
