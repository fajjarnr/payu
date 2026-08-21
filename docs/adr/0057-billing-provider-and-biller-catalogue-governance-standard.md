# ADR-0057: Billing Provider & Biller Catalogue Governance Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Core Banking Engineering, Platform Engineering  
**Relates to**: ADR-0041 (Outbox), ADR-0043 (Camel), biller-simulator

---

## Context

`backend/billing-service` (`domain/model/BillPayment.java`, `BillerType.java: PLN/PDAM/TELKOMSEL/GOPAY/OVO/DANA...`) + `biller-simulator` handles 23 biller types. Previously `Proposed P3` deferred per `ADR-0023` MVP, now in scope post 1.13.x. No contract pinning between `BillerType` enum and simulator; version drift risk.

## Decision

**Biller Catalogue is code as governance.**

* `BillerType` enum top-level file (`code, category, displayName`) single source; simulator loads same JSON catalogue.
* `BillPayment` `version` optimistic + `idempotencyKey UNIQUE(tenant_id, key)` + `eventPublished` flag for outbox `payu.billing.bill-payment.v1` + `.dlq`.
* Provider adapter interface per `BillerType` (port `BillerProviderPort`) + `Resilience4j` per-rail + `walletServicePort.reserve/commit/release` same pattern as `ADR-0060`.

## Consequences

**Positive**: no `PLN` vs `PLN_PASCABAYAR` drift.
**Negative**: enum change requires Flyway + simulator redeploy.

---
*Promoted 2026-08-22 from P3 deferred to Accepted — references `billing-service` + `biller-simulator`.*
