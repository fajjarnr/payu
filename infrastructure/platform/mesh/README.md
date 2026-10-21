# OpenShift Service Mesh 3.4 (Sail Operator)

> Istio-based service mesh for PayU Digital Banking Platform on Red Hat OpenShift 4.22+.

## Overview

The mesh runs on **OpenShift Service Mesh 3.4** with the **Sail Operator**
(`servicemeshoperator3.v3.4.3`). In OSSM 3.x there is no `ServiceMeshControlPlane`
and no `ServiceMeshMemberRoll`; the control plane is created by a
`sailoperator.io/v1` custom resource.

| Resource | API group / kind | Name | Namespace | Purpose |
| --- | --- | --- | --- | --- |
| Control plane | `istios.sailoperator.io/v1` `Istio` | `default` | `istio-system` | Installs istiod (and, when configured, gateways) |
| CNI plugin | `istiocnis.sailoperator.io/v1` `IstioCNI` | `default` | `istio-cni` | Installs the CNI plugin that redirects sidecar traffic without a privileged init container |
| Dev ingress | `gateway.networking.k8s.io/v1` `Gateway` | `payu-public-gateway` | `payu-mesh-gateway` | Gateway API ingress behind the shared router, published on `apps.fajjjar.my.id` |
| Console | `kiali.io/v1alpha1` `Kiali` | `payu-kiali` | `istio-system` | Kiali console, scoped to `payu-dev`, `payu-mesh-gateway`, `istio-system` |
| Console plugin | `kiali.io/v1alpha1` `OSSMConsole` | `payu-ossmconsole` | `istio-system` | "OpenShift Service Mesh Console" plugin, proxying the Kiali API |
| Sidecar telemetry | `monitoring.coreos.com/v1` `ServiceMonitor` | `payu-mesh-sidecars` | `payu-mesh-telemetry` | Scrapes the Envoy metrics of the 24 meshed workloads in `payu-dev` (port 15090) |
| Control-plane telemetry | `monitoring.coreos.com/v1` `ServiceMonitor` | `payu-mesh-istiod` | `payu-mesh-telemetry` | Scrapes the istiod metrics (port 15014) |

Both CRs **must** be named `default` — the operator rejects other names. (The
console CRs in `base/kiali.yaml` are not part of the Sail operator and have no
such constraint.)

Namespaces:

| Namespace | Role |
| --- | --- |
| `istio-system` | Mesh root namespace. The `Istio` CR installs istiod here. A `PeerAuthentication` placed here would apply mesh-wide — none exists, and none may be created (see the per-environment overlays). |
| `istio-cni` | Holds the `istio-cni-node` DaemonSet that installs the CNI plugin. |
| `payu-mesh-gateway` | Holds the dev ingress gateway (`payu-public-gateway`) and its Route; labelled `payu.fajjjar.my.id/ingress: shared` so the shared IngressController admits it. |
| `payu-mesh-telemetry` | Holds the mesh telemetry ServiceMonitors; labelled `openshift.io/cluster-monitoring: "true"` so the cluster Prometheus scrapes them. |

The control plane version is pinned in the CRs (`spec.version: v1.30.5`). The Sail
operator resolves the matching Red Hat registry image itself — **do not** add
`images:` overrides in kustomize, they would fight the operator and break the
control plane.

## Layout

```
infrastructure/platform/mesh/
├── base/                  # control plane (any cluster)
│   ├── namespace.yaml     # istio-system + istio-cni namespaces
│   ├── istio-cni.yaml     # IstioCNI CR
│   ├── service-mesh.yaml  # Istio CR
│   ├── kiali.yaml         # Kiali + OSSMConsole console CRs (applied separately)
│   └── kustomization.yaml
├── overlays/              # per-environment PeerAuthentication
│   ├── payu-dev/          # PERMISSIVE mTLS (applied)
│   ├── payu-sit/          # PERMISSIVE mTLS (applied)
│   ├── payu-uat/          # STRICT mTLS (applied)
│   ├── payu-preprod/      # STRICT mTLS (applied)
│   └── payu/              # STRICT mTLS (prod, not applied yet)
├── ingress/               # dev ingress on the shared domain (applied separately)
├── telemetry/             # Envoy + istiod scrape into the cluster Prometheus (applied separately)
├── kustomization.yaml     # root entrypoint -> base
└── README.md
```

