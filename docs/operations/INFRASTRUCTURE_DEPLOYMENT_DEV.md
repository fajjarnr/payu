# PayU Development Deployment Runbook

This runbook covers the `payu-dev` integration environment. Read the [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) first and keep the [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) as the governing contract.

## Contract

| Item | Value |
|:---|:---|
| Namespace | `payu-dev` |
| Workload overlay | `infrastructure/workloads/overlays/payu-dev` |
| Data overlay | `infrastructure/platform/data/overlays/dev` |
| Messaging overlay | `infrastructure/platform/messaging/overlays/payu-dev` |
| Deployment mode | Automatic after merge to the development branch; GitOps remains authoritative |
| Cache | Plain Hot Rod, endpoint authentication disabled; this is dev-only |

## Apply and verify

```bash
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/data/overlays/dev
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/messaging/overlays/payu-dev
rtk oc apply --server-side --dry-run=server -k infrastructure/workloads/overlays/payu-dev

rtk oc apply --server-side -k infrastructure/platform/data/overlays/dev
rtk oc apply --server-side -k infrastructure/platform/messaging/overlays/payu-dev
rtk oc apply --server-side -k infrastructure/workloads/overlays/payu-dev
```

Run the CI gates before requesting promotion:

```bash
rtk oc create -f infrastructure/platform/cicd/tekton/pipeline-runs/build-service-example.yaml
rtk oc create -f infrastructure/platform/cicd/tekton/pipeline-runs/test-service-example.yaml
rtk oc get pipelinerun -n payu-cicd -l environment=dev
```

Required evidence is a successful fetch, unit, architecture, integration,
security, SAST/SCA, image build, and dev smoke/DAST run. Do not promote a tag;
record the immutable `sha256:` digest produced by the build.

## Acceptance

```bash
rtk oc get cluster.postgresql.cnpg.io payu-database -n payu-dev
rtk oc get kafka payu-kafka -n payu-dev
rtk oc get infinispan payu-cache -n payu-dev
rtk oc get deployments,pods -n payu-dev
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -40
```

Stop if an infrastructure CR is not ready, a changed workload is in
CrashLoopBackOff, or the test PipelineRun is not `Succeeded`. Promotion to SIT
requires the build digest and a clean evidence bundle.

## Rollback

Revert the Git commit that changed the overlay and let ArgoCD reconcile. Do not
use `oc rollout undo` as the final rollback because it creates Git drift.
