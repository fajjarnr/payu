# PayU SIT Deployment Runbook

This runbook covers `payu-sit`, the restricted system-integration environment.
Read the [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) and the [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) first.

## Contract

| Item | Value |
|:---|:---|
| Namespace | `payu-sit` |
| Workload overlay | `infrastructure/workloads/overlays/payu-sit` |
| Data overlay | `infrastructure/platform/data/overlays/sit` |
| Messaging overlay | `infrastructure/platform/messaging/overlays/sit` |
| Gate | Argo sync-wait → ZAP → Schemathesis → LitmusChaos → k6 smoke |
| Approval | Automatic only after Dev evidence and immutable digest are approved |

## Promotion

Use the GitOps promotion pipeline. `push-changes=true` is required for a real
promotion; `false` is only for a non-mutating validation run.

```bash
rtk tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> \
  -p environment=sit \
  -p image-digest=<dev-sha256-digest> \
  -p push-changes=true \
  -p environment-base-url=http://gateway-service.payu-sit.svc:8080 \
  -p schema-url=http://gateway-service.payu-sit.svc:8080/q/openapi.json \
  -p chaos-gate-enabled=true
```

The Litmus gate must be enabled for a production-grade SIT promotion. A lab
waiver may set it to `false`, but the waiver and reason belong in the release
evidence; it is not a default for production.

## Acceptance

```bash
rtk oc get application -n openshift-gitops payu-sit
rtk oc get deployments,pods -n payu-sit
rtk oc get chaosengine,chaosresult -n payu-sit
rtk oc get events -n payu-sit --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -60
```

Every required TaskRun must be `Succeeded`, the promoted image digest in the
SIT ImageStream must equal the Dev digest, and no new warning tied to the
release may remain unexplained.

## Rollback

Promote the last known-good digest through
`payu-rollback-gitops-pipeline` with `environment=sit` and
`push-changes=true`. Reverting the Git write-back commit is the authoritative
fallback.
