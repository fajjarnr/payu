# ADR-0056: Simulator Fidelity and Contract Testing Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Quality Engineering  
**Relates to**: ARCHITECTURE.md 13 (External Simulators), ADR-0025 (SNAP-BI), ADR-0043 (Camel), TODOS SIM-001

---

## Context

5 simulators (`backend/simulators/{bi-fast, dukcapil, qris, biller, va}`) provide lab test env for BI-FAST, Dukcapil, QRIS, billers, virtual accounts. Codegraph audit (`BiFastService.java:19`, `BillerService.java`, `VaSimulatorService.java`, `Qris SimulatorConfig.java:11`) shows:

* `bi-fast-simulator` has inquiry/transfer/webhook HMAC, latency 50-500ms, 5% random fail, sandbox accounts — closest to SNAP-BI contract.
* `dukcapil`/`qris` have README + test NIK/merchant fixtures, `SimlatorConfig` SmallRye `@ConfigMapping` `latency/failure-rate/webhook`.
* `biller`/`va` lack README, no explicit SNAP-BI headers, no idempotency dedup.

Gaps vs industry (Mountebank/WireMock fidelity, SNAP-BI 1.3 spec): no idempotency dedup on `referenceNumber`/`X-External-Id`, no deterministic `X-Simulate` header for chaos, QR not EMVCo TLV CRC16, HMAC symmetric vs real SNAP-BI RSA JWS, no `lab` profile guard + `NetworkPolicy` to prevent prod use.

## Decision Drivers

* **Contract parity**: SNAP-BI headers `X-TIMESTAMP/X-SIGNATURE/X-PARTNER-ID/X-EXTERNAL-ID` must match prod `gateway-service` `RouteRegistry`.
* **Idempotency**: duplicate `X-External-Id` returns original `Transfer` without second persist (BI-FAST idempotent).
* **Determinism**: tests need `X-Simulate: success|blocked|timeout|5xx` to drive `k6`/`chaos` (ADR-0024), not only `random 5%`.
* **Latency realism**: histogram `min/max` per simulator, not fixed `Thread.sleep`.
* **Safety**: simulators never run in prod — `lab` profile + `EgressNetworkPolicy` block.

## Considered Options

### Option A — Umbrella fidelity standard + per-simulator contract fix (chosen)

* **Pros**: one ADR for all 5, reuse Quarkus `quarkus-rest` + Panache `findByReference` + SmallRye `ConfigMapping` already in simulators; SNAP-BI contract aligned; `X-Simulate` header adds deterministic chaos without extra infra; ArchUnit `no simulator import in core-service` prevents leak.
* **Cons**: need per-simulator OpenAPI update — small effort.

### Option B — Per-simulator ADR (5 ADRs)

* **Pros**: detailed per domain.
* **Cons**: ADR bloat, same principles repeated — rejected per `principal-architect` docs system (link, not duplicate).

### Option C — Drop simulators, use WireMock external

* **Pros**: off-shelf.
* **Cons**: lose Panache stateful `BankAccount`/`Transfer` fixtures + webhook HMAC already working — rejected.

## Decision

**Single fidelity standard for all simulators, implement per-simulator fixes.**

* **Contract**: each simulator exposes OpenAPI aligned to SNAP-BI (where applicable): `POST /api/v1/inquiry` `bankCode+accountNumber`, `POST /transfer` with `X-External-Id` (BiFast) / `X-Idempotency-Key` (VA/QRIS) — `referenceNumber` unique key; duplicate returns `200` with original entity (check `Transfer.findByReference` before `persist`).
* **Headers**: support `X-Simulate` enum (`success`, `blocked`, `timeout`, `rate-limit`, `5xx`) — if present, override random failure/latency for deterministic `k6`/`Cerberus`.
* **Latency**: keep `simulator.latency.min/max` + `failure-rate` via `SimulatorConfig` `@ConfigMapping(prefix="simulator")` `@WithDefault` — verified in `quarkusio/quarkus` docs.
* **Webhook**: keep `WebhookDispatcher.java:38` HMAC `timestamp+'\n'+body` with `X-Signature`/`X-Timestamp`, `retryCount/delayMs`; document `lab-only HMAC` vs prod RSA JWS in `ADR-0025`; add `NetworkPolicy` `deny simulator → prod`.
* **QRIS**: `qris-sim` generate EMVCo TLV `00-63` with CRC16 X25 (like `ADR-0052`), not random base64; personal QR `qrCodeHash` check same as `account-service`.
* **Guard**: `application-prod.yaml` `quarkus.profile.prod` disables simulators (`%prod.quarkus.http.port=-1`), `catalog-info.yaml` `annotations: backstage.io/techdocs-ref` points to `docs/architecture/simulators.md`.

## Rationale

A gives contract parity with minimal new ADR, reuses existing Quarkus stack (verified via Context7 `quarkusio/quarkus` 3.30 ConfigMapping/Panache), adds deterministic `X-Simulate` needed for `ADR-0024` chaos. Weighted 40% tech (idempotency/latency) 30% business (OJK SNAP-BI audit) 30% team (reuse 5 sims).

## Consequences

**Positive**: `k6` deterministic `X-Simulate: timeout` tests, prod parity for gateway `RouteRegistry`, no simulator leak to prod.
**Negative**: per-simulator OpenAPI diff vs SNAP-BI must be maintained — mitigate via `Buf` lint on OpenAPI.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | ADR | this file |
| 2 | Backlog | `TODOS.md` SIM-001 ref this ADR |
| 3 | Idempotency | `BiFastService.transfer()` check `findByReference(referenceNumber)` before `createTransfer`; same for `Va/Biller/Qris` |
| 4 | Header | `BiFastResource/QrisResource` read `X-Simulate` header → override `shouldSimulateFailure()` |
| 5 | QR | `qris-sim` `QrisGenerator` EMVCo TLV CRC16 |
| 6 | Guard | `application-prod.yaml` disable port, `NetworkPolicy` |
| 7 | Tests | `SimulatorContractTest` OpenAPI SNAP-BI header + idempotency dedup |

**Verification**: `BiFastServiceTest` duplicate `referenceNumber` returns same ref `200`, `curl -H X-Simulate:timeout` returns `TIMEOUT` `5s`, `ArchUnit` `no simulator` in `**/wallet/**`.

---
*Created for SIM-001 — implementasi wajib refer ADR ini + ADR-0025.*
