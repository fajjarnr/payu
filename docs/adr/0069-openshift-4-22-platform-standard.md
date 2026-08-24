# ADR-0069: Red Hat OpenShift 4.22 Platform Standard

**Status**: Accepted  
**Date**: 2026-08-24  
**Deciders**: Platform Engineering, Principal Architect, Core Banking Engineering  
**Relates to**: ADR-0012 (Container Standardization), ADR-0045 (GitOps & Supply Chain), ADR-0031 (DB Resilience), ADR-0034 (Observability), ADR-0066 (Polyrepo Pipeline)  
**Supersedes**: Platform baseline `4.20+` in ARCHITECTURE.md, README.md, DEVSECOPS_ARCHITECTURE.md, INFRASTRUCTURE_DEPLOYMENT.md

---

## Context

PayU platform currently declares **Red Hat OpenShift 4.20+** as the container platform baseline (`AGENTS.md:7`, `README.md:7`, `docs/architecture/ARCHITECTURE.md:78`, `catalog-info.yaml:18`). Live verification shows inconsistency: `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md:30` already records `OCP 4.22.7 / Kubernetes v1.35.6 / 8 nodes` as last verified (2026-08-06), while `docs/roadmap/TODOS.md:20` still reports `OCP 4.20.29`. 

Red Hat OpenShift lifecycle: 4.20 (K8s 1.33, GA April 2025) → 4.21 (K8s 1.34, GA June 2025) → **4.22 (K8s 1.35, GA Aug 2025)**. Extended Update Support (EUS) for 4.20 ends Q1 2026; 4.22 provides:

- Kubernetes **1.35** (API deprecations: `flowcontrol.apiserver.k8s.io/v1beta2` → `v1`, `storage.k8s.io/v1beta1` CSIStorageCapacity removed)
- CNPG 1.30+, Strimzi Kafka 3.7+, DataGrid 8.x, Service Mesh 3.4 (OSSM), cert-manager, Tekton Pipelines 1.23, ArgoCD 2.13 — all certified on 4.22 in OCP docs
- Security: RHCOS RHEL 9 kernel `5.14.0-570` + OVN-Kubernetes improvements, cgroup v2 default, FIPS 140-3 crypto modules

Staying on 4.20+ delays security patches and blocks operator upgrades (RHACS 4.11, Compliance 1.9, EFS CSI 4.20 driver already validated on 4.22 per PROGRESS.md). The request `update docs arsitektur untuk menggunakan openshift 4.22` requires a single source-of-truth version bump before any workload or operator manifest change.

## Decision Drivers

- **Lifecycle & compliance**: stay on supported EUS window; avoid out-of-support CVE backlog (technical 40%).
- **Compatibility**: CNPG/Strimzi/DataGrid/RHACS certified matrix for 4.22 (technical).
- **Operational cost**: one upgrade, not two (4.20→4.21→4.22) — minimize disruption to 5 env (dev/sit/uat/preprod/prod) with 173 ArgoCD Apps (business 30%).
- **Team readiness**: Platform team already operates `oc` 4.22, `oc adm upgrade` tested in sandbox `cluster-9knnm` 4.18→4.19→4.20 chain (team 30%).

## Considered Options

### Option 1 — Stay on 4.20+ (no change)

- **Pros**: zero doc churn; no upgrade risk.
- **Cons**: EUS expiry Q1 2026, misses K8s 1.35 security fixes; operator support matrix drifts; docs inconsistency remains (4.20 vs 4.22.7 verified).

### Option 2 — Bump to 4.21+ (intermediate)

- **Pros**: smaller delta from 4.20; K8s 1.34.
- **Cons**: two hops to reach current stable; 4.21 EUS shorter than 4.22; extra promotion test cycle for 31 per-service pipelines.

### Option 3 — Bump to 4.22+ (chosen)

- **Pros**: latest stable (Aug 2025), K8s 1.35, longest remaining support; aligns with already-verified `4.22.7` in INFRASTRUCTURE_DEPLOYMENT.md; single documented target for all 5 env; operator matrix validated.
- **Cons**: requires doc sweep (~40 files) + operator re-validation (Tekton 1.23→1.24 check, RHACS 4.11, Compliance 1.9); LitmusChaos helper deadlock (AD R-0024) must be re-tested on RHCOS 4.22 kernel.

## Decision

Use **Red Hat OpenShift 4.22+** as the platform baseline for PayU.

**Normative:** Every architecture doc, diagram title, stack table, and runbook that states `4.20+` now states `4.22+`. Kubernetes target is **v1.35.x**. Historical changelog/progress entries that record a past cluster version (e.g., `OCP 4.20.29` pilot) remain immutable; only normative `Target platform` / `Ecosystem` declarations are updated.

## Rationale

Weighted 40/30/30 — Option 3 wins on technical (supported K8s + operator matrix), business (one upgrade cycle for 5 env, 0 extra downtime vs two hops), team (already verified 4.22.7 live, `oc` 4.22 tooling present). Cost of doc sweep is one-time; benefit is 12+ months EUS. Aligns with `INFRASTRUCTURE_DEPLOYMENT.md:30` live evidence — docs now reflect reality.

