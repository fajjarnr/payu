# ADR-0064: Gateway Service — Rate Limiting, 3scale & Edge Protection Standard

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Platform Engineering, Core Banking Engineering  
**Relates to**: ADR-0014 (API Management), ADR-0032 (WAF), ADR-0039 (BFF), ADR-0042 (ShedLock), Red Hat 3scale 2.11, Quarkus 3.30

---

## Context

`backend/gateway-service` (Quarkus 3.x Native, `adapter/filter/ApiAnalyticsFilter.java`, `application/service/PersistentAnalyticsService.java`, `src/main/resources/application.yaml`) sits in front of all `payu.*` services (`C4 transaction_svc` `docs/architecture/ARCHITECTURE.md:178`). Current:

* Analytics filter `ContainerResponseFilter` records `partnerId, path, method, status, duration` to `Data Grid/TimescaleDB` — 90d detailed, 1y aggregated `IMP-016`.
* No explicit **3scale policy chain** in repo (`APIcast` lua `apiKey/AppId+AppKey` or `OIDC JWT` + `leaky_bucket/fixed_window/connection_limiters` per Red Hat 3scale rate-limit policy) — only Spring `RateLimit` `api-commons` per `TransactionController`.
* No `edge limiting` aggregated across replicas (Red Hat edge-limit `lua-resty-limit-traffic`, external Redis). No `IP check` policy.

Industry: Red Hat 3scale hybrid-cloud (gateway policy execution async from manager, survives manager down, `MaaS` model), rate limiting at `application plan` vs `edge limit` across all apps, IP check L7 + L3 firewall, JWT `DPoP` (ADR-0062).

## Decision Drivers

* **Performance**: backend `100 concurrent` protected from 1000-apps `×10/min =10k/min` burst.
* **FAPI**: per-user limit via `JWT claim` liquid template (`remote_ip, header, JWT claim, path param`).
* **Cost**: 3scale quotas per team/project.

## Considered Options

### Option A — 3scale APIcast as edge (chosen, OpenShift native)

* **Pros**: Red Hat supported, hybrid async, policies in Admin Portal + Lua custom; external Redis for shared edge limit.
* **Cons**: need `APIManager` cluster-level (local `api-management` profile authless — `TODOS.md:78` notes).

### Option B — Spring Cloud Gateway in-process

* **Pros**: same stack.
* **Cons**: not Red Hat integrated, no hybrid cache.

## Decision

**Option A — 3scale APIcast as edge, Quarkus gateway as inner routing.**

1. **Authentication**: `API key` (`user_key` `x-data-threescale-name=user_keys`) for TokoBapak partner, `App_ID/App_Key` for internal tier, `OIDC JWT` (`payu-web/mobile` via RHBK 26.4 DPoP — ADR-0062) — choose per service via `authentication pattern` (Red Hat docs).
2. **Rate limits**: 
   * `application plan` per `Account`+`Application` (e.g. `free 10/min, paid 100/min`) + `mapping rules` per OpenAPI path.
   * `edge limiting` `leaky_bucket` (avg + burst) + `fixed_window` + `connection_limiters=100` at service level, **before `APIcast` policy** in chain, shared via external Redis (`Infinispan Hot Rod` reuse).
   * Per-user fairness: liquid template `{{ jwt.claim.sub }}` or `{{ headers["X-User-Id"] }}` → `10/min per user` inside `100/min per app` (Red Hat per-user limiting example).
3. **IP & maintenance**: `IP check` allowlist partner CIDR, `maintenance mode` policy for blue-green deploy.
4. **Quarkus inner gateway**: keep `ApiAnalyticsFilter` for `partnerId` analytics + `Quarkus JwtAuthFilter` fallback local validation (JWKS cache 5m via Vault) when 3scale introspection down; forward `X-Correlation-Id` for OTel tracing (ADR-0034).
5. **Operational**: `3scale Toolbox` `3scale_api.sh` automation for `Account/Service/Application plan` + `ActiveDocs` import; approval gate per `signup-flows` (self-service vs manual — default `payu-internal` self, `partner` maker-checker `ADR-0035`).

## Rationale

* 3scale Manager async + gateway cache = survives control-plane outage (Red Hat hybrid benefit).
* Edge limit `before APIcast` + Redis = single backend limit across `N` replicas — fixes 1000-apps burst problem.

## Consequences

**Positive**: backend throughput protected, per-user fairness, hybrid resilience.
**Negative**: need `APIManager` on cluster (`cluster-nkk8q` lab vs `OCP 4.20.29` main), Lua policy authoring.

## Implementation Notes

* Admin Portal: `Audience → Developer Portal → Domains & Access` remove `access_code` only after Vault `gateway.key` injected (ADR-0044).
* Portal `application.yaml` `portal.services[].url` `openapiPath: /v3/api-docs.yaml` (SpringDoc) aggregated via `ApiPortalService` already handles partial failure `BUG-BE-088`.
* Metric: `payu_gateway_edge_limited_total`, `resilience4j.ratelimiter` per route; `GatewaySchedulerLock` via `HotRodCacheClient.tryLock` `ADR-0042`.

---
*References: Red Hat 3scale 2025 docs (rate-limit, IP check, edge-limit, OAS 3.0, signup-flows, MaaS) + CodeGraph `ApiAnalyticsFilter.java`*
