# ADR-0051: Support Ticket and FAQ Lifecycle Standard

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Support Ops  
**Relates to**: ADR-0020 (Support Centralized), BE-SUPP-001, ADR-0041 (Outbox), ADR-0033 (RLS)

---

## Context

`ADR-0020` (31 lines) says "PayU handles all" but only defines training management. `support-service` has `Agent/Training` only; no `POST /support/tickets` or `GET /faqs` — `support/page.tsx` static. `BE-SUPP-001` blocks MVP. Industry (ITIL 4, OJK consumer protection) requires ticket lifecycle, SLA, audit, and FAQ CMS with versioning.

## Decision Drivers

* **Lifecycle**: OPEN→IN_PROGRESS→WAITING_CUSTOMER→RESOLVED→CLOSED, with `assignedTo` agent.
* **SLA**: 24h first response, 72h resolution — metric `support_sla_breach`.
* **Idempotency**: `X-Idempotency-Key` prevents double ticket on retry.
* **Multi-tenant isolation**: RLS `tenant_id` per `ADR-0033`.
* **Audit**: outbox `payu.support.ticket-*` + `.dlq`.

## Considered Options

### Option A — Support tickets in support-service + FAQ in CMS (chosen)

* **Pros**: ticket domain stays with support-service (agent assignment), FAQ is content (CMS `cms-service` already has banners/promos), reuse `outbox-starter` + `saga-starter`.
* **Cons**: two services for support — mitigated by clear bounded context.

### Option B — All in support-service

* **Pros**: single service.
* **Cons**: CMS already owns content versioning — duplicate.

## Decision

**Tickets in support-service, FAQs in cms-service.**

* `support_tickets` table (id UUID, tenantId, userId, subject, description encrypted AES-GCM, category, priority, status enum, assignedTo, slaDueAt, createdAt) — `FORCE RLS`, `DECIMAL` not needed (no money).
* API `POST /api/v1/support/tickets` (idempotent), `GET /api/v1/support/tickets?status=`, `PATCH /:id/status`, `POST /:id/messages` — hexagonal `TicketUseCase` → `TicketPersistencePort`.
* Outbox `payu.support.ticket-created.v1` (CloudEvents), `ticket-assigned`, `ticket-resolved` → `notification-service` email.
* FAQ: `cms_service` `faqs` table (question, answer, category, status, version) — `GET /faqs?category=` cached via `cache-starter`.
* UI `support/page.tsx` now calls `SupportService.createTicket/getFAQs`.

## Rationale

A respects bounded contexts: support = workflow, CMS = content. Reuses `ADR-0020` centralized but adds lifecycle missing. Weighted 40% tech (RLS/outbox) 30% business (OJK SLA) 30% team (reuse CMS).

## Consequences

**Positive**: SLA trackable, audit via outbox, FAQ versioned.
**Negative**: cross-service `ticket-resolved → notification` needs DLQ handling.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Entity | `support/entity/SupportTicketEntity.java` RLS + encrypt |
| 2 | API | `support/adapter/web/SupportTicketController.java` |
| 3 | Outbox | `TicketEventPublisher` `payu.support.*.v1` |
| 4 | FAQ | `cms/.../FaqEntity.java` |
| 5 | Tests | `SupportTicketServiceTest` lifecycle + idempotency |

**Verification**: `SupportTicketTest` `OPEN→CLOSED` green, `GET /faqs` cached, `SELECT` RLS returns 0 for other tenant.

---
*Created for BE-SUPP-001 — implementasi wajib refer ADR-0020 + ADR ini.*
