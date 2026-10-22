# ADR-0074: Prod Mesh Gateway Behind 3scale APIcast

**Status**: Accepted  
**Date**: 2026-10-22  
**Deciders**: Platform Engineering, Principal Architect  
**Relates to**: ADR-0073 (Istio ingress gateway & shared domain ownership), ADR-0064 (gateway-service rate limiting, 3scale & edge protection), ADR-0037 (internal synchronous inter-service communication), MESH-004

---

## Context

ADR-0073 put the dev mesh gateway (`payu-mesh-gateway`, Gateway API, `ClusterIP`) behind the shared OpenShift router on `apps.fajjjar.my.id`, with one documented dev exception: the public dev Route `api-dev.apps.fajjjar.my.id` bypasses 3scale (3scale is not deployed in `payu-dev`). The prod rule never changed: **all public ingress stays APIcast-only** (`ARCHITECTURE.md` §7.3, "the public OpenShift `Route` for `gateway-service` has been removed to eliminate backdoor bypasses").

MESH-004 asked for the three remaining prod decisions: gateway placement + hostname, certificate automation, and the APIcast↔gateway relationship. This ADR records them. Manifests: `infrastructure/platform/mesh/ingress-prod/` (repo + schema-validated, not applied — namespace `payu` has no workloads yet).

## Decision Drivers

- **No APIcast bypass in prod** — API keys, rate plans and analytics must see every public call (security, ADR-0064).
- **No second public edge** — one owner for DNS/TLS per domain (ADR-0073); no second NLB (cost).
- **Mesh value preserved** — mTLS gateway→backend, Gateway API routing, mesh policy at one place (technical).
- **Operable TLS** — certificates must be automated, not hand-installed (reliability).

## Considered Options

### Option 1 (T1) — Mesh gateway **behind** APIcast (chosen)

Chain: `internet → shared router (TLS) → APIcast (key/quota/analytics) → mesh gateway ClusterIP (in-cluster HTTP) → HTTPRoute → gateway-service:8080 (namespace payu, mTLS)`.

- **Pros**: APIcast sees 100% of public traffic (rate limit/analytics cannot leak); mesh still adds mTLS + Gateway API routing + policy; no public Route/hostname/cert for the gateway at all; HTTPRoute matches on path only so the preserved APIcast Host header never 404s.
- **Cons**: two in-cluster hops (APIcast → gateway → service); the gateway namespace needs its own NetworkPolicy (ingress from `payu-api-management` only).

### Option 2 (T2) — APIcast calls `gateway-service` directly (no mesh gateway)

- **Pros**: one fewer hop; no gateway namespace to operate.
- **Cons**: loses the mesh routing/policy layer for the most important traffic in the platform; every path rule reverts to APIcast mapping templates; inconsistent with dev/UAT/preprod, which all terminate public traffic at a mesh gateway.

### Option 3 (T3) — Mesh gateway as a second public edge (own Route/hostname)

- **Pros**: pure mesh edge for prod.
- **Cons**: recreates the §7.3 backdoor (public path bypassing API keys/quotas/analytics); needs its own hostname + certificate on a domain APIcast already serves; directly contradicts the prod rule.

## Decision

**T1 — the prod mesh gateway sits behind APIcast as a ClusterIP backend.**

**Normative:**

1. **Placement**: namespace `payu-mesh-gateway-prod` (Gateway API `Gateway` `payu-public-gateway-prod`, `gatewayClassName: istio`, Service pinned `ClusterIP`). The namespace deliberately carries **no** `payu.fajjjar.my.id/ingress: shared` label, so no OpenShift router can ever admit a Route from it.
2. **No public surface**: the package contains **no** `Route`, no public hostname, no gateway certificate. The edge hostname stays `api-payu-apicast-production.apps.fajjjar.my.id` (3scale-owned). A guard test (`test_prod_ingress_gateway_sits_behind_apicast`) fails the build if a `Route` ever enters this package.
3. **TLS**: HTTP listener only. APIcast→gateway is cluster-internal plaintext; gateway→backend is mTLS (namespace `payu` is STRICT). No `reencrypt`/gateway certificate — it would buy nothing inside the cluster.
4. **Routing**: `HTTPRoute payu-gateway-prod` (namespace `payu`, no backendRef ReferenceGrant needed) routes `/api/v1` + `/v1/partner` to `gateway-service:8080`, sets `X-PayU-Environment: prod` + `X-PayU-Ingress: gateway-api`, and matches on **path only** (no `hostnames:` — APIcast preserves the original Host header).
5. **NetworkPolicy**: default-deny + `allow-apicast-to-gateway` (ingress 8080/15021 from `payu-api-management` only) + `allow-gateway-egress` (istiod 15012/15017, `payu` 8080/9090, DNS). The `payu` namespace needs the additive gateway-ingress allowance at promotion time (same shape as dev).
6. **APIcast relationship**: APIcast backend mapping points at `payu-public-gateway-istio.payu-mesh-gateway-prod.svc.cluster.local:8080` (not at `gateway-service` directly). Rate limit/analytics stay authoritative at APIcast; the `payu-header-forwarding` policy chain (`X-PayU-Partner-Id/Plan-Id/Request-Id`) is unchanged.

## Consequences

- Prod promotion must apply `ingress-prod/` **after** APIcast is live and **before** the first public call; order is enforced by the promotion runbook, not by automation yet.
- The `payu` overlay's `deny-all` + `allow-same-namespace` AuthorizationPolicy already covers gateway→backend (same `app.kubernetes.io/part-of: payu` label family); no new mesh policy needed for this hop.
- Dev/UAT/preprod keep their own gateway namespaces and Routes; nothing in this ADR changes their shape. Never copy the dev Route into UAT/prod.

## Verification

- `oc kustomize infrastructure/platform/mesh/ingress-prod` renders 6 objects (Namespace, Gateway, HTTPRoute, 3 NetworkPolicies), zero Routes.
- `oc apply --dry-run=client --validate=true` accepts the full render (server dry-run is N/A: target namespaces don't exist yet — same as `ingress-uat/`/`ingress-preprod/`).
- Guard test `test_prod_ingress_gateway_sits_behind_apicast` green (8/8 suite).

*Created 2026-10-22 — closes MESH-004 (design + manifests + ADR); live apply waits for prod bootstrap (PROD-READINESS).*
