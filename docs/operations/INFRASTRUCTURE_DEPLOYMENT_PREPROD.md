# PayU Preprod Deployment Runbook

This runbook covers `payu-preprod`, the production mirror and final resilience
gate. Read the [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) and the [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md).

## Contract

| Item | Value |
|:---|:---|
| Namespace | `payu-preprod` |
| Workload overlay | `infrastructure/workloads/overlays/payu-preprod` |
| Data overlay | `infrastructure/platform/data/overlays/preprod` |
| Messaging overlay | `infrastructure/platform/messaging/overlays/preprod` |
| Gate | Argo sync-wait → Cerberus health → Kraken chaos → CAB go/no-go |
| Approval | Security/CAB; production-like policy and capacity posture |

## Preflight

```bash
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/data/overlays/preprod
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/messaging/overlays/preprod
rtk oc get vaultstaticsecret -n payu-preprod
rtk oc get pods -n payu-preprod
rtk oc get deployment cerberus -n payu-preprod
```

The Kraken runtime and Cerberus must be rendered from
`infrastructure/platform/security/chaos/kraken`; do not run an ad-hoc chaos
pod. The gate fails closed if Cerberus is unavailable or the Kraken Job does
not complete within its timeout.

## Promotion

```bash
rtk tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=preprod \
  -p image-digest=<uat-sha256-digest> -p push-changes=true \
  -p environment-base-url=http://gateway-service.payu-preprod.svc:8080
```

Before CAB approval, attach pen-test/security results, immutable digest
provenance, Argo sync status, Cerberus health evidence, Kraken results, and
rollback digest.

## Acceptance and rollback

```bash
rtk oc get application -n openshift-gitops payu-preprod
rtk oc get deployment cerberus -n payu-preprod
rtk oc get job kraken-run -n payu-preprod
rtk oc get deployments,pods -n payu-preprod
rtk oc get events -n payu-preprod --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Rollback is a Git revert or the rollback pipeline to the last known-good
digest. Do not delete CNPG, Kafka, cache, or PVC resources during rollback.