### What is applied where

| Path | Applied | Contents |
| --- | --- | --- |
| `base/` | **First** — any cluster | The control plane: `istio-system` + `istio-cni` namespaces, `IstioCNI` CR, `Istio` CR. Applied by the root kustomization (`oc apply -k infrastructure/platform/mesh`) and by `scripts/apply-cluster-infra.sh`. |
| `base/kiali.yaml` | **Separately** — not by `base/kustomization.yaml` | The console CRs: `Kiali` (`payu-kiali`), `OSSMConsole` (`payu-ossmconsole`) and the `cluster-monitoring-view` binding for the Kiali service account. Needs the `kiali-operator` subscription (OperatorHub `kiali-ossm`); the `kialis.kiali.io` and `ossmconsoles.kiali.io` CRDs ship with it. Applied by `scripts/apply-cluster-infra.sh`, which skips the file when the CRD is absent. |
| `overlays/payu-dev/` | **Second** — `payu-dev` only | Dev-scoped `PeerAuthentication` — **PERMISSIVE** mTLS, namespace-scoped, no selector. STRICT mTLS is enforced from UAT upward per `docs/adr/0037-internal-synchronous-inter-service-communication-via-grpc-and-protobuf-governance.md` and `docs/architecture/DEVSECOPS_ARCHITECTURE.md`. Applied by `scripts/mesh/deploy.sh -e dev` (step 2) and by `scripts/apply-cluster-infra.sh`. |
| `overlays/payu-sit/` | **Second** — `payu-sit` only | SIT-scoped `PeerAuthentication` — **PERMISSIVE** mTLS, same reason as dev. Applied by `scripts/mesh/deploy.sh -e sit` (step 2). |
| `overlays/payu-uat/` | **Second** — `payu-uat` only | UAT-scoped `PeerAuthentication` — **STRICT** mTLS. Applied by `scripts/mesh/deploy.sh -e uat` (step 2). |
| `overlays/payu-preprod/` | **Second** — `payu-preprod` only | Pre-prod-scoped `PeerAuthentication` — **STRICT** mTLS. Applied by `scripts/mesh/deploy.sh -e preprod` (step 2). |
| `overlays/payu/` | **Prod promotion** — not applied yet | Prod-scoped `PeerAuthentication` — **STRICT** mTLS. The `payu` namespace exists on `payu-drrpz` but carries no prod workloads, so this overlay is applied when the prod control plane and workloads are bootstrapped. |
| `ingress/` | **Separately** — not by `../kustomization.yaml` | The dev ingress on the shared domain: namespace `payu-mesh-gateway`, Gateway API `Gateway`/`HTTPRoute`, the `api-dev` Route and the gateway NetworkPolicies. Needs istiod (the `Gateway` is reconciled by it), so apply it after the control plane. Applied by `scripts/mesh/deploy.sh -e dev` (step 3). |
| `telemetry/` | **Separately** — not by `../kustomization.yaml` | The mesh telemetry: namespace `payu-mesh-telemetry`, the two ServiceMonitors, the read-only discovery RBAC for the `prometheus-k8s` service account and the NetworkPolicy that opens port 15090 in `payu-dev`. Applied by `scripts/mesh/deploy.sh -e dev` (step 4). |

Per-environment overlays are **not** referenced from the root kustomization: the
`payu-dev` overlay sets `namespace: payu-dev`, and pulling it into the root
would move the namespaced `Istio` CR out of `istio-system` and break the
control plane.

### Per-environment policy overlays

Each environment has its own namespace-scoped `PeerAuthentication`:

| Overlay | Namespace | Object | Mode | State |
| --- | --- | --- | --- | --- |
| `overlays/payu-dev/` | `payu-dev` | `permissive-mtls` | PERMISSIVE | applied |
| `overlays/payu-sit/` | `payu-sit` | `permissive-mtls` | PERMISSIVE | applied |
| `overlays/payu-uat/` | `payu-uat` | `strict-mtls` | STRICT | applied |
| `overlays/payu-preprod/` | `payu-preprod` | `strict-mtls` | STRICT | applied |
| `overlays/payu/` | `payu` | `strict-mtls` | STRICT | not applied yet — prod promotion |

Apply one through the script (step 2) or directly:

