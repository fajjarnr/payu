# PayU Preprod Deployment Runbook

`payu-preprod` adalah production mirror dan final resilience gate. Baca [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) dan [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md).

## Environment contract

| Item | Kontrak saat ini |
|:---|:---|
| Namespace | `payu-preprod` |
| Workload | `infrastructure/workloads/overlays/payu-preprod` |
| Data | `infrastructure/platform/data/overlays/preprod` |
| Messaging | `infrastructure/platform/messaging/overlays/preprod` |
| Argo Applications | `data-preprod`, `messaging-preprod`, `identity-preprod`, `payu-preprod` |
| Gate | Argo sync-wait → Cerberus health → Kraken chaos → CAB go/no-go |
| Approval | Security/pen-test + CAB; production-like capacity and policy |

Preprod harus mirror production security semantics. Profil single-zone/Ceph
cluster pada lab hanya membuktikan manifest dapat dirender dan dijalankan;
tidak membuktikan node failure, multi-AZ storage, atau regional DR.

## Preflight

```bash
rtk oc get applications.argoproj.io data-preprod messaging-preprod identity-preprod payu-preprod -n openshift-gitops
rtk oc get vaultstaticsecret -n payu-preprod
rtk oc get cluster.postgresql.cnpg.io payu-database -n payu-preprod
rtk oc get kafka payu-kafka -n payu-preprod
rtk oc get infinispan payu-cache -n payu-preprod
rtk oc get deployments,pods -n payu-preprod
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/data/overlays/preprod
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/messaging/overlays/preprod
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/workloads/overlays/payu-preprod
rtk oc kustomize infrastructure/platform/security/chaos/kraken >/tmp/payu-preprod-kraken.yaml
```

Do not run an ad-hoc chaos pod. Pipeline task `kraken-chaos-gate` applies the
versioned overlay, waits for `deployment/cerberus` Available, then waits for
`job/kraken-run` Complete. Gate failure or timeout is fail-closed.

## Promotion

```bash
rtk tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=preprod \
  -p image-digest=<uat-sha256-digest> -p push-changes=true \
  -p environment-base-url=http://gateway-service.payu-preprod.svc:8080
```

Validation boleh memakai `push-changes=false`, tetapi CAB evidence harus berasal
dari run yang benar-benar melakukan immutable promotion dan Git write-back.

## Evidence and go/no-go

```bash
rtk tkn pipelinerun list -n payu-cicd -l type=deploy,environment=preprod
rtk oc get taskrun -n payu-cicd -l tekton.dev/pipelineRun=<pipelinerun>
rtk oc get istag -n payu-preprod | rtk rg '<service>|sha256-'
rtk oc get deployment cerberus -n payu-preprod
rtk oc get job kraken-run -n payu-preprod
rtk oc get applications.argoproj.io data-preprod payu-preprod -n openshift-gitops
rtk oc get deployments,pods -n payu-preprod
rtk oc get events -n payu-preprod --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Go hanya bila digest provenance, pen-test/security result, Argo sync, Cerberus,
Kraken, workload health, dan rollback digest lengkap. Jangan mengubah verdict
chaos secara manual.

## Abort and rollback

Abort bila Vault/identity tidak ready, any data integrity/migration error,
Kraken report gagal, Cerberus tidak healthy, image digest mismatch, atau
warning baru tidak terjelaskan. Simpan seluruh evidence sebelum cleanup.

```bash
rtk tkn pipeline start payu-rollback-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=preprod \
  -p image-digest=<last-known-good-sha256> -p push-changes=true
```

Fallback adalah Git revert. Jangan menghapus CNPG, Kafka, cache, broker, atau
PVC sebagai shortcut rollback.
