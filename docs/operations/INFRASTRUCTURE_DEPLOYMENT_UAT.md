# PayU UAT Deployment Runbook

`payu-uat` adalah environment acceptance dengan QA dan Product Owner approval.
Baca [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) dan [DevSecOps architecture](../architecture/DEVSECOPS_ARCHITECTURE.md).

## Environment contract

| Item | Kontrak saat ini |
|:---|:---|
| Namespace | `payu-uat` |
| Workload | `infrastructure/workloads/overlays/payu-uat` |
| Data | `infrastructure/platform/data/overlays/uat` |
| Messaging | `infrastructure/platform/messaging/overlays/uat` |
| Argo Applications | `data-uat`, `messaging-uat`, `identity-uat`, `payu-uat` |
| Gate | Argo sync-wait → Schemathesis → k6 smoke/load → QA/PO approval |
| Ingress | `app-uat.apps.fajjjar.my.id`, internal gateway URL untuk Tekton |
| Security | Strict namespace policy, VSO, cache mTLS/PKCS#12 contract |

UAT tetap harus production-like dalam security dan promotion semantics. Kapasitas
lab, single-zone scheduling, dan Ceph-backed PVC bukan bukti availability
production/multi-AZ.

## Preflight: infrastructure and secrets

```bash
rtk oc get applications.argoproj.io data-uat messaging-uat identity-uat payu-uat -n openshift-gitops
rtk oc get vaultstaticsecret -n payu-uat
rtk oc get vaultstaticsecret -n payu-uat -o json | rtk jq -r '.items[] | [.metadata.name, (.status.conditions // [] | map(select(.type=="SecretSynced") | .status) | first // "missing"), (.status.conditions // [] | map(select(.type=="SecretSynced") | .message) | first // "")] | @tsv'
rtk oc get secret payu-cache-client-ca payu-cache-server-tls payu-cache-credentials -n payu-uat
rtk oc get cluster.postgresql.cnpg.io payu-database -n payu-uat
rtk oc get infinispan payu-cache -n payu-uat
rtk oc get pods -n payu-uat | rtk rg 'payu-cache|gateway-service|auth-service|account-service'
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/data/overlays/uat
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/messaging/overlays/uat
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/workloads/overlays/payu-uat
```

Required cache secret contract: `truststore.p12` adalah binary PKCS#12 yang
valid dan `truststore-password` adalah password string. VSO transformation
harus decode value yang disimpan di Vault; raw base64 text yang dipasang sebagai
keystore adalah invalid. Jangan mencetak nilai Secret.

Do not promote while any required VSS is unsynced, cache `WellFormed`/pods are
not ready, gateway health fails, or Argo is still reconciling a failed revision.

The UAT validation PipelineRun `account-service-deploy-uat-rjj9s` completed,
but it is not sufficient to waive this preflight. The run's functional gates
did not prove the cache mTLS path; inspect gateway logs for
`certificate_unknown` and verify `SecretSynced=True` first.

## Promotion and gates

```bash
rtk tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=uat \
  -p image-digest=<sit-sha256-digest> -p push-changes=true \
  -p environment-base-url=http://gateway-service.payu-uat.svc:8080 \
  -p schema-url=http://gateway-service.payu-uat.svc:8080/q/openapi.json
```

`push-changes=false` hanya validation. Real promotion harus melakukan digest
promotion, Git write-back, dan Argo reconciliation. Argo sync sukses saja
tidak berarti UAT accepted.

## Evidence and acceptance

```bash
rtk tkn pipelinerun list -n payu-cicd -l type=deploy,environment=uat
rtk oc get taskrun -n payu-cicd -l tekton.dev/pipelineRun=<pipelinerun>
rtk oc get istag -n payu-uat | rtk rg '<service>|sha256-'
rtk oc get applications.argoproj.io data-uat payu-uat -n openshift-gitops
rtk oc get deployments,pods -n payu-uat
rtk oc get events -n payu-uat --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Pass criteria:

- `promote-image`, write-back, Argo wait, Schemathesis, k6 smoke/load, dan
  notify semuanya `Succeeded`.
- Digest SIT dan UAT identik; tidak ada rebuild berdasarkan tag.
- Schemathesis memenuhi seluruh operation yang diharapkan.
- k6 memenuhi success-rate, error-rate, dan latency thresholds.
- QA dan PO memberi approval terhadap evidence yang sama.

## Abort and rollback

Abort jika VSO/cache gagal, registration/core CRUD gagal, digest mismatch,
Argo sync failed, Schemathesis menemukan unauthorized/crash, atau k6 threshold
gagal. Simpan log gateway/account/cache, PipelineRun/TaskRun, Argo revision,
dan k6 summary.

```bash
rtk tkn pipeline start payu-rollback-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=uat \
  -p image-digest=<last-known-good-sha256> -p push-changes=true
```

Git revert adalah fallback authoritative. Jangan patch Deployment langsung,
hapus PVC/stateful CR, atau menyembunyikan failed gate dengan rerun tanpa
root-cause evidence.
