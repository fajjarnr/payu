# ADR-0072: OpenShift Service Mesh 3.4 (Sail Operator) — Dev Rollout

**Status**: Accepted  
**Date**: 2026-10-21  
**Deciders**: Platform Engineering, Principal Architect  
**Relates to**: ADR-0069 (OpenShift 4.22 Platform Standard), ADR-0037 (gRPC & Protobuf Governance), ADR-0012 (Container Standardization), ADR-0034 (Observability), ADR-0031 (DB Resilience)

---

## Context

The platform has declared OpenShift Service Mesh (OSSM) as the mTLS layer since the 4.22 baseline bump (ADR-0069), and `docs/architecture/DEVSECOPS_ARCHITECTURE.md` §4.5.2 mandates `PeerAuthentication: STRICT` from `payu-uat` upward. Until now the mesh existed only as **manifests**: the repository carried Maistra-era 2.6 objects (`ServiceMeshControlPlane`, `ServiceMeshMemberRoll`, `control-plane.yaml`, `gateway.yaml`, `destination-rules.yaml`) that were never applied, and the mTLS claim in the architecture docs was therefore **unimplemented**.

OSSM 3.x removed the Maistra CRDs entirely. The control plane is now created by the **Sail Operator** through `sailoperator.io/v1` custom resources (`Istio`, `IstioCNI`). The legacy `ServiceMeshControlPlane` objects cannot be applied against a 3.4 operator, and their image overrides pinned to `2.6.0` would have broken a 3.4 control plane even if the CRDs had survived.

Two further constraints shaped this decision:

- **Shared namespace.** `payu-dev` on cluster `payu-drrpz` (OpenShift 4.22.18) hosts not only app-tier Deployments but also CNPG (PostgreSQL), Strimzi Kafka, DataGrid, Keycloak and AMQ pods. A namespace-wide injection label would put an Envoy sidecar in front of all of them.
- **Dev posture.** Dev is a debugging environment: the OpenShift shared router and the kubelet probe arrive as plain-text, non-mesh sources.

## Decision Drivers

- **Make the mTLS claim real** — the architecture docs already promise mesh mTLS; a live control plane is the only way to close that gap (technical).
- **Supported operator path** — stay on the certified OSSM 3.4 / Sail Operator API rather than resurrecting Maistra 2.6 CRDs (technical).
- **No blast radius on shared infrastructure** — CNPG, Kafka, DataGrid and Keycloak must keep working; a sidecar cannot be injected into them (business).
- **Environment-appropriate posture** — dev stays PERMISSIVE with NetworkPolicy carrying dev ingress/egress; STRICT starts at UAT (business).
- **Low operational surface** — no second ingress tier to maintain in dev; the OpenShift router already serves external traffic (team).

## Considered Options

### Option 1 — Stay on the legacy Maistra 2.6 manifests, no live mesh

- **Pros**: zero work; the docs keep describing a mesh that exists only on paper; no sidecar to reason about in dev.
- **Cons**: the operator version is unsupported; `ServiceMeshControlPlane` / `ServiceMeshMemberRoll` CRDs no longer exist, so the control plane **can never run**; the mTLS promise in `DEVSECOPS_ARCHITECTURE.md` §4.5.2 stays unimplemented; the image overrides pinned to `2.6.0` would have broken a 3.4 control plane; dev would keep relying on a single-layer posture with no service identity.

### Option 2 — OSSM 3.4 with namespace-wide injection label

- **Pros**: a single label on `payu-dev`; zero per-service edits; new services are meshed automatically.
- **Cons**: the label also injects sidecars into the CNPG, Strimzi Kafka, DataGrid, AMQ and Keycloak pods that share the namespace, and breaks them; no per-service control over what is meshed; a namespace label cannot be scoped to the app tier.

### Option 3 — OSSM 3.4 Sail Operator + per-deployment opt-in injection (chosen)

- **Pros**: only the 24 app-tier services are meshed; infrastructure pods are untouched; per-service control over injection; no namespace-wide blast radius; the injection label lives in the shared workload `base/` so every environment inherits it.
- **Cons**: 24 label edits in `infrastructure/workloads/base/<svc>/kustomization.yaml`; injection must be repeated for every new service (a service added without the label is silently unmeshed); the label in shared `base/` also carries into the sit/uat/preprod/prod overlays.

### Option 4 — STRICT mTLS immediately in dev

- **Pros**: strongest possible posture; exercises the exact configuration that UAT and prod will run; no reliance on auto-mTLS.
- **Cons**: breaks the plain-text path from the non-meshed OpenShift router and the kubelet health probe in dev, which are not mesh members; contradicts the documented dev posture (NetworkPolicy carries dev ingress/egress); is not required by ADR-0037, which mandates mTLS from UAT upward.