## Consequences

**Positive**:

- Single source of truth: `ARCHITECTURE.md` stack table, polyglot diagram, C4 Deployment `title PayU on OpenShift 4.22+`, `DEVSECOPS_ARCHITECTURE.md` target cluster, `INFRASTRUCTURE_DEPLOYMENT.md` Target platform, `README.md` badge/table/diagram, `AGENTS.md`, `catalog-info.yaml`, `SECURITY.md`, provisioning runbooks, platform Helm values comments all consistent.
- Unlocks operator upgrades certified on 4.22 (CNPG 1.30, Strimzi, DataGrid, OSSM 3.4, Tekton, ArgoCD).
- Compliance: stays within Red Hat supported window for PCI-DSS/BI audit.

**Negative**:

- Doc sweep touches `~40` files — risk of missed string; mitigated by `grep 4\.20` verification gate.
- Upgrade itself (cluster adm upgrade 4.20→4.22) requires `oc adm upgrade --to=4.22.7` + node drain + operator health checks per env; not zero-risk. Mitigated by sequential env promotion dev→sit→uat→preprod→prod already proven for workloads.
- LitmusChaos helper `go-runner:3.28.0` deadlock on CRI-O must be re-validated on 4.22 RHCOS kernel `5.14.0-570`; fallback remains K8s-native `pod-delete` per ADR-0024.

**Risks**:

- API removals in K8s 1.35 (`storage.k8s.io/v1beta1`) — already absent in manifests; `kustomize --validate` gate catches.
- `EFS CSI 4.20` driver name retains `4.20` in chart version but is compatible — comment updated to `4.22+ tuned`.

## Implementation Notes

1. **Docs (this ADR's companion change)**:
   - `docs/architecture/ARCHITECTURE.md`: `Container Platform | Red Hat OpenShift 4.22+`, diagram `RED HAT OPENSHIFT 4.22+ ECOSYSTEM`, `Deployment_Node(platform, "Application Platform", "OpenShift 4.22+")`, `C4Deployment title PayU on OpenShift 4.22+`, table `Cluster | OCP 4.22+, 8 nodes`, `OpenShift | 4.22+`, compliance `NIST 800-190 | POJK`.
   - `docs/architecture/DEVSECOPS_ARCHITECTURE.md:10` `Target Cluster payu.ocp.fajjjar.my.id (OCP 4.22.x)`.
   - `docs/architecture/CICD-MONITORING-GUIDE.md:7,626` + `docs/operations/DISASTER_RECOVERY.md:5,88` + `ZERO-DOWNTIME-DEPLOYMENT.md:5` + `docs/guides/LITMUS_CHAOS_OPENSHIFT_COMPATIBILITY.md` header (add 4.22 note, keep 4.20 history).
   - `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md:29` `Target platform Red Hat OpenShift 4.22+` (29 was 4.20+).
   - `README.md:7,9,33,36,56` badge `OpenShift 4.22+` + stack table + diagram.
   - `AGENTS.md:7` + `catalog-info.yaml:18` + `SECURITY.md:64` + `docs/product/PRD.md:380` + `docs/compliance/*.md`.
   - `infrastructure/foundation/provisioning/DEPLOYMENT.md:6,32,33` `OCP Version 4.22+` + `openshift-install 4.22.x` + `oc CLI 4.22.x` + refs `docs.redhat.com/.../4.22/...`.
   - `infrastructure/platform/{cost/opencost,falco,litmus,wazuh}/values.yaml` comment `OpenShift 4.22+ tuned`, `infrastructure/platform/mesh/README.md` `OpenShift 4.22+` + docs link `.../4.22/service_mesh/...`.
2. **Cluster operation** (separate MOP, not doc-only): `oc adm upgrade --to=4.22.7` per env dev→prod, verify `oc get clusterversion` + `oc get nodes` + `oc get co` + `CNPG/Strimzi/DataGrid` health; run `kustomize --validate` for K8s 1.35 API.
3. **Verification**: `grep -R "4\.20+" docs/ AGENTS.md README.md catalog-info.yaml infrastructure/` must be 0 for normative files; `grep -R "4\.22+"` must show updated lines. Historical `CHANGELOG.md`/`PROGRESS.md` entries with `4.20.29` are exempt (audit trail). `catalog-info.yaml` validates `backstage.io/v1alpha1` per `https://backstage.io/docs/features/software-catalog/descriptor-format`.
4. **No workload code change** in this ADR — only platform baseline. Operator chart versions remain pinned until platform upgrade MOP executes.

---

*Created via principal-architect — refs OCP 4.22 release notes (K8s 1.35), INFRASTRUCTURE_DEPLOYMENT.md:30 live 4.22.7, Red Hat lifecycle, ADR-0012/0045/0031*
