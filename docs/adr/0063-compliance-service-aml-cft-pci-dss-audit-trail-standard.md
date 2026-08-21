# ADR-0063: Compliance Service — AML/CFT, PCI-DSS & Audit-Trail Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Risk & Compliance, Core Banking Engineering, Platform Engineering  
**Relates to**: ADR-0010 (Security), ADR-0030 (Velocity/AML), ADR-0032 (WAF/SIEM), ADR-0034 (Observability), ADR-0040 (Encryption), PCI-DSS Req 10, UU PDP, FATF 2025

---

## Context

`backend/compliance-service` (`domain/model/AuditReport.java`, `ComplianceCheck.java`, `DataAccessAudit.java`, `application/service/ComplianceAuditService.java:19`) saat ini:

* Models: `AuditReport` (transactionId, merchantId, `ComplianceStandard PCI_DSS/OJK/AML/CFT/GDPR`, `checks[]`, `overallStatus PASS/FAIL/WARNING/NOT_APPLICABLE`, `version`), `ComplianceCheck` pure domain (JPA embed only `ComplianceCheckEmbeddable` per ponytail), `DataAccessAudit` (userId, service, resourceType/id, `DataOperationType READ/UPDATE/DELETE/EXPORT/SEARCH`, purpose, ip, success, version).
* Service: `ComplianceAuditService.createAuditReport` aggregates `FAIL/WARNING` → `overallStatus`, `@CircuitBreaker @Retry` per `resilience-starter`, `DataAccessAuditService` log every READ/EXPORT.
* Gap: belum ada **AML transaction monitoring (TM)** signal ingestion, belum **PCI-DSS Req 10** structured append-only immutable log + daily review, belum **maker-checker** untuk compliance decision, belum **DORA/HKMA** maturity framework alignment.

Industry 2025-2026: SPD payment security (data/trust/observability boundaries, CDE scope via tokenization, mTLS+OAuth2, forensic-ready structured immutable logs, kill switch), Everest AML 4-stage framework (rule → AI hybrid, 90%+ false positive, fragmented data), HKMA 2024 TM insights (data lineage + reconciliation daily, governance committee, KPI falsePositive/STR, AI principles), Gruv PCI-Req10 audit-trail (append-only, dual approval for beneficiary/amount/routing, exception linkage), BCG compliance-by-design.

## Decision Drivers

* **OJK/BI + PCI-DSS Req 10**: log `access` attributable, protected from destruction, reviewed daily — tanpa itu gagal audit QSA.
* **UU PDP**: purpose + DataAccessAudit traceable.
* **Risk**: `compliance-service` adalah trust boundary sendiri — setiap provider baru expand scope jika tidak segmented.

## Considered Options

### Option A — Harden in-place: append-only audit + dual approval + TM pipeline (chosen)

* **Pros**: reuse `DataAccessAudit` + `AuditReport`; PCI `immutable JSONB` + `WORM` retention; align HKMA TM data lineage.
* **Cons**: perlu `pg_audit` + `WORM` bucket.

### Option B — Outsource GRC (RegTechONE)

* **Pros**: workflow orchestration siap.
* **Cons**: vendor lock-in, data residency BI issue.

## Decision

**Option A.**

1. **Audit trail**: `ledger_entries`-style **append-only** — `audit_reports` + `data_access_audits` `INSERT` only, `REVOKE UPDATE,DELETE`, `version` optimistic, `idempotency` `auditId`. Structured JSON: `traceId, businessId (account/transaction), actor, action, purpose, result, tenant_id` — masked PII. Immutable via `TimescaleDB` or `WORM S3 Glacier` 1y hot (3m immediate) + 7y cold per PCI/SOX.
2. **Boundaries**: CDE tokenization boundary explicit (`gateway → compliance` mTLS + scoped OAuth2); micro-segmentation per service inside CDE; 3scale edge limit covers gateway, but `compliance-service` sendiri enforce `leaky_bucket` per `merchantId` + `fixed_window` per `tenant`.
3. **Dual approval**: beneficiary/amount/routing threshold (>50M IDR) → `maker != checker` `DB CHECK`, `ComplianceCheck` second reviewer `PASS` before `overallStatus=PASS`.
4. **AML TM pipeline**: ingest `payu.transaction.*.v1` + `payu.account.*.v1` via Kafka `outbox` → scenario engine (rules first, ML scorecard `VelocityGuard` `ADR-0030`) → KPI `alertVolume, falsePositiveRate, STR rate` reported quarterly to AML committee (HKMA) — periodic review 12-24m + trigger review (surge, new typology).
5. **Observability**: `traceId` propagation OpenTelemetry (ADR-0034), daily `reviewed_logs` evidence, exception `taxonomy` linkage `initiation→approval→execution→reconciliation` (Gruv), dashboard Wazuh SIEM (ADR-0032).

## Rationale

* HKMA: `data lineage + reconciliation` catches mapping bug; SPD: `observability at architecture time`.
* Cost: HKMA maturity 4-stage allows hybrid rule+ML, not full AI replacement — matches `Everest` `POC fail at scale` warning.

## Consequences

**Positive**: QSA defensible, single disputed-payout reconstructible e2e from one `traceId`.
**Negative**: storage `1y` cost, dual approval adds latency for high-value.

## Implementation Notes

* Flyway `V...__append_only_audit.sql`: `REVOKE UPDATE,DELETE`, `ENABLE ROW LEVEL SECURITY` tenant.
* Spring `AuditAspect` already — ensure `DataAccessAuditService` called on every `READ/EXPORT`.
* Contract: `POST /v1/compliance/audit-reports` returns `201` with `overallStatus`, `GET /v1/compliance/data-access?purpose=` audit export.

---
*References: web 2026-08-22 (SPD, Everest, HKMA, Gruv, BCG) + CodeGraph `ComplianceAuditService.java:19`, `DataAccessAudit.java`*