```bash
scripts/mesh/deploy.sh -e uat
oc apply -k infrastructure/platform/mesh/overlays/payu-uat
```

There is **no** mesh-wide policy. A `PeerAuthentication` in `istio-system` (the
mesh root namespace) applies to every workload in the mesh, so on a cluster
that hosts `payu-dev` next to `payu-sit`/`payu-uat` it would bind the sibling
namespaces too. It must not be created — the per-environment overlays are the
only place the posture is defined.

PERMISSIVE in dev and SIT keeps the plain-text path open for the sources that
are not part of the mesh (the OpenShift router, the kubelet health probe);
STRICT from UAT upward per ADR-0037.

### Legacy Maistra-era assets still in `base/`

The following files in `base/` are the **legacy Maistra 2.x / prod-ingress
assets**. They are **not** referenced by `base/kustomization.yaml`, so they are
not applied by the dev control plane, and they still need rework for OSSM 3.4
before prod/UAT ingress work can rely on them:

| File | Legacy content | Needs |
| --- | --- | --- |
| `base/control-plane.yaml` | Maistra-era namespace/network policy/HPA/PDB for an `istio-ingressgateway` deployment | Rework for OSSM 3.4 — the dev ingress is now the Gateway API package `ingress/` |
| `base/gateway.yaml` | Istio `Gateway` + `VirtualService` for the prod/UAT ingress gateway, TLS termination, JWT | Rework against the OSSM 3.4 ingress story |
| `base/destination-rules.yaml` | `DestinationRule` traffic policies for the `payu` namespace | Port per environment |
| `base/ratelimit-service.yaml` | Rate limit service + `EnvoyFilter` | Rework for OSSM 3.4 |
| `base/security-headers.yaml` | Security-header `EnvoyFilter` | Rework for OSSM 3.4 |
| `base/certs/` | Legacy ingress CA/TLS material | Ingress certificates are provisioned externally; private keys are not kept in git |
| `base/gateway-api/` | Optional Kubernetes Gateway API package that reuses the Istio ingress service | Superseded by `ingress/` — it points at a non-existent `istio-ingressgateway.istio-system` service; not applied |
| `base/peer-authentication.yaml` | Mesh-wide + per-namespace `PeerAuthentication` and `AuthorizationPolicy` for `payu`, `payu-uat`, `payu-preprod`, `payu-sit` | The per-env overlays replace the `PeerAuthentication`; the `AuthorizationPolicy` set is still to be reworked |

> **Hazard — do not add `base/peer-authentication.yaml` to `base/kustomization.yaml`.**
> Its root-namespace `mesh-wide-mtls` (STRICT) object applies to every workload
> in the mesh root namespace. On a cluster that hosts `payu-dev` alongside
> `payu-sit`/`payu-uat`, applying it would bind those sibling namespaces to
> STRICT as well. It also carried a `payu-dev`/`permissive-mtls` object that
> clashed with the dev overlay (last-apply-wins); that duplicate has been
> removed — `overlays/payu-dev/peer-authentication.yaml` is the single owner of
> the dev PERMISSIVE policy.

## Prerequisites

1. OpenShift 4.22+ running.
2. The Sail Operator (`servicemeshoperator3`) installed from OperatorHub — see
   `infrastructure/foundation/cluster-operators/subscriptions.yaml`.
3. Cluster resources for istiod and the CNI DaemonSet (one pod per node).

## Runbook

### Apply

The per-environment entry point is `scripts/mesh/deploy.sh`. It applies the
control plane, the environment policy overlay and the dev-scoped ingress and
telemetry packages in that order:

```bash
# Everything for dev (control plane + policy + ingress + telemetry)
scripts/mesh/deploy.sh -e dev

# A single step, or a dry run
scripts/mesh/deploy.sh -e dev -s 3
scripts/mesh/deploy.sh -e dev -d
```

| Step | Package | Scope |
| --- | --- | --- |
| 1 | `infrastructure/platform/mesh` (control plane) | cluster-wide |
| 2 | `infrastructure/platform/mesh/overlays/<env>` | per environment |
| 3 | `infrastructure/platform/mesh/ingress` | dev only |
| 4 | `infrastructure/platform/mesh/telemetry` | dev only |

The console CRs are cluster-scoped operator CRs and stay with the
cluster-scoped script: `scripts/apply-cluster-infra.sh` applies
`base/kiali.yaml` and skips it when the `ossmconsoles.kiali.io` CRD is absent.

