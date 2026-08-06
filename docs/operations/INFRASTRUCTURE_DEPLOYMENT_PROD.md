# PayU Production Deployment Runbook

This runbook covers the `payu` production namespace. Read the [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) and the [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md).

## Hard gates

Production is not a lab sync with a different namespace. A release is blocked
until all of these are true:

- CAB and CISO approval, maintenance window, owner, and rollback digest exist.
- The image is promoted by immutable digest and its signature/provenance passes RHACS.
- Vault HA, VSO sync, production storage, backup/restore evidence, and monitoring are healthy.
- Production workloads use Argo Rollouts with a tested health analysis and automatic rollback; a plain Deployment is not a production canary.
- ArgoCD sync window is open. Do not bypass it with direct `oc apply` or a break-glass command without the approved incident record.

## Contract

| Item | Value |
|:---|:---|
| Namespace | `payu` |
| Workload overlay | `infrastructure/workloads/overlays/payu-prod` |
| Data overlay | `infrastructure/platform/data/overlays/prod` |
| Messaging overlay | `infrastructure/platform/messaging/overlays/prod` |
| Strategy | Argo Rollouts blue/green first; canary only with metrics evidence |
| Security | mTLS/zero-trust, RHACS admission, Vault/VSO, production backup and audit |

## Preflight and promotion

```bash
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/data/overlays/prod
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/messaging/overlays/prod
rtk oc apply --server-side --dry-run=server -k infrastructure/workloads/overlays/payu-prod
rtk oc get rollout -n payu
rtk oc get applications.argoproj.io -n openshift-gitops | rtk rg 'payu'
rtk oc get vaultstaticsecret -n payu
rtk oc get networkpolicy -n payu
```

Only after the hard gates pass, start the GitOps pipeline with
`environment=prod`, `push-changes=true`, and the approved digest. The pipeline
must fail closed when `push-changes` is false; production sync is never forced.

## Acceptance

```bash
rtk oc get applications.argoproj.io -n openshift-gitops
rtk oc get rollout -n payu
rtk oc get analysisrun -n payu
rtk oc get pods -n payu
rtk oc get events -n payu --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Promote traffic only after the Rollout analysis is healthy. Save Argo history,
Rollout/AnalysisRun results, RHACS evidence, digest, approvals, and monitoring
links in the change record.

## Rollback

Use the Rollout abort/rollback path or revert the Git write-back commit, then
let ArgoCD reconcile. Preserve the failed ReplicaSet and evidence until the
incident review is complete. Never delete production PVCs or stateful CRs as a
rollback shortcut.
