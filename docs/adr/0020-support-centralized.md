# ADR-0020: Support — PayU Handles All (Single-Tenant)

**Status**: Accepted  
**Date**: 2026-05-07  
**Deciders**: PayU Engineering Team

## Context

Support/helpdesk ticketing system architecture decision.

## Decision

**PayU handles all support centrally.** Single support system for all project clients.

### Current State
- `support-service` handles training management (agents, modules, trainings)
- No ticketing system yet — to be added in future iteration

### Future Extension
- `POST /api/v1/support/tickets` — create ticket
- `GET /api/v1/support/tickets` — list tickets (filterable by client/status)
- `PATCH /api/v1/support/tickets/{id}` — update ticket status
- Backoffice agents handle all tickets across clients

## Consequences

- ✅ Simple architecture — no multi-tenant complexity
- ✅ Single support dashboard for all clients
- ✅ No code changes needed now — current support-service is sufficient for training
- ⚠️ Ticketing system needs to be built when support volume grows
- ⚠️ PayU support team needs to scale with client growth