The same packages by hand:

```bash
# 1. Control plane (istio-system + istio-cni namespaces, IstioCNI CR, Istio CR)
oc apply -k infrastructure/platform/mesh

# 2. Dev-scoped mesh policy (PERMISSIVE mTLS for payu-dev)
oc apply -k infrastructure/platform/mesh/overlays/payu-dev

# 3. Dev ingress on the shared domain (needs istiod from step 1)
oc apply -k infrastructure/platform/mesh/ingress

# 4. Mesh telemetry (ServiceMonitors + discovery RBAC)
oc apply -k infrastructure/platform/mesh/telemetry
```

The overlay needs the `payu-dev` namespace to exist — apply
`infrastructure/foundation/namespaces/overlays/shared` first. Steps 3 and 4
need that namespace too, and for the telemetry to carry data the meshed
workloads must exist (`infrastructure/workloads/overlays/payu-dev`).

### How to verify

```bash
# Control plane CRs
oc get istios.sailoperator.io -n istio-system
oc get istiocnis.sailoperator.io -n istio-cni

# Control plane rollouts
oc rollout status deploy/istiod -n istio-system
oc rollout status ds/istio-cni-node -n istio-cni

# Dev mesh policy (expect: permissive-mtls, PERMISSIVE, namespace payu-dev)
oc get peerauthentication -n payu-dev

# Injected sidecars: meshed pods list istio-validation + istio-proxy
# as initContainers; non-meshed pods list nothing
oc get pods -n payu-dev -o jsonpath='{range .items[*]}{.metadata.name}{"  "}{.spec.initContainers[*].name}{"\n"}{end}'

# Proxy config of one meshed workload
# (the istio-proxy container ships no curl; use pilot-agent from the proxy
# container, or curl from the app container)
oc exec -n payu-dev deploy/<svc> -c istio-proxy -- pilot-agent request GET config_dump | grep -A5 'outbound|.*<other-svc>'
```

#### Drift check

The mesh packages are applied as whole kustomizations, so they are not
compared per service. The drift check is the server-side dry run of the
package: every resource must come back `unchanged`. `scripts/verify-overlay-drift.sh`
runs it for the control plane, the console CRs, the environment policy overlay
and the dev-scoped ingress/telemetry packages.

```bash
# One package by hand (nothing is persisted by a server dry run)
oc apply --dry-run=server -k infrastructure/platform/mesh/ingress
oc apply --dry-run=server -k infrastructure/platform/mesh/telemetry

# All of them, with the workload overlay drift
./scripts/verify-overlay-drift.sh payu-dev
```

`overlays/payu` is deliberately not part of the check — the prod overlay is
not applied yet, so it would always report `created`.

### Kiali console

