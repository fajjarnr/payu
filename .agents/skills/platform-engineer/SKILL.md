---
name: platform-engineer
description: PayU platform, SRE, and release engineering for OpenShift and Kubernetes workloads, GitOps, Argo CD, Tekton, Kustomize, Helm, container hardening, networking, observability, reliability, disaster recovery, and FinOps. Use when designing, deploying, debugging, or reviewing infrastructure and delivery changes; verify third-party APIs and CLI behavior with Context7 first.
---

# PayU Platform Engineer

Make the smallest safe infrastructure change. Treat Git as the deployment
source of truth, prefer existing repository patterns and operators, and never
patch production directly when the same change belongs in GitOps.

## Context7 documentation gate

Before writing or changing manifests, pipelines, charts, scripts, or commands
that use a third-party platform or tool:

1. Inspect the target overlay, installed operator/CRD, cluster version, and
   repository configuration. Determine the exact version instead of guessing.
2. Resolve the library or product in Context7. Prefer the official,
   high-reputation result, then query one concrete topic at a time.
3. Pin the query to the deployed version when indexed. Use the returned docs as
   the source of truth for API fields, CLI flags, defaults, and behavior.
4. If the exact version is not indexed, state the fallback and verify against
   the installed CRD, `oc explain`, `kubectl explain`, chart schema, or source
   before editing.
5. Re-resolve after changing an operator, chart, Kubernetes, OpenShift, Argo
   CD, Tekton, mesh, or observability version. Do not mix major-version APIs.

Resolve and query the relevant official sources for Kubernetes/OpenShift, Argo
CD/ApplicationSet, Tekton Pipelines/Triggers, Helm, Kustomize, Istio or Gateway
API, Strimzi, Vault/External Secrets, and observability operators. Context7 does
not replace inspection of PayU manifests or operator CRDs.

## Repository map

Start at the actual path, not a copied example:

```text
infrastructure/workloads/base/                 shared workload manifests
infrastructure/workloads/overlays/payu-*      dev, sit, uat, preprod, prod
infrastructure/platform/cicd/argocd/           Argo CD projects and applications
infrastructure/platform/cicd/tekton/           pipelines, tasks, runs, triggers
infrastructure/foundation/                     namespaces and cluster foundation
infrastructure/platform/{security,mesh,data,observability}
```

Read the relevant `kustomization.yaml`, base resources, overlay patches, RBAC,
secrets references, and existing tests before changing a workload. Verify the
real namespace and resource name; do not infer them from the service name.

## Delivery workflow

Use this sequence for changes that affect deployment:

1. Inventory dependencies, owners, blast radius, rollback, and required
   approvals.
2. Edit the smallest base or overlay that owns the behavior. Keep environment
   differences in overlays, not duplicated bases.
3. Render and statically validate manifests. Review the rendered diff.
4. Run server-side dry-run or `oc diff` against the target cluster when access
   exists; otherwise report that cluster validation is pending.
5. Commit and promote through Argo CD. Wait for health, rollout, and smoke
   checks at each environment.
6. Verify metrics, logs, events, probes, dependencies, and rollback readiness.

Never claim deployment success without command output or an Argo/cluster status.
Do not use `oc edit`, `kubectl apply` to bypass GitOps, `setenforce 0`, or
destructive cluster commands as a shortcut.

## Kubernetes and OpenShift manifests

Use the API versions supported by the target cluster and resolved documentation.
For current Kubernetes APIs, prefer the stable forms such as:

- `apps/v1` for Deployments and StatefulSets;
- `networking.k8s.io/v1` for NetworkPolicy and Ingress;
- `policy/v1` for PodDisruptionBudget;
- `autoscaling/v2` for HorizontalPodAutoscaler;
- `tekton.dev/v1` for current Tekton Pipeline, Task, PipelineRun, and TaskRun
  resources.

Do not carry forward deprecated `v1beta1` resources without verifying the
installed operator. Validate OpenShift-specific resources against the cluster's
CRDs. Use `Route`, Gateway API, or Ingress according to the existing platform
standard; do not introduce a second ingress model for one workload.

