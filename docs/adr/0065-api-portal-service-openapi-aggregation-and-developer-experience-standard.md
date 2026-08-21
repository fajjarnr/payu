# ADR-0065: API Portal Service — OpenAPI Aggregation & Developer Experience Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Platform Engineering, DX Engineering, Core Banking Engineering  
**Relates to**: ADR-0014 (API Management), ADR-0037 (gRPC), ADR-0039 (BFF), ADR-0056 (Simulator Fidelity), SpringDoc 2025, Quarkus 3.30, Red Hat 3scale ActiveDocs

---

## Context

`backend/api-portal-service` (Quarkus, `application/service/ApiPortalService.java:27`, `config/PortalConfig.java:8`, `adapter/web/ApiPortalResource.java`) aggregates `portal.services[].openapiPath: /v3/api-docs.yaml` (SpringDoc `springdoc-openapi-starter-webmvc-ui`) via `HttpClient` `VirtualThread` + `ConcurrentHashMap specCache` + `CacheConfig ttl` (`Duration.parse`). Current `BUG-BE-088` fix: per-service error tracking with partial result (`specCache` + `failedServices`).

Gap: no **Groups** segregation (`GroupedOpenApi` `public vs admin`), no **contract testing** (`archunit-starter` is there but `TEST-GAP` in `docs/roadmap/SERVICES.md:12` says 6/8 core services lack integration), no **simulator fidelity** linkage to OpenAPI (Qris/VA/Bill Biller specs drift). 3scale ActiveDocs expects `OAS 3.0` (now default `swagger-ui` webpack) but repo templates still OAS 2.0 per Red Hat docs.

## Decision Drivers

* **DX**: partner TokoBapak needs self-service `try-it` without copying `user_key`.
* **Contract drift**: Biller `PLN/PDAM` types change without breaking `billing-service` client.
* **Governance**: `EVENT_CATALOG.md` already 107 Kafka topics (65+42 DLQ) — API catalog must mirror.

## Considered Options

### Option A — Keep Quarkus portal + SpringDoc aggregated via 3scale ActiveDocs (chosen)

* **Pros**: reuse existing `ApiPortalService` partial-failure logic; 3scale `ActiveDocs x-data-threescale-name` auto-fill `user_keys/app_ids/app_keys`; single `portal.services` YAML source.
* **Cons**: need to upgrade templates OAS 2.0→3.0 manually.

### Option B — Backstage.io Developer Hub directly

* **Pros**: Catalog sync `DX-CATALOG-001` already ghost+5 service.
* **Cons**: heavier for lab.

## Decision

**Option A.**

1. **Grouping**: Spring Boot service `@Configuration ApiDocumentationConfig` `GroupedOpenApi` beans (`public` `pathsToMatch /api/v1/**` + `admin` `pathsToMatch /api/admin/**`) `displayName` per SpringDoc Context7; portal aggregates per `group` (not just host) — `portal.services[].name` + `openapiPath: /v3/api-docs/public`.
2. **Aggregation**: keep `ConcurrentHashMap` + `TTL` + `VirtualThread` fetching + `shouldRefreshCache()` + `failedServices` list (partial result) — add `stale-while-revalidate` `ETag` header for UI.
3. **3scale ActiveDocs**: import OAS `3.0` JSON via Admin `Services→ActiveDocs` (`swagger.json`), liquid tag `{% active_docs version: "3.0" %}`, `x-data-threescale-name: user_keys` auto-fill for `API key` auth, `app_ids/app_keys` for `App_ID` pair, OAuth2 client-credentials `ActiveDocs` for token endpoint (Red Hat example Echo API). `oas proxy` for CORS, host must be domain not IP.
4. **Contract & simulator fidelity** (ADR-0056): `spring-cloud-contract-maven-plugin` + `springdoc-openapi` contract test per service (`ContractVerifierBase` like `architecture` tests) → `api-commons` Pact in CI (`PARTNER-PROD-010` gate); biller-simulator spec pinned to `billing-service` `BillerType` enum (`ADR-0057`).
5. **DX**: `Creating Developer Portal` flow — remove `access_code` (Domains & Access) only after `Audience→Accounts→Usage Rules` approve vs self-service chosen; signup `credit card gateway` placeholder disabled for lab; marketing `Liquid tags` for partner tier badge.

## Rationale

* Portal already handles `5s` `HttpClient` timeout + per-service `UNKNOWN` health (`/q/health`) — best-effort partial is correct for FAPI (avoid blocking portal when 1 service down).
* SpringDoc `GroupedOpenApi` is Context7 best practice for multi-surface APIs without extra gateway.

## Consequences

**Positive**: single portal = single `user_key` try-it, OAS drift caught by contract CI.
**Negative**: OAS 3.0 template upgrade manual step.

## Implementation Notes

* `portal.services` YAML (already) + `application.yaml` `portal.cache.ttl: PT5M`.
* Pom: `org.springdoc:springdoc-openapi-starter-webmvc-ui` (Spring Boot 4) + `@Tag @Operation @ApiResponse` on every `@RestController` (like `UserController` Context7 example).
* Test: `ApiPortalServiceTest` must assert `refreshCache` partial when 1 of N services returns `500`.
* Link `docs/INDEX.md` → portal `AggregatedOpenApiResponse` `v1` endpoint.

---
*References: Red Hat 3scale 2025 (ActiveDocs OAS 3.0, x-data-threescale-name, portal creation, signup-flows) + SpringDoc Context7 (GroupedOpenApi) + CodeGraph `ApiPortalService.java:27`*