## Decision

Adopt **Red Hat OpenShift Service Mesh 3.4 driven by the Sail Operator** as the PayU mesh control plane, and roll it out in `payu-dev` with **opt-in, per-deployment sidecar injection** for the **24 app-tier services**. Dev remains **PERMISSIVE** at the mesh layer; **STRICT** is enforced from UAT upward. Dev has **no Istio ingress gateway** — the OpenShift shared router serves external traffic.

**Normative:**

1. The control plane is created by `sailoperator.io/v1` CRs — an `Istio` CR named `default` in `istio-system` (mesh root namespace) and an `IstioCNI` CR named `default` in `istio-cni`. `ServiceMeshControlPlane` / `ServiceMeshMemberRoll` are never used again.
2. Sidecar injection is **per deployment**, via the pod-template label `sidecar.istio.io/inject: "true"` in `infrastructure/workloads/base/<svc>/kustomization.yaml`. A namespace-level injection label is forbidden in shared namespaces.
3. `payu-dev` is **PERMISSIVE** (`PeerAuthentication` `permissive-mtls`, namespace-scoped, no selector). STRICT mTLS plus `AuthorizationPolicy` is mandatory from `payu-uat` upward (`payu-uat`, `payu-preprod`, `payu`); `payu-sit` stays PERMISSIVE.
4. Dev gets **no Istio ingress gateway**. External traffic enters through the OpenShift router.
5. The legacy Maistra-era prod-ingress manifests (`base/gateway.yaml`, `base/destination-rules.yaml`, `base/ratelimit-service.yaml`, `base/security-headers.yaml`, `base/control-plane.yaml`, `base/certs/`, `base/gateway-api/`) are **quarantined**: they are not referenced by `base/kustomization.yaml` and are not applied. They must be reworked for OSSM 3.4 before prod ingress is built on them.

## Rationale

Option 3 is the only option that makes the mTLS claim real without putting shared infrastructure at risk. Option 1 leaves the platform on an API that no longer exists; Option 2 puts the Envoy sidecar in front of the CNPG and Kafka pods that the whole platform depends on; Option 4 would break the dev debugging path and the health probes for a posture that dev is not required to hold.

The chosen path also matches the repository's existing documented standard: `docs/architecture/DEVSECOPS_ARCHITECTURE.md` §4.5.2 ("mTLS mandatory antar semua service di `payu-uat` ke atas") and `docs/architecture/ARCHITECTURE.md` §6.1 ("mTLS between all services (UAT+)"). Dev at PERMISSIVE with auto-mTLS is not a weakening of that standard — Istio's auto-mTLS upgrades mesh-to-mesh traffic to mutual TLS even under PERMISSIVE, which was verified live.

## Consequences

**Positive**:

- The mTLS claim in the architecture docs is now backed by a running control plane: `istiod` rolled out successfully, `istio-cni-node` DaemonSet 4/4 ready, `Istio` and `IstioCNI` CR status **Healthy**.
- 24 app-tier services are meshed in `payu-dev`; workload certificates are SPIFFE (`spiffe://payu.local/ns/payu-dev/sa/<service>`) and mesh-to-mesh traffic is mutual TLS — proven by `connection_security_policy="mutual_tls"` on `istio_requests_total` (23 `mutual_tls` + 2 `none` from non-mesh sources: the kubelet probe and the OpenShift router).
- Injection is per service, so CNPG / Strimzi / DataGrid / Keycloak / AMQ pods are provably unaffected, and each app service can be excluded or added independently.
- The Sail Operator path is the supported one for OSSM 3.x, so the control plane can be upgraded by changing `spec.version` on the `Istio` CR.
- Dev remains debuggable: PERMISSIVE preserves plain-text access while still giving service identity and mutual TLS between meshed peers.

**Negative**:

