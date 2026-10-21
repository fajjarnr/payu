# ADR-0073: Istio Ingress Gateway & Shared Domain Ownership

**Status**: Accepted  
**Date**: 2026-10-21  
**Deciders**: Platform Engineering, Principal Architect  
**Relates to**: ADR-0072 (OSSM 3.4 Sail Operator dev rollout), ADR-0064 (gateway-service rate limiting, 3scale & edge protection), ADR-0069 (OpenShift 4.22 platform standard)

---

## Context

The platform has one shared public domain: `apps.fajjjar.my.id`. It is served by the OpenShift IngressController `shared-ingress` (`infrastructure/foundation/cluster-config/ingress/shared.yaml`), which publishes an **AWS NLB** (`endpointPublishingStrategy: LoadBalancerService`, provider `type: AWS`, `networkLoadBalancer: {}`) and terminates TLS with the cert-manager certificate `shared-ingress-cert` (Let's Encrypt, `CN=apps.fajjjar.my.id`, SAN `*.apps.fajjjar.my.id`). The `IngressController` admits only namespaces labelled `payu.fajjjar.my.id/ingress: shared`, and DNS is **unmanaged** by the operator (`dnsManagementPolicy: Unmanaged`) — the zone `apps.fajjjar.my.id` in Route53 points at the NLB.

With the mesh live in `payu-dev` (ADR-0072), the open question was: can the Istio ingress gateway *use* that domain, and if so, who owns it end-to-end?

The answer is constrained by the DNS and certificate topology, not by Istio:

- The **DNS** for `apps.fajjjar.my.id` resolves to the shared NLB (`52.74.162.171`). Anything else that wants that hostname must be behind that NLB.
- The **certificate** for the shared domain is issued for the router (`shared-ingress-cert`) and covers exactly **one** label under the domain: `*.apps.fajjjar.my.id` matches `api-dev.apps.fajjjar.my.id` but **not** `api.dev.apps.fajjjar.my.id`.
- The **OpenShift Route** model binds a Route to a Service **in the same namespace**, and the shared IngressController only serves namespaces carrying the `payu.fajjjar.my.id/ingress: shared` label.

The legacy assets in `infrastructure/platform/mesh/base/` (`gateway.yaml`, `gateway-api/`) are Maistra-era and were never applied: they point at a non-existent `istio-ingressgateway.istio-system.svc.cluster.local` service and use hostnames under `payu.fajjjar.my.id`, not the shared domain.

## Decision Drivers

- **One owner for the domain** — DNS, TLS and routing must not be split between two ingress tiers (technical).
- **No second load balancer** — the shared NLB already costs money and already fronts the domain; a mesh gateway with its own `LoadBalancer` Service would provision a second NLB (cost).
- **No APIcast bypass in prod** — ADR-0064 / `ARCHITECTURE.md` §7.3 removed the public Route to `gateway-service` precisely because it bypassed API keys, rate limits and analytics (security).
- **Do not disturb the running dev environment** — `payu-dev` and `istio-system` must not be relabelled into the shared IngressController, or the live web-app / analytics Routes would move routers (reliability).
- **Supported operator path** — the gateway must be created by a mechanism OSSM 3.4 actually reconciles (technical).

## Considered Options

### Option 1 (T1) — Istio gateway **behind** the shared router (chosen)

The OpenShift router keeps the domain: it terminates TLS with `shared-ingress-cert` and proxies plain HTTP to a `ClusterIP` Istio gateway Service in a dedicated namespace. The Istio gateway is a **backend**, not the domain owner.

- **Pros**: one owner for DNS/TLS/DNS-NLB; no second NLB; the mesh gateway still adds mesh-native routing (Gateway API `HTTPRoute`), mTLS to the backend and a single place for mesh policy; the shared router keeps its existing certificate automation; prod keeps APIcast in front.
- **Cons**: two hops (router → gateway → service); the gateway namespace needs its own NetworkPolicy; TLS terminates one layer earlier than a pure-mesh edge would.

### Option 2 (T2) — Istio gateway **owns** the domain

The gateway gets its own `LoadBalancer` Service, its own DNS record and its own certificate for `apps.fajjjar.my.id`.

- **Pros**: a pure mesh edge, single hop, full control over TLS and headers.
- **Cons**: a second AWS NLB (cost) and a second DNS record on a zone the platform does not manage through the operator; the wildcard certificate would have to be re-issued for the gateway (a cert-manager `Certificate` for the same domain already owned by the router — conflicting ownership of the same SAN); the shared IngressController's `shared-ingress-cert` and the gateway's certificate would both claim `*.apps.fajjjar.my.id`.

### Option 3 (T3) — No Istio gateway at all

Expose the dev API through the shared router with a plain Route to `gateway-service`, or keep the mesh purely internal.

- **Pros**: fewest moving parts; no gateway namespace, no NetworkPolicy work.
- **Cons**: the router becomes the policy point — every path and header rule has to be expressed in Route/HAProxy terms instead of Gateway API; no mesh-native routing layer; and a direct public Route to `gateway-service` is exactly the backdoor §7.3 removed (a dev-scoped instance of that bypass would be created without any mesh value).

## Decision

**T1 — the shared OpenShift router owns `apps.fajjjar.my.id`; the Istio ingress gateway is a backend behind it.** The mesh gateway is created with the **Gateway API** (`gateway.networking.k8s.io/v1` `Gateway`, `gatewayClassName: istio`) in a dedicated namespace `payu-mesh-gateway`, with the generated Service pinned to `ClusterIP` (annotation `networking.istio.io/service-type: ClusterIP`).

**Normative:**

1. **Domain ownership**: DNS (Route53 → shared NLB), TLS termination (OpenShift router, `shared-ingress-cert`) and routing (OpenShift `Route`) stay with the platform ingress tier. An Istio gateway may only be a **backend** of that tier. The Gateway API is the supported way to create one on OSSM 3.4 — the classic `spec.values.gateways.istio-ingressgateway` always installs into `istio-system`, which cannot be labelled for the shared IngressController without moving live Routes.
2. **Namespace**: the gateway lives in `payu-mesh-gateway`, which carries the label `payu.fajjjar.my.id/ingress: shared`. `payu-dev` and `istio-system` are **never** labelled for the shared IngressController.
3. **Service type**: the gateway Service stays `ClusterIP`. No second NLB, no extra cloud cost, no DNS change.
4. **TLS**: `edge` termination on the Route — the router terminates TLS with the wildcard `shared-ingress-cert` and forwards plain HTTP to the gateway. `reencrypt`/`passthrough` are rejected because the gateway has no certificate of its own; it is an internal Service by design.
5. **Hostnames** on the shared domain must be **one label** under `apps.fajjjar.my.id` (`api-dev.apps.fajjjar.my.id`), because the wildcard SAN `*.apps.fajjjar.my.id` does not cover two labels (`api.dev.apps.fajjjar.my.id`).
6. **APIcast**: the prod rule is unchanged — all public ingress goes through 3scale APIcast to `gateway-service`, and the mesh gateway is **not** the prod public edge. The dev Route to the mesh gateway is a **dev-scoped exception** because 3scale is not deployed in `payu-dev`; it is documented here and in `docs/roadmap/TODOS.md` (`MESH-004`) and must not be copied to UAT/prod.

## Rationale

Option 1 is the only topology where the domain has exactly one owner. The DNS record, the NLB and the wildcard certificate all belong to the platform ingress tier, and OpenShift Routes bind a Service to a namespace — so a mesh gateway that wants to be reachable on `apps.fajjjar.my.id` has to be fronted by the shared router anyway. Putting the Istio gateway behind it costs one proxy hop and buys mesh-native routing, a single mTLS hop to the backend, and a policy point that can be restricted per path.

Option 2 would require two certificates for the same wildcard and a second NLB, and it would put certificate rotation for `apps.fajjjar.my.id` outside the existing cert-manager/IngressController automation that already works.

Option 3 is cheaper but it either keeps the dev API unreachable from the public domain or recreates the §7.3 backdoor without adding any mesh capability. The mesh gateway adds value precisely because it is the layer where Gateway API routing, path-level exposure and mTLS to the backend live.

## Consequences

**Positive**:

- One owner for the shared domain: platform ingress. The mesh gateway is an implementation detail behind it.
- No second NLB: the gateway Service is `ClusterIP`, and the transient NLB that the default `LoadBalancer` Service provisioned was deleted when the type was pinned.
- A real public API URL on the shared domain: `https://api-dev.apps.fajjjar.my.id/api/v1/*` → `gateway-service`, proven live (HTTP 401 on a protected path).
- Path-level exposure control in the mesh: only `/api/v1/*` and `/v1/partner/*` are routed; management endpoints (`/q/metrics`, `/q/openapi`, `/q/swagger-ui`) answer 404 at the gateway.
- The gateway → `gateway-service` hop is mutual TLS: `connection_security_policy="mutual_tls"` with `source_principal=spiffe://payu.local/ns/payu-mesh-gateway/sa/payu-public-gateway-istio`.

**Negative**:

- Two proxy hops in front of `gateway-service` in dev; the router's `X-Forwarded-For` is the only client IP the gateway sees.
- The `payu-mesh-gateway` namespace needs its own NetworkPolicy set (default-deny + router ingress + istiod/payu-dev/DNS egress), and `payu-dev` needed one additive ingress allowance for the gateway namespace (its `allow-intra-namespace` rule is namespace-local).
- The default IngressController has no `namespaceSelector`, so it **also** admits the new Route and serves `api-dev.apps.fajjjar.my.id` on the default NLB with the default (mismatched) certificate. DNS points only at the shared NLB, so the public path is unambiguous, but the duplicate admission is a known artifact.
- The dev Route bypasses APIcast by construction (3scale is absent in dev). It is a documented dev-only exception, not a precedent.

## Implementation Notes

Manifests: `infrastructure/platform/mesh/ingress/` (applied separately from `base/kustomization.yaml`).

```
oc apply -k infrastructure/platform/mesh/ingress
```

- `namespace.yaml` — `payu-mesh-gateway`, labelled `payu.fajjjar.my.id/ingress: shared`.
- `gateway.yaml` — Gateway API `Gateway` `payu-public-gateway` (HTTP :8080, `allowedRoutes` selector `app.kubernetes.io/part-of: payu`), `networking.istio.io/service-type: ClusterIP`.
- `route.yaml` — OpenShift `Route` `api-dev`, host `api-dev.apps.fajjjar.my.id`, `tls.termination: edge` → Service `payu-public-gateway-istio:http`.
- `http-route.yaml` — Gateway API `HTTPRoute` `payu-gateway-dev` in `payu-dev` → `gateway-service:8080`, paths `/api/v1`, `/v1/partner`, headers `X-PayU-Environment: dev` / `X-PayU-Ingress: gateway-api`.
- `network-policies.yaml` — default-deny in `payu-mesh-gateway`, router ingress on 8080/15021, egress to `istiod` (15012/15017), to `payu-dev` (8080/9090), DNS; plus the additive `allow-mesh-gateway-ingress` in `payu-dev` for `gateway-service:8080`.

Verification on `payu-drrpz` (2026-10-21):

```
$ curl -sk -o /dev/null -w "%{http_code}" https://api-dev.apps.fajjjar.my.id/api/v1/accounts
401
$ openssl s_client -connect api-dev.apps.fajjjar.my.id:443 -servername api-dev.apps.fajjjar.my.id
subject=CN=apps.fajjjar.my.id
X509v3 Subject Alternative Name: DNS:*.apps.fajjjar.my.id, DNS:apps.fajjjar.my.id
```

---
*Created via principal-architect — live evidence on cluster `payu-drrpz` (OCP 4.22.18), Sail Operator `servicemeshoperator3.v3.4.3`, Istio v1.30.5; refs ADR-0072 (mesh rollout), ADR-0064 (3scale edge protection), ADR-0069 (4.22 baseline)*
