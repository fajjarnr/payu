# PayU UAT Deployment Runbook

This runbook covers `payu-uat`, where Product Owner and QA approval is required.
Read the [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) and the [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md).

## Contract

| Item | Value |
|:---|:---|
| Namespace | `payu-uat` |
| Workload overlay | `infrastructure/workloads/overlays/payu-uat` |
| Data overlay | `infrastructure/platform/data/overlays/uat` |
| Messaging overlay | `infrastructure/platform/messaging/overlays/uat` |
| Gate | Argo sync-wait → Schemathesis → k6 load → QA/PO approval |
| Security | VaultStaticSecret synced; strict ingress; production-like cache TLS contract |

## Preflight

```bash
rtk oc apply --server-side --dry-run=server -k infrastructure/platform/data/overlays/uat
rtk oc get vaultstaticsecret -n payu-uat
rtk oc get secret payu-cache-client-ca payu-cache-server-tls payu-cache-credentials -n payu-uat
rtk oc get pods -n payu-uat | rtk rg 'payu-cache|gateway-service|auth-service'
```

Do not start a UAT promotion while any required VaultStaticSecret is unsynced,
the cache is not ready, or the gateway health endpoint is failing. The cache
client truststore must be a valid PKCS#12 contract (`truststore.p12` plus
`truststore-password`); do not use a raw base64 string as a mounted keystore.

## Promotion and gates

```bash
rtk tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=uat \
  -p image-digest=<sit-sha256-digest> -p push-changes=true \
  -p environment-base-url=http://gateway-service.payu-uat.svc:8080 \
  -p schema-url=http://gateway-service.payu-uat.svc:8080/q/openapi.json
```

Schemathesis must pass all expected operations. k6 must pass its configured
success-rate, error-rate, and latency thresholds. A successful Argo sync alone
is not UAT acceptance.

## Acceptance and rollback

```bash
rtk oc get application -n openshift-gitops payu-uat
rtk oc get deployments,pods -n payu-uat
rtk oc get events -n payu-uat --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -60
```

Attach the QA/PO approval, PipelineRun/TaskRun results, digest comparison, and
k6 summary to the release record. Roll back by Git revert or the rollback
pipeline to the last known-good digest; never patch a Deployment directly.