`base/kiali.yaml` holds three objects: the `Kiali` CR (`payu-kiali`, the console
server), the `OSSMConsole` CR (`payu-ossmconsole`, the "OpenShift Service Mesh
Console" plugin that proxies the Kiali API) and a `cluster-monitoring-view`
ClusterRoleBinding for the Kiali service account, so the console can query the
cluster Prometheus.

Apply it separately from the control plane — both CRDs come from the
`kiali-operator` subscription, not from OSSM 3.4:

```bash
oc apply -f infrastructure/platform/mesh/base/kiali.yaml
```

`scripts/apply-cluster-infra.sh` applies this file as part of the
cluster-scoped install and skips it when the `ossmconsoles.kiali.io` CRD is
absent.

The console is scoped to three namespaces: `payu-dev` (the app tier),
`payu-mesh-gateway` (the ingress gateway the dev `HTTPRoute` points at) and
`istio-system` (the control plane). `deployment.accessible_namespaces` lists
them (deprecated in the CRD) and the scope is actually enforced by
`deployment.cluster_wide_access: false` plus a `discovery_selectors`
`matchExpressions` on `kubernetes.io/metadata.name In (payu-dev,
payu-mesh-gateway, istio-system)`. The gateway namespace must stay in scope:
without it Kiali cannot resolve the `HTTPRoute` parentRef and flags the route
with KIA1401 "Route is pointing to a non-existent or inaccessible K8s gateway"
in the Istio Config view. It authenticates with OpenShift OAuth
(`auth.strategy: openshift`) and reads metrics from the in-cluster Thanos
querier — the only Prometheus reachable from other namespaces, since
`prometheus-operated:9090` is blocked by the `openshift-monitoring`
NetworkPolicy.

Reach it:

- Route: `https://kiali-istio-system.apps.payu.ocp.fajjjar.my.id` (cluster
  domain, admitted by the default router)
- OpenShift console: the "OpenShift Service Mesh Console" plugin, which proxies
  the Kiali API with the logged-in user token.

Verify:

```bash
# CR status: Running/Successful True, Failure False
oc get kiali,ossmconsole -n istio-system -o custom-columns='KIND:.kind,NAME:.metadata.name,STATUS:.status.conditions[*].status'

# Rollout and route
oc rollout status deploy/kiali -n istio-system
oc get route kiali -n istio-system

# Console API — the image ships no curl/wget, so query it from another pod
oc exec -n payu-dev deploy/account-service -c app -- \
  curl -sk https://kiali.istio-system.svc:20001/api
oc exec -n payu-dev deploy/account-service -c app -- \
  curl -sk -o /dev/null -w '%{http_code}\n' https://kiali.istio-system.svc:20001/healthz

# Mesh inventory seen by the console (needs the OpenShift token)
TOKEN=$(oc whoami -t)
oc exec -n payu-dev deploy/account-service -c app -- \
  curl -sk -H "Authorization: Bearer $TOKEN" \
  "https://kiali.istio-system.svc:20001/api/clusters/services?namespaces=payu-dev"
```

`/api` answers without a token and reports the console state plus the
Prometheus version it talks to; the rest of the API needs the OpenShift token
(`/api/namespaces` answers `401` without one). The traffic graph is
`/api/namespaces/graph?namespaces=payu-dev`.

### Sidecar injection

Injection is **opt-in per deployment**, via the pod-template label
`sidecar.istio.io/inject: "true"` (set in the `labels.pairs` block of each
`infrastructure/workloads/base/<service>/kustomization.yaml`). The 24 app-tier
services are meshed; the simulators (`bi-fast`, `biller`, `dukcapil`, `qris`,
`va`) and `web-app` are not.

Do **not** opt a whole namespace in with a namespace-wide label: that would also
inject the CNPG, Strimzi and Kafka pods running in `payu-dev` and break them.

Two Istio 1.30 specifics worth knowing:

- **The sidecar is a native sidecar.** `istio-proxy` is injected as an
  initContainer with `restartPolicy: Always` (plus an `istio-validation`
  initContainer), so `spec.containers` still shows only `app`. Look at
  `spec.initContainers` to confirm injection.
- **The sidecar runs with the namespace UID range** (`runAsUser: 1000759999`),
  not the legacy hardcoded `1337`, which is what keeps injected pods compliant
  with the `restricted-v2` SCC.

#### Sidecar mode vs ambient mode

The mesh runs in **sidecar mode** (`spec.profile: default`): one Envoy per
pod, injected by the webhook. A native sidecar is still a sidecar — the
implementation changed (initContainer with `restartPolicy: Always`), not the
architecture.

**Ambient mode** (sidecarless: a node-level `ztunnel` for L4 mTLS plus optional
per-service `waypoint` proxies for L7) is **GA in OSSM 3.4** — the operator in
this cluster already ships the `ztunnels.sailoperator.io` CRD and the ztunnel
image. It is a different data plane, not a newer version of this one; the
migration is a separate decision, tracked as `MESH-006` in
`docs/roadmap/TODOS.md`. Ambient does not support the VirtualService features
that the chaos strategy needs (`HTTPFaultInjection`, JWT claim-based routing,
Delegate rules, rate limiting, `ignoreCase`), so those workloads must stay on
sidecars either way.

### Dev ingress on the shared domain (`ingress/`)

`ingress/` is a **separate package** — it is not referenced from
`../kustomization.yaml`:

```bash
oc apply -k infrastructure/platform/mesh/ingress
```

It publishes the dev API on the shared domain `apps.fajjjar.my.id`:

```
client -> AWS NLB (shared) -> OpenShift router (TLS, shared-ingress-cert)
      -> Istio gateway (payu-mesh-gateway, ClusterIP) -> gateway-service (mTLS)
```

| Object | Kind | Namespace | Notes |
| --- | --- | --- | --- |
| `payu-mesh-gateway` | Namespace | - | labelled `payu.fajjjar.my.id/ingress: shared` so the shared IngressController admits its Route |
| `payu-public-gateway` | `gateway.networking.k8s.io/v1` `Gateway` | `payu-mesh-gateway` | `gatewayClassName: istio`; istiod reconciles it into Deployment+Service `payu-public-gateway-istio` |
| `api-dev` | `route.openshift.io/v1` `Route` | `payu-mesh-gateway` | host `api-dev.apps.fajjjar.my.id`, TLS `edge` termination by the shared router |
| `payu-gateway-dev` | `gateway.networking.k8s.io/v1` `HTTPRoute` | `payu-dev` | routes `/api/v1` and `/v1/partner` to `gateway-service:8080` |
| `deny-public-simulator-paths` | `security.istio.io/v1beta1` `AuthorizationPolicy` | `payu-mesh-gateway` | DENY `/api/v1/simulator*` on the gateway workload — the gateway's `AuthorizationFilter` treats that path as public for the internal `account-service` hop, and the public route forwards the whole `/api/v1` prefix |

The shared router owns the domain end to end (DNS, NLB, TLS certificate); the
Istio gateway is a backend behind it and never owns the domain. The hostname
must be ONE label below the domain: `api-dev.apps.fajjjar.my.id` is covered by
the wildcard `*.apps.fajjjar.my.id`, `api.dev.apps.fajjjar.my.id` is not.

### Mesh telemetry (`telemetry/`)

`telemetry/` is a **separate package** — it is not referenced from
`../kustomization.yaml`:

```bash
oc apply -k infrastructure/platform/mesh/telemetry
```

It scrapes two targets into the OpenShift cluster Prometheus:

| ServiceMonitor | Target | Port | Series |
| --- | --- | --- | --- |
| `payu-mesh-sidecars` | the 24 meshed workloads in `payu-dev` | Envoy metrics `15090` (`/stats/prometheus`) | `istio_requests_total`, `envoy_cluster_upstream_cx_total` |
| `payu-mesh-istiod` | the `istiod` deployment in `istio-system` | `http-monitoring` (`15014`) | control-plane metrics |

**The cluster Prometheus is the only option here.** It is the Prometheus the
Kiali CR queries (`https://thanos-querier.openshift-monitoring.svc:9091`), and
this cluster runs no user-workload Prometheus — anything scraped elsewhere
would never reach the Kiali service graph.

**One label rule.** The cluster Prometheus selects ServiceMonitors only in
namespaces labelled `openshift.io/cluster-monitoring: "true"`
(`spec.serviceMonitorNamespaceSelector`). The label sits on the dedicated
namespace `payu-mesh-telemetry`, which holds the ServiceMonitors, so the
scrape configuration stays out of the app namespace and out of
`istio-system`, whose Namespace objects are managed elsewhere in this repo.

**The relabel trick.** The meshed pods declare no `15090` containerPort — the
Envoy metrics port is excluded from the Istio iptables capture list, so it
answers on the pod IP but is not a declared port. A ServiceMonitor with a
numeric `targetPort: 15090`, or a PodMonitor, would therefore match nothing.
Instead the discovered endpoint address is relabeled to `podIP:15090`:

```yaml
relabelings:
  - sourceLabels: [__address__]
    regex: '([^:]+):[0-9]+'
    replacement: '${1}:15090'
    targetLabel: __address__
    action: replace
```

**Extra RBAC and NetworkPolicy.** The cluster Prometheus resolves the targets
through the Kubernetes API, so its service account (`prometheus-k8s` in
`openshift-monitoring`) needs read access to services, endpoints, pods and
endpointslices in `payu-dev` and `istio-system` — the stock `prometheus-k8s`
role covers the `openshift-*` namespaces only. Read-only verbs, no write, no
other subjects. `payu-dev` denies all ingress by default, so a NetworkPolicy
opens port `15090` for the `openshift-monitoring` namespace only; the istiod
endpoint in `istio-system` is not restricted and needs no policy.

Verify:

```bash
# The metrics are in the cluster Prometheus
TOKEN=$(oc whoami -t)
curl -sk -H "Authorization: Bearer $TOKEN" \
  'https://thanos-querier-openshift-monitoring.apps.payu.ocp.fajjjar.my.id/api/v1/query?query=count(istio_requests_total)'
# {"status":"success",...,"value":[1792581799.894,"72"]}

curl -sk -H "Authorization: Bearer $TOKEN" \
  'https://thanos-querier-openshift-monitoring.apps.payu.ocp.fajjjar.my.id/api/v1/query?query=count%20by%20(connection_security_policy)%20(istio_requests_total)'
# mutual_tls 25, none 3, unknown 44

# The Kiali graph is built from those series
TOKEN=$(oc whoami -t)
oc exec -n payu-dev deploy/account-service -c app -- \
  curl -sk -H "Authorization: Bearer $TOKEN" \
  "https://kiali.istio-system.svc:20001/api/namespaces/graph?namespaces=payu-dev"
# elements: 31 nodes, 71 edges
```

The graph answer is in the Cyborg shape: `elements.nodes` and `elements.edges`.

### mTLS in dev

Auto-mTLS is on: mesh-to-mesh traffic is upgraded to `mutual_tls` even though
the dev `PeerAuthentication` is PERMISSIVE. Verified via the proxy metrics:

```bash
oc exec -n payu-dev deploy/<svc> -c app -- curl -s localhost:15090/stats/prometheus \
  | grep -o 'connection_security_policy="[a-z_]*"' | sort -u
# connection_security_policy="mutual_tls"
```

PERMISSIVE (not STRICT) is deliberate in dev: it keeps the plain-text path
open for non-mesh sources — the kubelet health probe and the OpenShift router —
which are not part of the mesh. STRICT mTLS is enforced from UAT upward.

### Remove the mesh

```bash
# Remove the dev policy and the control plane
oc delete -k infrastructure/platform/mesh/overlays/payu-dev

# Remove the dev ingress (it needs istiod)
oc delete -k infrastructure/platform/mesh/ingress

# Remove the mesh telemetry
oc delete -k infrastructure/platform/mesh/telemetry

oc delete -k infrastructure/platform/mesh

# The operator cleans up istiod and the CNI DaemonSet
oc get pods -n istio-system
oc get pods -n istio-cni
```

Pods that already carry an injected sidecar keep it until they are recreated;
delete the workload pods after removing the control plane if the sidecar must
go away.

## Security posture

- **mTLS**: PERMISSIVE in `payu-dev` and `payu-sit` (the policy overlays of
  the same name), STRICT from `payu-uat` upward. Istio issues and rotates the
  workload certificates — no certificate material in this repository.
- **Authorization**: not implemented yet. The per-environment overlays carry a
  `PeerAuthentication` only; the deny-by-default `AuthorizationPolicy` set
  still lives in the legacy `base/peer-authentication.yaml`, which is not
  applied.
- **NetworkPolicy**: complements the mesh at the CNI level
  (`infrastructure/foundation/namespaces/base/network-policies.yaml`).
- **Traffic**: the CNI plugin redirects sidecar traffic without a privileged
  init container, so injected pods stay compatible with the `restricted-v2` SCC.

## Troubleshooting

#### Sidecar not injected

```bash
# Does the pod template opt in?
oc get deployment <name> -n payu-dev \
  -o jsonpath='{.spec.template.metadata.labels.sidecar\.istio\.io/inject}{"\n"}'

# Did the pod get the native sidecar?
oc get pods -n payu-dev -o jsonpath='{range .items[*]}{.metadata.name}{"  "}{.spec.initContainers[*].name}{"\n"}{end}'

# Is the injector webhook present?
oc get mutatingwebhookconfiguration istio-sidecar-injector
```

#### Control plane not ready

```bash
oc get istios.sailoperator.io -n istio-system
oc describe istios.sailoperator.io default -n istio-system
oc logs deployment/istiod -n istio-system
```

#### mTLS connection failures

```bash
oc get peerauthentication -n payu-dev -o yaml
oc logs deployment/<service> -n payu-dev -c istio-proxy
```

## References

- [Red Hat OpenShift Service Mesh Documentation](https://docs.openshift.com/container-platform/4.22/service_mesh/index.html)
- [Istio Documentation](https://istio.io/latest/docs/)
- [Istio Security Best Practices](https://istio.io/latest/docs/concepts/security/)

## Known limits

- **Dev ingress runs through an Istio gateway behind the shared router.**
  Dev now has an Istio ingress gateway: Gateway API `Gateway`
  `payu-public-gateway` in the `payu-mesh-gateway` namespace, reconciled by
  istiod into a ClusterIP Deployment/Service, published by the Route `api-dev`
  on the shared domain `apps.fajjjar.my.id`. The shared domain is served by the
  `shared-ingress` IngressController (3/3 replicas, Let's Encrypt
  `shared-ingress-cert`, wildcard `*.apps.fajjjar.my.id`). The OpenShift
  **default** IngressController still serves the cluster domain
  `apps.payu.ocp.fajjjar.my.id` (`web-app`, `analytics-ws`) with
  `app-router-certs`. The cluster has no router-labelled nodes
  (`oc get nodes -l node-role.kubernetes.io/router` is empty), so the shared
  router is a separate IngressController rather than a dedicated node tier.
  The dev path bypasses 3scale APIcast (not deployed in dev) — the prod rule
  "all public ingress through APIcast" is unchanged, see ADR-0073.
- **The shared-domain Route is admitted by both IngressControllers.** The
  `shared-ingress` controller has a `namespaceSelector` on
  `payu.fajjjar.my.id/ingress: shared`; the `default` controller has none, so
  it admits every Route in the cluster, `api-dev` included. DNS for
  `api-dev.apps.fajjjar.my.id` points only at the shared NLB
  (`52.74.162.171`), so the public path is unambiguous — the duplicate
  admission is an artifact, not a second entry point.
- **The ingress gateway is a single replica with no PodDisruptionBudget.**
  `payu-public-gateway-istio` runs 1/1 and `payu-mesh-gateway` holds no PDB,
  so a node drain takes the public dev API down for the length of the
  restart.
- **`reencrypt` is not exercised.** The gateway holds no certificate — the
  shared router terminates TLS (`edge`) in front of the ClusterIP gateway — so
  the `reencrypt` mode of the ingress path is untested.
- **The prod gateway design behind APIcast is still open.** Whether the prod
  ingress is 3scale APIcast or the Istio gateway is undecided;
  `base/gateway.yaml` and the other Maistra-era assets are still to be
  reworked.
- **Simulators, `web-app`, Keycloak and the data plane are not in the mesh.**
  They carry no `sidecar.istio.io/inject` label.
- **Kiali is deployed for the dev mesh only; Jaeger/tracing is not wired up.**
  The console CRs live in `base/kiali.yaml` and the console is restricted to
  `payu-dev`, `payu-mesh-gateway` and `istio-system`, but there is no tracing
  backend in the cluster, so `spec.server.observability.tracing.enabled` stays
  `false`.
- **The traffic graph is populated, but only for dev.** `telemetry/` scrapes
  the 24 sidecars and istiod into the cluster Prometheus, so
  `istio_requests_total` is no longer empty and the Kiali graph renders
  (31 nodes / 71 edges on `payu-drrpz`, 2026-10-21). The other environments
  have no telemetry package, and the console is scoped to `payu-dev`.
- **The STRICT posture of sit/uat/preprod/prod is unverified in traffic.** The
  per-environment overlays exist and are applied (see the table above), but
  those environments are not bootstrapped on `payu-drrpz` — they carry no
  workloads, so STRICT mTLS has never been exercised. The prod overlay
  (`overlays/payu`) is not applied at all.
- **STRICT namespaces need their public ingress through a mesh gateway.** The
  OpenShift router is not a mesh member, so its plain-text traffic is rejected
  by a STRICT `PeerAuthentication` — the two `connection_security_policy="none"`
  series seen in `payu-dev` come from the kubelet probe and the router. Before
  `payu-uat` / `payu-preprod` are promoted to STRICT, their public route has to
  be fronted by a mesh gateway the way `ingress/` does in dev. Kubelet probes
  are unaffected: Istio rewrites them to the agent port.
- **The simulator path is closed by mesh policy, not by the gateway.** The
  gateway's `AuthorizationFilter` lists `/api/v1/simulator/dukcapil/verify` as
  public for the internal `account-service` hop, and the public route forwards
  the whole `/api/v1` prefix, so `AuthorizationPolicy`
  `deny-public-simulator-paths` denies `/api/v1/simulator*` at the gateway
  workload. The internal hop never traverses the gateway and keeps working;
  narrowing the gateway's own `PUBLIC_ENDPOINTS` list is still open.

