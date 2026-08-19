# ADR-0045: GitOps Continuous Delivery, Infrastructure as Code & Supply Chain Security

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Platform Engineer, Cybersecurity Architect  
**Relates to**: DEVSECOPS-017, DEPLOY-011, ADR-0031  

---

## Context

PayU uses `ArgoCD` 18 apps (`SIT/UAT/preprod` live lab `cluster-nkk8q`), `Kustomize/Helm`, `Tekton` (`Results 365d`, `Chains SLSA`), `CNPG Barman`. Gap: `prod` sync window, `RHTAS CNPG archive failure`, `Chains SLSA/Rekor` fresh evidence per `DEVSECOPS-017`.

Need `GitOps` + `IaC` + `supply-chain` (SLSA/Rekor, signed admission) standard.

## Decision Drivers

* **Git as SSOT** — declarative `infrastructure/` + `backend/*/k8s`.
* **Supply-chain** — SLSA `Buildah` digest + `Rekor` + `admission Enforce`.
* **Promotion** — `SIT→UAT→preprod→prod` via `ArgoCD` `ApplicationSet`.

## Considered Options

### Option 1 — ArgoCD GitOps + Tekton + Chains/Rekor + Kustomize (dipilih)

Pros: existing, `Results HA`, `CNPG` integrated. Cons: `ArgoCD` cred via `ESO Vault` needed (DEVSECOPS-017).

### Option 2 — Flux / manual `oc apply`

Pros: alt. Cons: no Argo UI — rejected.

## Decision

**ArgoCD GitOps + Tekton Results + Chains SLSA/Rekor + Kustomize + signed admission.**

* Repo: `infrastructure/platform/argocd/applicationset-payu.yaml` (`SIT/UAT/preprod/prod` targets, `syncWindow` prod).
* `Tekton` `Pipeline` `buildah` → `push` digest → `Chains` `SLSA` → `Rekor` transparency log → `Results HA PG` `365d`.
* `Vault`-backed `ArgoCD` repo cred via `ESO` (DEVSECOPS-017).
* `Kyverno`/`Gatekeeper` `signed-image Enforce` (31 images).
* `CNPG` `barmanObjectStore S3` `archive_timeout=60s` + `VolumeSnapshot` daily (ADR-0031), `RHTAS` fix `readyInstances=3`.
* Promotion: `image digest` `Kustomize` `prod` `syncWindow` + `drift` alert `Slack/PagerDuty` via Vault.

## Consequences

**Positive**: auditable deploy, SLSA evidence, drift alert.

**Negative**: `ArgoCD` Vault cred migration — tracked `DEVSECOPS-017`.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Argo | `infrastructure/platform/argocd/*` |
| 2 | Tekton | `tekton/pipelines/buildah.yaml` |
| 3 | Chains | `tekton/chains/config` |

**Verification**: `argocd app sync` `Synced Healthy`; `rekor search --artifact` shows entry; `signed admission` blocks unsigned image.

---
*Created for DEVSECOPS-017 — implementasi wajib refer ADR ini.*
