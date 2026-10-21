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

Both CRs **must** be named `default` — the operator rejects other names.

Namespaces:

| Namespace | Role |
| --- | --- |
| `istio-system` | Mesh root namespace. The `Istio` CR installs istiod here; a `PeerAuthentication` placed here applies mesh-wide. |
| `istio-cni` | Holds the `istio-cni-node` DaemonSet that installs the CNI plugin. |

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
│   └── kustomization.yaml
├── overlays/
│   └── payu-dev/          # dev-scoped PeerAuthentication (PERMISSIVE mTLS)
├── kustomization.yaml     # root entrypoint -> base
└── README.md
```

### What is applied where

| Path | Applied | Contents |
| --- | --- | --- |
| `base/` | **First** — any cluster | The control plane: `istio-system` + `istio-cni` namespaces, `IstioCNI` CR, `Istio` CR. Applied by the root kustomization. |
| `overlays/payu-dev/` | **Second** — `payu-dev` only | Dev-scoped `PeerAuthentication` — **PERMISSIVE** mTLS, namespace-scoped, no selector. STRICT mTLS is enforced from UAT upward per `docs/adr/0037-internal-synchronous-inter-service-communication-via-grpc-and-protobuf-governance.md` and `docs/architecture/DEVSECOPS_ARCHITECTURE.md`. |

Per-environment overlays are **not** referenced from the root kustomization: the
`payu-dev` overlay sets `namespace: payu-dev`, and pulling it into the root
would move the namespaced `Istio` CR out of `istio-system` and break the
control plane.

### Legacy Maistra-era assets still in `base/`

The following files in `base/` are the **legacy Maistra 2.x / prod-ingress
assets**. They are **not** referenced by `base/kustomization.yaml`, so they are
not applied by the dev control plane, and they still need rework for OSSM 3.4
before prod/UAT ingress work can rely on them:

| File | Legacy content | Needs |
| --- | --- | --- |
| `base/control-plane.yaml` | Maistra-era namespace/network policy/HPA/PDB for an `istio-ingressgateway` deployment | Rework for OSSM 3.4 (no ingress gateway in dev) |
| `base/gateway.yaml` | Istio `Gateway` + `VirtualService` for the prod/UAT ingress gateway, TLS termination, JWT | Rework against the OSSM 3.4 ingress story |
| `base/destination-rules.yaml` | `DestinationRule` traffic policies for the `payu` namespace | Port per environment |
| `base/ratelimit-service.yaml` | Rate limit service + `EnvoyFilter` | Rework for OSSM 3.4 |
| `base/security-headers.yaml` | Security-header `EnvoyFilter` | Rework for OSSM 3.4 |
| `base/kiali.yaml` | Kiali CR | Kiali operator is installed separately (`kiali-ossm` subscription) |
| `base/certs/` | Legacy ingress CA/TLS material | Ingress certificates are provisioned externally; private keys are not kept in git |
| `base/gateway-api/` | Optional Kubernetes Gateway API package that reuses the Istio ingress service | Apply separately, only after the ingress rework |
| `base/peer-authentication.yaml` | Mesh-wide + per-namespace `PeerAuthentication` and `AuthorizationPolicy` for `payu`, `payu-uat`, `payu-preprod`, `payu-sit` | Per-env overlay replaces it; dev posture lives in `overlays/payu-dev/` |

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

```bash
# 1. Control plane (istio-system + istio-cni namespaces, IstioCNI CR, Istio CR)
oc apply -k infrastructure/platform/mesh

# 2. Dev-scoped mesh policy (PERMISSIVE mTLS for payu-dev)
oc apply -k infrastructure/platform/mesh/overlays/payu-dev
```

The overlay needs the `payu-dev` namespace to exist — apply
`infrastructure/foundation/namespaces/overlays/shared` first.

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
oc delete -k infrastructure/platform/mesh

# The operator cleans up istiod and the CNI DaemonSet
oc get pods -n istio-system
oc get pods -n istio-cni
```

Pods that already carry an injected sidecar keep it until they are recreated;
delete the workload pods after removing the control plane if the sidecar must
go away.

## Security posture

- **mTLS**: PERMISSIVE in `payu-dev` (mesh policy in `overlays/payu-dev/`),
  STRICT from `payu-uat` upward. Istio issues and rotates the workload
  certificates — no certificate material in this repository.
- **Authorization**: `AuthorizationPolicy` with deny-by-default and explicit
  allow rules lives in the per-environment overlays.
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

- **No Istio ingress gateway in dev.** External traffic in dev is served by
  the OpenShift **default IngressController** on the cluster domain
  `apps.payu.ocp.fajjjar.my.id` (e.g. `payu-dev.apps.payu.ocp.fajjjar.my.id`),
  with the Let's Encrypt certificate `app-router-certs`
  (`CN=*.apps.payu.ocp.fajjjar.my.id`) — not by an Istio `Gateway`. The
  `payu-dev` Routes carry that cluster-domain host and no `routerName`, so they
  are admitted by the `default` router only.
  The `shared-ingress` IngressController (domain `apps.fajjjar.my.id`) is
  healthy — 3/3 replicas, its own Let's Encrypt `shared-ingress-cert` — but no
  Route is published on the shared domain, so `*.apps.fajjjar.my.id` answers
  503. The cluster has no router-labelled nodes
  (`oc get nodes -l node-role.kubernetes.io/router` is empty), so there is no
  dedicated ingress tier. The legacy ingress assets in `base/` are not
  applied.
- **Simulators, `web-app`, Keycloak and the data plane are not in the mesh.**
  They carry no `sidecar.istio.io/inject` label.
- **No Kiali/Jaeger dashboards are wired up yet.** The Kiali operator is
  installed, but `base/kiali.yaml` is a legacy asset awaiting rework.
- **Per-env policy overlays exist for `payu-dev` only.** UAT and prod overlays
  are still to be written.
