# Tekton Results durable backend decision

Status verified 2026-08-06: the Tekton Results operator, API, watcher, and
365-day retention policy are Ready, but the live `TektonResult` still has
`is_external_db: false` and uses the operator-managed PostgreSQL deployment.

The CNPG `payu-tekton-results` Database object exists in `payu-dev`, but it is
not connected to Tekton Results. The expected `tekton-results-db` Secret is also
not present; the operator-managed `tekton-results-postgres` Secret is not a
production durable-secret contract.

Do not patch `TektonResult` directly: it is owned by `TektonConfig` and the
operator reverts direct edits. The production migration remains open until a
dedicated Vault-backed database role/Secret exists, the database endpoint is
reachable from `openshift-pipelines`, and the migration is verified with live
API records plus a restore test.


## Decision 2026-08-28 — Single-Instance Tolerance (CICD-RESULTS-001 CLOSED 1.18.52)

- **Current**: `TektonConfig.spec.result.is_external_db: false` + `StatefulSet tekton-results-postgres` 1 replica — fragile during node rotation (evidence: `tekton-results-postgres statefulset is not ready` during 4 worker `SchedulingDisabled` 2026-08-24/25, `error upserting record ... dial tcp :5432 connection refused` on `GetResult`, `results.tekton.dev/taskrun` finalizer stuck, manual strip 2026-08-25).
- **Impact**: `PipelineRun`/`TaskRun` execution **not blocked** (`gateway-service/web-app 1.18.46 Completed 15/15` after rotation); history in `tekton-results-api` may have gaps during rotation, but `tkn` + `oc get pipelinerun` still show `Succeeded` via etcd.
- **HA option**: CNPG `Database payu-tekton-results` `tekton_results` on `Cluster payu-database` (3/3 Healthy, `barman-cloud 1/1` `ObjectStore 5/5` `ContinuousArchiving True` `RPO=0`) + `TektonConfig.spec.result.is_external_db: true` + `Secret tekton-results-db` (Vault-backed). Requires: `VaultStaticSecret` for `payu-tekton-results` role, `Secret` creation, `TektonConfig` patch (operator owns `TektonResult`, direct `oc patch TektonResult` reverted), live API + restore test.
- **Decision**: **Keep single-instance** with tolerance documentation; **defer HA** until Vault HA + restore test. Rationale: `ponytail` — single-instance + `scripts/verify-tekton-results.sh` + `scripts/cleanup-tekton-results-finalizer.sh` (manual `oc patch taskrun ... finalizers:null`) costs 0, while CNPG external adds Vault + Secret + migration complexity for `payu-dev` history (not critical path). Re-evaluate trigger: if `oc get events -n openshift-pipelines | grep tekton-results-postgres` shows `connection refused` >1% of `PipelineRun` records, or rotation >1x/quarter causes >12h postgres downtime, or `payu-cicd` history loss blocks audit.

- **Verification**: `oc get statefulset -n openshift-pipelines tekton-results-postgres` `1/1 Ready`, `oc get pod -n openshift-pipelines tekton-results-postgres-0` `1/1 Running`, `oc get tektonconfig config -o jsonpath {.spec.result.is_external_db}` `false`, `scripts/verify-tekton-results.sh` `1/1 Ready` locally `kustomize build` 0 error.
- **Runbook**: `scripts/verify-tekton-results.sh --fix-finalizers` to strip stuck finalizers (as done 2026-08-25 `TaskRun` manual `oc patch`); monitor `oc get events -n openshift-pipelines --field-selector reason=FailedCreate` for `tekton-results-postgres`.