- The inject label lives in the **shared** `infrastructure/workloads/base/`, so `payu-sit`, `payu-uat`, `payu-preprod` and `payu-prod` overlays carry it too. It is inert until those namespaces get a control plane — but it **will** activate the moment their mesh is installed, which is the intent per ADR-0037.
- The legacy `infrastructure/platform/mesh/base/peer-authentication.yaml` still carries the prod/UAT-wide `mesh-wide-mtls` **STRICT** object in `istio-system`. A root-namespace STRICT policy binds **every** namespace in the mesh, so that object must never be applied on a cluster that also hosts `payu-dev` / `payu-sit` / `payu-uat`.
- **Native sidecar changes the pod shape.** Istio 1.30 injects the proxy as `spec.initContainers` (`istio-validation` + `istio-proxy` with `restartPolicy: Always`), so `spec.containers` still shows only `app`. Tooling or tests that assert "2 containers" give a false negative.
- **Envoy only intercepts declared ports.** A gRPC server on 9090 whose pod spec does not declare `containerPort: 9090` is silently blackholed once a sidecar is injected — the Service exposing 9090 is not enough. `containerPort: 9090` (name `grpc`) had to be added to `account-service` and `transaction-service`.
- The e2e health sweep returns **14/18** endpoints 200. The 4 that return 403 (`partner`, `transaction`, `compliance`, `dispute`) return `{"error_code":"ACCESS_DENIED","detail":"Insufficient permissions"}` with `response_code_details=via_upstream` — application RBAC for the seeded `customer1` user (roles KYC_VERIFIED / PREMIUM / USER), **not** a mesh fault.
- STRICT mTLS behaviour was deliberately **not** exercised in dev, so the STRICT path remains unverified end-to-end until the UAT environments are bootstrapped.

**Risks**:

- The 24 injection labels must be re-added to any new service; a forgotten label means a silently unmeshed service. Mitigation: injection label is part of the app-tier service definition and is auditable per `kustomization.yaml`.
- No Istio ingress gateway exists in dev, so Gateway API / gateway resource behaviour is untested there; prod ingress design is still open.
- ~~Kiali is **not deployed**, so the service graph is unavailable from the UI (metrics remain available via Prometheus).~~ **Amended 2026-10-21**: Kiali **is deployed** for `payu-dev` — `Kiali` CR `payu-kiali` + `OSSMConsole` CR `payu-ossmconsole` (both `kiali.io/v1alpha1` in `istio-system`), Route `kiali` → `kiali-istio-system.apps.payu.ocp.fajjjar.my.id`, OpenShift console plugin "OpenShift Service Mesh Console" registered. The service graph now renders the live topology (**31 nodes / 70 edges**). It needs the Envoy telemetry, which is scraped by the **cluster Prometheus** (`openshift-monitoring/k8s`) through the `payu-mesh-sidecars` / `payu-mesh-istiod` ServiceMonitors in `infrastructure/platform/mesh/telemetry/`; a user-workload Prometheus would have produced an empty graph. Residual: the console is scoped to `payu-dev` only and has no tracing backend (`server.observability.tracing.enabled: false`), so the trace view stays off.

> **Amended 2026-10-21** — two statements above went stale within the same day and are corrected here: the Kiali risk (the console is live for `payu-dev`, together with the Envoy scrape it depends on) and the follow-up list in Implementation Notes §5 (per-environment `PeerAuthentication` overlays, the console, and the mesh telemetry scrape). The decision itself is unchanged.

## Implementation Notes

1. **Manifest layout** (new, `infrastructure/platform/mesh/`):
   - `base/namespace.yaml` — namespaces `istio-system` (mesh root) + `istio-cni`.
   - `base/istio-cni.yaml` — `IstioCNI` CR `default` in `istio-cni`, `spec.version: v1.30.5`, `spec.namespace: istio-cni`. The operator requires the CR name `default`; the multus plugin publishes the `default/istio-cni` NetworkAttachmentDefinition so the redirect happens without a privileged init container.
   - `base/service-mesh.yaml` — `Istio` CR `default` in `istio-system`, `spec.version: v1.30.5`, `profile: default`, `updateStrategy: InPlace`. `v1.30.5` is the highest version in the operator's `spec.relatedImages` (list: v1.27.3, v1.27.5, v1.27.8, v1.27.9, v1.28.4, v1.28.5, v1.28.6, v1.28.8, v1.28.10, v1.30.1, v1.30.3, v1.30.4, v1.30.5).
   - `base/kustomization.yaml` — resources: those three only.
   - `overlays/payu-dev/kustomization.yaml` + `peer-authentication.yaml` — dev policy: `PeerAuthentication` `permissive-mtls` in `payu-dev`, `spec.mtls.mode: PERMISSIVE`, no selector. The overlay deliberately does **not** include `../../base`: a `namespace: payu-dev` transformer would move the namespaced `Istio` CR out of `istio-system`.