Every workload should have, as applicable:

- explicit CPU and memory requests/limits;
- startup, readiness, and liveness probes with realistic thresholds;
- a `Service` selecting the exact pod labels;
- a PDB for replicated production workloads;
- HPA only when requests, metrics, stabilization, and max capacity are defined;
- NetworkPolicy with explicit DNS, ingress, and dependency egress;
- stable `app.kubernetes.io/*` labels, owner, environment, version, and cost
  attribution metadata.

Use native Kubernetes/OpenShift controllers for rollout, scaling, health, and
policy. Avoid shell loops or custom controllers for behavior the platform already
provides.

## GitOps with Argo CD

Keep Application, ApplicationSet, AppProject, sync-wave, and policy manifests
under `infrastructure/platform/cicd/argocd/`.

- Restrict each AppProject to approved source repositories and destination
  clusters/namespaces. Avoid `*` in production permissions.
- Use ApplicationSet templates with `goTemplate: true` and
  `goTemplateOptions: [missingkey=error]` when templating is required.
- Use generators that reflect the repository's environment layout; current PayU
  overlays use `payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, and
  `payu-prod`.
- Enable `prune` and `selfHeal` only with an explicit ownership decision. Keep
  `allowEmpty: false` so a bad render cannot wipe an application.
- Use sync waves or progressive sync for dependencies and environment
  promotion. Gate production with a sync window, approval, or both.
- Configure bounded retry backoff for transient sync failures. Do not retry
  validation, authorization, or broken-manifest errors indefinitely.
- Roll back by reverting the Git change or using the approved Argo rollback
  flow. Record why the rollback happened and verify data compatibility.

For progressive delivery, use the installed Argo Rollouts or mesh/Gateway API
capability only after resolving its exact API and health-analysis behavior.

## Tekton pipelines

Define pipelines and tasks under `infrastructure/platform/cicd/tekton/` and use
the current `tekton.dev/v1` API where the installed Tekton release supports it.
Use `PipelineRun` with `pipelineRef`, explicit parameters, named workspaces,
bounded timeouts/retries, and a least-privilege `serviceAccountName`.

Keep the supply-chain stages explicit and fail closed:

```text
checkout -> secret scan -> SAST/SCA -> tests -> build
         -> image scan -> SBOM -> policy/RHACS -> sign -> publish
         -> deploy through Argo -> smoke/contract/load gates
