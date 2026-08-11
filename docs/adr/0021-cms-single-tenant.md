# ADR-0021: CMS — PayU Only (Single-Tenant)

**Status**: Accepted  
**Date**: 2026-05-07  
**Deciders**: PayU Engineering Team

## Context

CMS (`cms-service`) untuk content management — banners, promos, alerts, popups. Saat ini single-tenant.

## Decision

**PayU only.** CMS content dikelola oleh PayU untuk semua project client.

### Current State
- `cms-service` with `ContentController` (CRUD banners/promos/alerts)
- `PublicContentController` for unauthenticated content retrieval
- Single tenant — no client-specific content separation

### Rationale
- CMS content bersifat platform-wide (system banners, maintenance alerts, promos global)
- Project client (TokoBapak, Nobar) tidak perlu custom CMS — mereka bisa manage promos via `promotion-service`
- No code changes needed — current implementation is sufficient

## Consequences

- ✅ No code changes — current cms-service works as-is
- ✅ Simple content management for platform operators
- ⚠️ If a client needs custom CMS in future, evaluate multi-tenant then
