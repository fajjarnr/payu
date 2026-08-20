# ADR-0050: FX Provider and Rate Governance Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Treasury  
**Relates to**: PROD-002, ADR-0022 (Money), ADR-0041 (Outbox), ADR-0043 (Camel)

---

## Context

`fx-service` provides currency conversion for lending/investment and TokoBapak cross-border. `PROD-002` requires approved FX provider URL/credential + live evidence, but no ADR defines provider selection, rate source (BI middle vs commercial vendor), precision, caching, or fallback. `AGENTS.md:1` mandates `BigDecimal HALF_EVEN` `DECIMAL(19,4)`. Rates change intraday; stale rate causes ledger mismatch (IDR 1M * 0.01% = 100 IDR error).

## Decision Drivers

* **Accuracy**: `BigDecimal` HALF_EVEN, no float; `DECIMAL(19,4)` DB.
* **Freshness**: intraday TTL <5m, BI rate fallback.
* **Audit**: every conversion logs provider, rate, timestamp, `X-Idempotency-Key`.
* **Resilience**: provider timeout 1s, retry idempotent, circuit breaker.
* **Compliance**: OJK FX transaction reporting.

## Considered Options

### Option A — Single approved provider + BI fallback + cache (chosen)

* **Pros**: one contract, BI fallback deterministic, `cache-starter` Caffeine+Hot Rod TTL 5m, `saga-starter` idempotent replay safe, audit simple.
* **Cons**: single point of failure if provider down > BI lag.

### Option B — Multi-provider aggregator (best rate)

* **Pros**: best rate.
* **Cons**: complex reconciliation, provider-shopping audit risk, latency — rejected for MVP.

### Option C — Static table, daily batch

* **Pros**: simple.
* **Cons**: stale intraday — rejected for lending pricing.

## Decision

**Single approved FX provider with BI fallback and governed cache.**

* `fx-service` `FxRate` (pair, rate `BigDecimal 19,4`, provider, fetchedAt, ttl 5m) — `CoaCode` agnostic.
* `FxProviderPort` (hexagonal) — adapter `BiFxAdapter` (primary) + `BiMiddleRateAdapter` (fallback). Config `payu.fx.provider-url` via Vault (ADR-0044), not code.
* `GET /fx/rates/{pair}` → cache read `cache-starter` (Hot Rod + Caffeine L2), miss → provider `RestClient` (Spring Boot 4.1 `RestClient`, deadline 1s, retry 1), `BigDecimal` math `HALF_EVEN`.
* `POST /fx/convert` idempotent `X-Idempotency-Key`, persist `fx_conversions` (pair, rate, amount, result, provider, idempotencyKey unique) + outbox `payu.fx.rate-fetched.v1`.
* Fallback: provider timeout → BI middle rate + `WARN` + metric `fx_fallback_total`.
* No float in conversion: `amount.multiply(rate).setScale(4, HALF_EVEN)`.

## Rationale

Maps to drivers: A gives audit-simple single provider with BI fallback for freshness, cache for <5m TTL without multi-provider complexity. Weighted 40% tech (cache + idempotency) 30% business (OJK audit) 30% team (single adapter).

## Consequences

**Positive**: live evidence per `PROD-002` (rate + provider logged), p95 `<50ms` cached, fallback prevents outage.
**Negative**: provider lock-in — mitigated by port, can add aggregator later as `TRIAL`.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Port | `fx/domain/port/out/FxProviderPort.java` |
| 2 | Adapter | `fx/adapter/provider/BiFxAdapter.java` `RestClient` |
| 3 | Cache | `cache-starter` `payu-fx` cache TTL 5m |
| 4 | Entity | `fx_conversions` `DECIMAL(19,4)` `idempotencyKey` unique |
| 5 | Tests | `FxServiceTest` HALF_EVEN `0.1*0.2=0.02`, fallback test |

**Verification**: `FxServiceTest` green, `k6` p95 cached <50ms, audit `SELECT * FROM fx_conversions` shows provider.

---
*Created for PROD-002 — implementasi wajib refer ADR ini.*