2. **Injection**: label `sidecar.istio.io/inject: "true"` added to the pod-template labels of the 24 app-tier services under `infrastructure/workloads/base/<svc>/kustomization.yaml` (the `labels.pairs` block, `includeSelectors: false`, `includeTemplates: true`). Deployment `spec.selector.matchLabels` and Service selectors are untouched. Meshed: account, analytics, api-portal, auth, backoffice, billing, cms, compliance, dispute, fx, gateway, integration, investment, kyc, lending, loan-origination-process, notification, partner, product-catalog, promotion, statement, support, transaction, wallet. Not meshed: the 5 external simulators (bi-fast, biller, dukcapil, qris, va) and `web-app`.
3. **Sidecar shape**: the proxy runs as `runAsUser`/`runAsGroup` = `1000759999` (OpenShift namespace UID range `1000750000/10000`), `runAsNonRoot=true`, `allowPrivilegeEscalation=false`, `readOnlyRootFilesystem=true`, `privileged=false`, capabilities dropped ALL — never the hardcoded `1337`, so it satisfies the `restricted-v2` SCC. Proxy resources: requests 100m CPU / 128Mi, limits 500m CPU / 256Mi.
4. **Verification performed** (cluster `payu-drrpz`, OpenShift 4.22.18, namespace `payu-dev`):
   - `oc apply --dry-run=server -k infrastructure/platform/mesh/base` → all 4 resources `unchanged` (no drift).
   - `./scripts/verify-overlay-drift.sh payu-dev` → `OK: no overlay drift in payu-dev` (exit 0), before and after the rollout.
   - All 24 deployments rolled out and Ready; inbound listeners account `[8080,9090]`, transaction `[8080,9090]`, wallet `[8080,9090]`, cms `[8080]`, billing `[8080]`.
   - Live gRPC call in the proxy access log: `path=/payu.account.AccountService/GetAccountsByUser upstream=outbound|9090||account-service.payu-dev.svc.cluster.local response_code=200`.
   - `scripts/e2e/health-check-all.sh` → 14/18 endpoints 200; the 4 remaining are application RBAC 403s, not mesh faults.
   - External path still works: `https://payu-dev.apps.payu.ocp.fajjjar.my.id/` → 200 (web-app, not meshed); `https://analytics-ws.apps.payu.ocp.fajjjar.my.id/` reaches the meshed analytics-service (proxy log `upstream=inbound|8080||`). The repository defines no OpenShift Route to `gateway-service`, so the literal "router → gateway-service" hop could not be exercised; the analytics-ws route was used to prove router → meshed-service still works.
   - Quota after the rollout: pods 43/150, requests.cpu 8650m/24, requests.memory 17472Mi/64Gi, limits.cpu 41750m/96, limits.memory 54528Mi/120Gi.
5. **Follow-ups** (tracked in `docs/roadmap/TODOS.md` as `MESH-001` … `MESH-005`): rework of the legacy Maistra-era prod-ingress manifests, quarantine of the root-namespace STRICT `PeerAuthentication`, STRICT + `AuthorizationPolicy` rollout for `payu-uat` / `payu-preprod` / `payu`, the prod/UAT Istio ingress gateway decision, and Kiali deployment for the service graph.
   - **Amended 2026-10-21**: `MESH-005` (Kiali) is **closed** — the console is live for `payu-dev` and its service graph renders the real topology (31 nodes / 70 edges); the residuals are that the console has no tracing backend and that it is scoped to `payu-dev` only. `MESH-002` (root-namespace STRICT) is **advanced** — the `PeerAuthentication` posture now lives in per-environment overlays (`overlays/payu-dev/` + `overlays/payu-sit/` PERMISSIVE; `overlays/payu-uat/` + `overlays/payu-preprod/` + `overlays/payu/` STRICT), all namespace-scoped, no selector, and **no** mesh-wide `PeerAuthentication` in `istio-system`; the legacy `base/peer-authentication.yaml` is annotated SUPERSEDED and stays out of the apply path. `MESH-003` is **advanced** — the STRICT `PeerAuthentication` is in the repo and applied for `payu-uat` / `payu-preprod`, but those environments are not bootstrapped so the STRICT path has no traffic to observe, and the `AuthorizationPolicy` half is still open. `MESH-001` (legacy prod-ingress manifests) and the prod side of `MESH-004` (prod gateway behind APIcast) remain open, as recorded in `docs/roadmap/TODOS.md`.
   - Later work of the same day: ADR-0073 (Istio ingress gateway and shared-domain ownership — the dev gateway behind the shared OpenShift router), plus the per-environment policy overlays, the Kiali console, and the mesh telemetry scrape. Their live state is documented in `docs/architecture/DEVSECOPS_ARCHITECTURE.md` §4.5.2, `docs/architecture/ARCHITECTURE.md` §7.2, and `docs/roadmap/PROGRESS.md`.

---

*Created via principal-architect — live evidence on cluster `payu-drrpz` (OCP 4.22.18), Sail Operator `servicemeshoperator3.v3.4.3`, Istio v1.30.5; refs ADR-0069 (4.22 baseline), ADR-0037 (gRPC governance), ADR-0012 (container standard), ADR-0034 (observability)*
