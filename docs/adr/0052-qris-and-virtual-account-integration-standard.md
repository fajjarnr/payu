# ADR-0052: QRIS and Virtual Account Integration Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Compliance  
**Relates to**: ADR-0025 (SNAP-BI), FE-STUB-003, ARCHITECTURE.md 3.2.3/3.2.22

---

## Context

`ARCHITECTURE.md:13` simulators `qris-sim` `va-sim` exist, but no ADR for QR intent. `qris/page.tsx:25` simulates `setTimeout` toast, no EMVCo 4.3 TLV decode, no `GET /accounts/{id}/qris` personal QR (`AccountEntity.qrCodeHash`), no SNAP-BI `POST /qris` via gateway. OJK requires QRIS CPM/CPM validation + VA idempotency.

## Decision Drivers

* **EMVCo compliance**: TLV decode, tag 00/01/26/30/54/59/63, CRC16 X25 checksum.
* **SNAP-BI**: `POST /snap-bi/qris` with `X-Idempotency-Key`, `X-External-Id`, `X-Timestamp`.
* **Personal QR**: hash stored, not raw QR in DB.
* **Idempotency**: VA/Qris payment replay safe.

## Considered Options

### Option A — Decode TLV in transaction-service + gateway SNAP-BI route (chosen)

* **Pros**: reuse `gateway-service` `RouteRegistry`, `transaction-service` already has BI-FAST, QRIS same money path, `qrCodeHash` stays in `account-service`.
* **Cons**: TLV parser needed — small lib `emvco-java` or hand parser 80 LOC.

### Option B — Separate qris-service

* **Pros**: isolated.
* **Cons**: extra service for <1 endpoint — overkill at current scale.

## Decision

**TLV parser in transaction-service, gateway proxies SNAP-BI, personal QR in account-service.**

* `QrisParser` (domain) — parse `String qr` → `QrisData {merchantId, amount, currency, checksumValid}` — validate `CRC16("1104...6304")` per EMVCo; tag 26 Merchant AccountInfo, 54 Transaction Amount, 59 Merchant Name.
* `AccountEntity.qrCodeHash` stores `SHA256(qr)` — `GET /v1/accounts/{id}/qris` returns hash + `GET /v1/qris/decode?qr=` for UI preview (no DB store raw).
* `POST /v1/qris/pay` → `transaction-service` validates checksum, holds `wallet` via `JournalEntry`, calls gateway `POST /snap-bi/qris` with `X-Idempotency-Key=transactionId`, outbox `payu.transaction.qris-initiated.v1`.
* Simulators `qris-sim`/`va-sim` in lab; prod uses real SNAP-BI via `integration-service` Camel (ADR-0043).
* Tests: `QrisParserTest` `00-63` tags, `WalletJournalTest` QR amount.

## Rationale

A reuses existing money path (wallet + saga) and gateway, minimal new infra vs B. Weighted 40% tech (TLV CRC) 30% business (OJK QRIS) 30% team (reuse transaction-service).

## Consequences

**Positive**: EMVCo valid QR only, SNAP-BI idempotent, personal QR hash privacy.
**Negative**: hand parser must track EMVCo spec updates — add `emvco-spec` link in code comment.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Parser | `transaction/domain/QrisParser.java` CRC16 |
| 2 | Account | `account/entity/AccountEntity.qrCodeHash` already exists |
| 3 | API | `transaction/adapter/web/QrisController.java` `POST /qris/pay` |
| 4 | Gateway | `gateway/adapter/RouteRegistry.java` add `qris` route |
| 5 | Tests | `QrisParserTest` + `QrisPayIntegrationTest` |

**Verification**: `QrisParserTest` CRC pass/fail green, `POST /qris/pay` with bad CRC 400 `QRIS_001`, gateway route returns 200 in lab.

---
*Created for FE-STUB-003 — implementasi wajib refer ADR-0025 + ADR ini.*
