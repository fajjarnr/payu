# ADR-0066: Polyrepo Per-Service Pipeline — DevSecOps Standard (Monorepo → Service Repos)

**Status**: Accepted  
**Date**: 2026-08-22  
**Deciders**: Platform Engineering, Core Banking Engineering  
**Relates to**: ADR-0045 (GitOps CD), ADR-0042 (ShedLock), `docs/architecture/DEVSECOPS_ARCHITECTURE.md:1` (PRD 1.4.0), `TODOS.md:152` DEVSECOPS-017

---

## Context

Saat ini `1 repo` `backend/pom.xml` (`25 microservices` + `5 simulators` + `shared` 9 starters, `buildah-task.yaml` `maven-java21-task.yaml` `gitleaks/semgrep/trivy/grype/syft/cosign/rhacs/zap/schemathesis/k6/litmus/kraken` — 27 Tekton Tasks di `infrastructure/platform/cicd/tekton/tasks/`) → `ArgoCD ApplicationSet` 22 Apps (SIT/UAT/preprod) → `payu-dev` 23/23 Running. Fondasi kuat per `DEVSECOPS_ARCHITECTURE.md:4` (6 stages `Source→Build→Test→Deploy→Runtime→Observability`, OWASP Top10 2025/API 2023, SLSA L2+, PCI-DSS v4.0, namespace `payu-dev|sit|uat|preprod|payu` promote by digest).

Target bank/e-wallet global (Wise/JPM `Backstage catalog` + `3scale` + `DORA` ICT resilience) adalah **polyrepo per service** — `fajjarnr/payu-account-service`, `payu-transaction-service`, ... masing-masing punya `.tekton/pipeline.yaml`, `.argocd/application.yaml`, `Containerfile` UBI9, `SECURITY.md`. Monorepo build 23 concurrent `PipelineRuns` sudah `ExceededNodeResources` (affinity 97% CPU) — `kyc/analytics/dispute...` 7 `canceled`.

## Decision Drivers

* **Blast radius**: polyrepo isolate build failure per service, tidak block 22 service lain.
* **Ownership**: `CODEOWNERS` per service repo, `CODEOWNERS` global tidak scale.
* **DORA**: deploy frequency per service independent.
* **Compliance**: `SLSA` attestation + `Rekor` per service artifact, bukan monolith digest.

## Considered Options

### Option A — Stay monorepo + path-filtered Trigger `when: changed-service` (status quo)

* **Pros**: 1 PR, shared `pom.xml` version bump atomik.
* **Cons**: `Backend/pom.xml` `T 1C` parallel `T` lock, 23-way contention, ArgoCD sync 22 apps blast.

### Option B — Polyrepo per service + template repo + centralized Tekton Catalog (chosen)

* **Pros**: independent SHA + digest + `gitops-writeback` per service, `ApplicationSet` per service across `sit/uat/preprod/payu`, Vault path `payu/<env>/<service>/*`.
* **Cons**: 29 repos to maintain — mitigated via `payu-service-template` (`.opencode/skills`, `Containerfile.runtime`, `Task` catalog git submodule).

### Option C — Monorepo with Bazel + remote cache

* **Pros**: build cache.
* **Cons**: heavy migration, not OpenShift native.

## Decision

**Option B — Polyrepo per service (template-driven).**

1. **Repo layout per service**: `service-name/` → `src/`, `Containerfile.runtime` UBI9 UID 1001 `readOnly FS`, `pom.xml` (version `1.0.0-SNAPSHOT` `payu-backend-parent` → service parent), `.tekton/` `pipeline.yaml` + `trigger.yaml` (`GitHub`/`GitLab` webhook `el-github-listener` `ClusterTriggerBinding` `payu-service`), `.argocd/` `application.yaml` + `kustomize` `base/overlays/dev|sit|uat|preprod|prod`, `.github/CODEOWNERS`.
2. **Pipeline 6 stages** (reuse `DEVSECOPS_ARCHITECTURE.md:180`): `Stage1 gitleaks+trufflehog+semgrep(SpotBugs/FindSecBugs Java, Bandit Python)`, `Stage2 Buildah+Syft+Grype+Trivy+CVE gate`, `Stage3 ZAP baseline (dev) + Schemathesis (sit) + Dredd contract +ArchUnit`, `Stage4 ArgoCD sync-wait (prune/selfHeal true) + cosign verify admission (Kyverno/ACS)`, `Stage5 OSSM mTLS PeerAuth STRICT (>uat) + Falco skip (RHCOS+RHACS)`, `Stage6 LokiStack+Wazuh SIEM 12m retention + Grafana`. `PipelineRun` `results.tekton.dev` 365d (PCI Req10).
3. **Shared catalog**: `infrastructure/platform/cicd/tekton/catalog/` git tag `payu-catalog-v1` — `buildah-task`, `gitleaks-task`, `semgrep-task`, `trivy-task`, `cosign-task`, `argocd-sync-task`, `gitops-writeback-task` — per-service `pipeline.yaml` `taskRef: catalog#v1`.
4. **Namespace promotion**: `Applicationset-payu.yaml` per service generates `payu-dev|sit|uat|preprod|payu` Applications from single repo `infrastructure/workloads/<service>/overlays/<env>` — promote by digest `kustomize edit set image <service>@sha256:digest` via `Tekton gitops-writeback` + `Argo Image Updater` disabled (no write-back bypass).
5. **Secrets**: per-service `VaultStaticSecret` `payu/<env>/<service>/db-credentials` via `VSO` `rolloutRestartTarget` (ADR-0044), `ESO` `ClusterSecretStore payu-vault` shared — no monorepo `db-credentials` single secret.
6. **Migration**: `payu-service-template` → `30` repos (`cookiecutter`); monorepo `backend/` frozen as `payu-monorepo-archive` after `payu-account-service` first polyrepo ships.

## Consequences

**Positive**: per-service deploy `MTTR <30m` independent, build queue 1 vs 23, `CODEOWNERS` per service.
**Negative**: 29 `PipelineRun` concurrent still need `Tekton` resource quota + `LimitRange` per `DEVSECOPS_ARCHITECTURE.md:531`.

## Implementation Notes

* Template repo `payu-service-template` contains `.opencode/skills`, `Containerfile.runtime` `ubi9`, `.tekton/pipeline.yaml` `when: - input: $(body.head_commit.message) operator: contains value: \"service: <name>\"` removed — polyrepo trigger always runs.
* `TODOS.md` `DEVSECOPS-017` split per service `PIPELINE-HARDEN-<service>`.

---
*Created for polyrepo split — pondasi kuat prerequisite per user request — refs `DEVSECOPS_ARCHITECTURE.md:4` + CodeGraph `buildah-task.yaml`, `argocd-sync-task.yaml`*
