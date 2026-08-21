# ADR-0058: Backoffice RBAC & Admin Audit Trail Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Platform Engineering, Risk & Compliance  
**Relates to**: ADR-0033 (RLS), ADR-0034 (Observability), ADR-0040 (Blind Index)

---

## Context

`backend/backoffice-service` internal admin (Quarkus, `BlindIndexService` via `security-starter`) previously `Proposed P3`. Need RBAC `PARTNER_MAKER/PARTNER_CHECKER` already in `ADR-0035`, but backoffice adds `ADMIN_VIEWER/ADMIN_OPERATOR/ADMIN_AUDITOR` + `maker≠checker DB CHECK` for `freeze/close` account, `refund` dispute.

## Decision

**RBAC Enum + `DataAccessAudit` every admin READ/UPDATE (ADR-0063) + RLS tenant filter.**

* Admin action requires `preAuthorize` + audit `purpose` field (`DataAccessAudit.java: operationType, purpose`).
* All admin queries `FORCE RLS` per `ADR-0033`.

## Consequences

**Positive**: admin leak audit-trail defensible.
**Negative**: extra `AccessDeniedException` handling in `ComplianceAuditService` fallback.

---
*Promoted 2026-08-22 from P3 deferred to Accepted.*
