# ADR-0055: Promotion, Cashback and Reward Saga Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk  
**Relates to**: ARCHITECTURE.md 3.2.13, ADR-0015 (Drools), ADR-0038 (Saga), ADR-0049 (Ledger)

---

## Context

`promotion-service` handles campaigns, vouchers, cashback, rewards. No ADR for eligibility rules (campaign often changes) and cashback settlement (needs ledger). `ARCHITECTURE.md:3.2.13` plus `ADR-0015` Phase2 mentions promotion rules but no decision. Need idempotent cashback via ledger and rule engine for eligibility.

## Decision Drivers

* **Rules**: campaign eligibility changes weekly — need Drools DRL/DMN via `rules-starter` (like lending ADR-0048).
* **Ledger**: cashback is money — ledger reversal if campaign fails, saga.
* **Idempotency**: cashback per `transactionId` once.

## Considered Options

### Option A — Drools for eligibility + saga for cashback settlement (chosen)

* **Pros**: rules externalized (marketing can propose DRL), saga ensures `CASHBACK_PENDING→SETTLED` with compensation.
* **Cons**: drools dep — already in `lending` via `rules-starter`.

### Option B — Hardcoded eligibility

* **Pros**: simple.
* **Cons**: weekly campaign needs deploy — rejected per `ADR-0015` rationale.

## Decision

**Drools eligibility + saga cashback.**

* `PromotionRule` DRL `rules/promotion/eligibility.drl` (fact `PromotionFact {userId, transactionAmount, campaignId, eligible}`) via `rules-starter`.
* `Cashback` (id, userId, transactionId unique, campaignId, amount `19,4`, status) — saga `CashbackSagaOrchestrator` (steps: `EVALUATE_RULE` → `CREDIT_WALLET` via `ADR-0049` journal `CoaCode.LIABILITY_PROMOTION`).
* Outbox `payu.promotion.cashback-credited.v1`.
* API `POST /v1/promotions/evaluate` idempotent.

## Rationale

A gives marketing agility + money safety vs B hardcode. Weighted 40% tech (drools/saga) 30% business (campaign speed) 30% team (reuse rules-starter).

## Consequences

**Positive**: campaign update = DRL push, cashback audited.
**Negative**: DRL needs marketing review — add `promotion-rules` CODEOWNERS.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Rules | `promotion/resources/rules/promotion/eligibility.drl` |
| 2 | Saga | `promotion/saga/CashbackSagaOrchestrator.java` |
| 3 | Tests | `PromotionRuleTest` + `CashbackSagaTest` |

**Verification**: `PromotionRuleTest` eligible true/false, cashback idempotent second `transactionId` returns same.

---
*Created for promotion-service — implementasi wajib refer ADR ini + ADR-0015.*
