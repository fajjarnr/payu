# ADR-0059: Product Catalog & Partner Product Governance Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Platform Engineering, Core Banking Engineering  
**Relates to**: ADR-0023 (MVP Scope), ADR-0035 (Partner)

---

## Context

`backend/product-catalog-service` (`8100`) partner product registry previously `Proposed P3`. Catalog drives `partner-service` dynamic product enablement + `api-portal-service` grouping.

## Decision

**Catalog as versioned source.**

* `Product` `version` + `tenant_id` RLS, `outbox` `payu.catalog.product-published.v1`.
* Partner product enablement via `partner-service` `Maker-Checker` (ADR-0035) referencing `catalogId`.

## Consequences

**Positive**: no orphan `productId` in partner onboarding.
**Negative**: catalog change needs saga to update partner cache.

---
*Promoted 2026-08-22 from P3 deferred to Accepted.*
