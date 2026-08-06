# PayU Production Deployment Runbook

`payu` adalah production namespace. Baca [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) dan [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md). Tidak ada langkah di file ini yang memberi izin bypass.

## Release blocked until hard gates pass

Production **belum release-ready** bila salah satu berikut belum terbukti:

- CAB dan CISO approval, maintenance window, owner/on-call, change ticket, dan
  rollback digest tersedia.
- Image dipromosikan dari preprod berdasarkan digest; RHACS admission menerima
  signature, provenance, SBOM, dan registry policy.
- Vault HA/auto-unseal/backup, VSO sync, secret rotation, storage, monitoring,
  audit log, dan restore evidence sehat.
- Workload production memakai Argo Rollouts `Rollout` + `AnalysisTemplate`
  dengan blue/green atau canary dan automatic rollback. Plain `Deployment`
  bukan bukti canary production.
- Argo sync window terbuka. AppProject production tetap menolak sync di luar
  window; jangan bypass dengan direct `oc apply`.

## Environment contract

| Item | Kontrak target |
|:---|:---|
| Namespace | `payu` |
| Workload | `infrastructure/workloads/overlays/payu-prod` |
| Data | `infrastructure/platform/data/overlays/prod` |
| Messaging | `infrastructure/platform/messaging/overlays/prod` |
| Argo Applications | `data-prod`, `messaging-prod`, `identity-prod`, `payu-prod` |
| Strategy | Argo Rollouts blue/green first; canary only with metrics evidence |
| Security | mTLS/zero-trust, RHACS admission, Vault/VSO, backup/audit |
| Storage | Approved production class only; lab Ceph/RBD/GP3 is not production evidence |

## Read-only readiness audit

```bash
rtk oc whoami
rtk oc get clusterversion
rtk oc get applications.argoproj.io data-prod messaging-prod identity-prod payu-prod -n openshift-gitops
rtk oc get appproject payu -n openshift-gitops
rtk oc get applications.argoproj.io payu-prod -n openshift-gitops -o jsonpath='{.spec.syncPolicy}{"\n"}'
rtk oc get vaultstaticsecret -n payu
rtk oc get networkpolicy -n payu
rtk oc get crd rollouts.argoproj.io analysistemplates.argoproj.io
rtk oc get rollout,analysisrun -n payu
rtk oc get storageclass
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/data/overlays/prod
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/messaging/overlays/prod
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/workloads/overlays/payu-prod
```

Jika CRD Rollouts/AnalysisTemplate belum ada, atau Application production
`Missing`, audit berhenti. Jangan menjalankan pipeline production hanya untuk
“mencoba”. Current repo/lab evidence memang belum memenuhi gate ini.

## Controlled promotion

Hanya setelah approval tertulis, readiness audit hijau, dan sync window terbuka:

```bash
rtk tkn pipeline start payu-deploy-gitops-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=prod \
  -p image-digest=<preprod-sha256-digest> -p push-changes=true \
  -p environment-base-url=https://gateway.apps.fajjjar.my.id \
  -p schema-url=https://gateway.apps.fajjjar.my.id/q/openapi.json
```

`push-changes=false` wajib ditolak untuk production oleh pipeline. Jangan
memaksa Argo sync, mengubah image tag langsung, atau melakukan `oc apply` ke
`payu` sebagai shortcut.

## Acceptance and evidence

```bash
rtk tkn pipelinerun list -n payu-cicd -l type=deploy,environment=prod
rtk oc get applications.argoproj.io data-prod payu-prod -n openshift-gitops
rtk oc get rollout -n payu
rtk oc get analysisrun -n payu
rtk oc get pods -n payu
rtk oc get events -n payu --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Promote traffic hanya jika Rollout analysis sehat pada setiap step, error/latency
SLO memenuhi threshold, RHACS tidak menghasilkan violation, dan observability
menunjukkan no regression. Simpan Argo history, Rollout/AnalysisRun, RHACS,
digest/signature/provenance/SBOM, approvals, monitoring link, dan operator log.

## Abort and rollback

Abort otomatis/manual bila analysis gagal, SLO breach, signature/policy gagal,
VSO/Vault unhealthy, data migration error, atau audit evidence incomplete.

Gunakan Rollout abort/rollback yang didefinisikan manifest, atau rollback
pipeline/Git revert ke digest known-good. Preserve failed ReplicaSet, logs,
and evidence sampai incident review selesai. Jangan menghapus PVC, CNPG,
Kafka, cache, atau stateful CR production.