```

- Pin task and step images by trusted digest or controlled immutable tag.
- Run steps as non-root with bounded resources and no privilege escalation.
- Pass credentials through Vault/External Secrets or Tekton-bound secrets; never
  put tokens, kubeconfigs, or registry passwords in YAML or logs.
- Use workspaces for source and artifacts; do not depend on an implicit shared
  filesystem or deprecated PipelineResources.
- Keep deployment credentials separate from build credentials and restrict RBAC
  to the namespace and resources that the pipeline owns.
- Use Tekton Triggers or Pipelines-as-Code only after verifying the installed
  trigger API and webhook authentication configuration.

## Images and container hardening

Build with the repository's UBI9/Podman pattern and preserve enough build context
for parent POMs, shared modules, and lockfiles. Prefer a multi-stage build and a
small UBI9 runtime image. Use pre-built artifacts only when the build has already
run the same tests and scans.

Require:

- internal application port `8080` for PayU services;
- immutable image tags matching the Git release tag, preferably referenced by
  digest;
- non-root execution (`UID 1001` where the image and cluster policy support it;
  otherwise use OpenShift's assigned UID range);
- `runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem:
  true`, `seccompProfile.type: RuntimeDefault`, and dropped `ALL` capabilities;
- explicit writable `emptyDir` mounts only where the application needs `/tmp` or
  another transient path;
- OCI labels, provenance, SBOM, vulnerability scan, and image signature.

Use Kubernetes probes instead of relying on an image `HEALTHCHECK`. Match probe
paths and startup delays to the service. Do not install packages that already
exist in the selected UBI image; verify the image contents before adding a
healthcheck binary. Never weaken SELinux or add a privileged SCC to solve a file
permission problem—inspect the audit event, UID/GID, volume, and label first.

## Kustomize and Helm

Use Kustomize for the repository's existing base/overlay layout. Render with the
same version used by CI and verify the output:

```bash
kustomize build infrastructure/workloads/overlays/payu-dev
```

Use Helm only when an existing chart or platform contract requires it. Validate
`Chart.yaml`, `values.schema.json`, rendered templates, and environment values;
do not add Helm merely to template a few Kubernetes fields. Do not mix Helm and
Kustomize ownership of the same resource without a documented boundary.

## Networking and security

- Default to least-privilege NetworkPolicy. Allow only required ingress,
  namespace traffic, DNS, mesh control-plane traffic, and named dependencies.
- Use TLS at routes and strict mTLS/authorization policies when the installed
  mesh supports them. Verify service accounts and principals instead of allowing
  an entire namespace by label alone.
- Store secrets in Vault, External Secrets, or the platform secret manager. Git
  may contain references and encrypted material only when the repository policy
  explicitly permits it.
- Keep RBAC namespaced and minimal. Avoid `cluster-admin` in workloads,
  pipelines, service accounts, and local development manifests.
- Treat routes, webhooks, registries, admission policies, and operator
  credentials as trust boundaries. Validate inputs and audit changes.

## SRE, observability, and reliability

Define an SLI, SLO, owner, alert threshold, and runbook for each production
service. Instrument the existing platform stack for logs, metrics, traces, and
Kubernetes events; do not add a second telemetry system without a measured need.

Check:

- availability, latency, error rate, saturation, queue lag, and dependency
  health;
- structured logs with correlation/request IDs and no secrets or PII;
- alerts that page only on actionable symptoms and include a runbook link;
- HPA, PDB, resource quotas, limit ranges, and capacity headroom;
- rollout health, startup time, probe failures, crash loops, and OOM kills.

Run chaos and load tests only in approved non-production environments. Make the
test reversible, bound blast radius, and capture evidence. For financial flows,
verify no duplicate posting, event loss, or unsafe retry occurs during failure.

## Disaster recovery and FinOps

Define RPO/RTO per dependency, backup ownership, replication mode, failover
steps, and data reconciliation. Test restore and failover on a schedule; a
multi-region diagram is not evidence of recoverability. Keep the primary/standby
mode of PostgreSQL, Kafka, Redis, and object storage explicit and verified.

Right-size requests and limits from observed usage, enforce quotas, and apply
owner/environment/cost-center labels. Use VPA recommendations or OpenCost data
before changing production resources; do not enable automatic eviction or
resource mutation without an availability review.

## Release and rollback checklist

- [ ] Git diff is limited to the owning base/overlay or platform component.
- [ ] API versions and fields were verified with Context7 and the installed CRD.
- [ ] Rendered manifests and schema/lint checks pass.
- [ ] Image is immutable, scanned, signed, and tagged to the Git release.
- [ ] Secrets, RBAC, NetworkPolicy, probes, resources, PDB, and HPA are covered.
- [ ] Argo/Tekton permissions and production gates are explicit.
- [ ] Smoke, contract, migration, and rollback checks are defined.
- [ ] Post-deploy health and rollback evidence is captured.

## References

Read only the reference that matches the task:

- [Infrastructure patterns](./references/INFRASTRUCTURE_PATTERNS.md)
- [Deployment patterns](./references/DEPLOYMENT_PATTERNS.md)
- [Argo CD GitOps](./references/argocd-gitops.md)
- [CI/CD pipeline guide](./references/cicd_pipeline_guide.md)
- [Infrastructure as code](./references/infrastructure_as_code.md)
- [Deployment strategies](./references/deployment_strategies.md)
- [Istio traffic management](./references/istio-traffic-management.md)
- [Release engineering](./references/releases/release-engineering.md)
- [SRE practices](./references/sre/sre-practices.md)
- [Incident playbooks](./references/sre/incident-playbooks.md)
- [Disaster recovery](./references/sre/disaster-recovery.md)

Treat bundled scripts as helpers, not authority. Read and test them before use;
do not assume a generator or deployment script is complete or safe for a live
cluster.
