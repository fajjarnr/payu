# PayU Development Deployment Runbook

Runbook ini untuk environment integrasi `payu-dev`. Baca [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) terlebih dahulu; kontrak teknisnya berasal dari [DevSecOps architecture](../../infrastructure/DEVSECOPS_ARCHITECTURE.md).

## Tujuan dan batasan

`payu-dev` adalah tempat validasi developer dan integrasi, bukan bukti kesiapan
production. Ia boleh lebih longgar agar feedback cepat, tetapi tetap memakai
manifests yang sama, image digest immutable, non-root container, NetworkPolicy,
dan secret runtime dari Vault/VSO.

| Item | Kontrak saat ini |
|:---|:---|
| Namespace | `payu-dev` |
| Workload | `infrastructure/workloads/overlays/payu-dev` |
| Data/cache | `infrastructure/platform/data/overlays/dev` |
| Messaging | `infrastructure/platform/messaging/overlays/payu-dev` |
| Identity | `infrastructure/platform/identity/overlays/dev` di `payu-sso` |
| GitOps workload | Application `payu-dev` |
| Cache | Plain Hot Rod, endpoint auth/encryption disabled; dev-only |
| HA profile | Overlay dev; jangan dipakai sebagai baseline capacity production |

## Dependency order

1. Namespace, quota, LimitRange, NetworkPolicy, dan required operators.
2. CNPG, Infinispan, Kafka, AMQ Broker, dan runtime secrets.
3. RHBK/Keycloak dan identity secrets.
4. Workload overlay dan image pull permissions.
5. Tekton test/build evidence, lalu digest untuk SIT.

Jangan melompati dependency hanya karena pod aplikasi dapat dibuat. Aplikasi
yang `Running` tetapi database, cache, broker, atau VSO belum `Ready` belum
merupakan deployment berhasil.

## Preflight dan render

```bash
rtk oc whoami
rtk oc version
rtk oc get ns payu-dev payu-cicd payu-sso
rtk oc get csv -A | rtk rg 'cloudnative|datagrid|amq|keycloak|gitops|pipelines|vault'
rtk oc get storageclass
rtk oc kustomize infrastructure/platform/data/overlays/dev >/tmp/payu-dev-data.yaml
rtk oc kustomize infrastructure/platform/messaging/overlays/payu-dev >/tmp/payu-dev-messaging.yaml
rtk oc kustomize infrastructure/workloads/overlays/payu-dev >/tmp/payu-dev-workloads.yaml
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/data/overlays/dev
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/messaging/overlays/payu-dev
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/workloads/overlays/payu-dev
```

Simpan render dan output dry-run ke release evidence. Jika storage class,
image registry, Route, namespace, atau secret reference berubah, review diff
sebelum apply.

## Apply dan GitOps

Untuk bootstrap/dev recovery yang disetujui:

```bash
rtk oc apply --server-side -k infrastructure/platform/data/overlays/dev
rtk oc apply --server-side -k infrastructure/platform/messaging/overlays/payu-dev
rtk oc apply --server-side -k infrastructure/platform/identity/overlays/dev
rtk oc apply --server-side -k infrastructure/workloads/overlays/payu-dev
```

Setelah ApplicationSet aktif, perubahan workload normal harus melalui Git dan
Application `payu-dev`; direct apply hanya untuk bootstrap atau incident record.

```bash
rtk oc get applications.argoproj.io payu-dev -n openshift-gitops
rtk oc get applications.argoproj.io identity-dev -n openshift-gitops
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -60
```

## CI evidence sebelum SIT

```bash
rtk oc create -f infrastructure/platform/cicd/tekton/pipeline-runs/build-service-example.yaml
rtk oc create -f infrastructure/platform/cicd/tekton/pipeline-runs/test-service-example.yaml
rtk oc get pipelinerun -n payu-cicd -l app=payu,environment=dev
rtk tkn pipelinerun list -n payu-cicd
```

PipelineRun harus memiliki TaskRun `Succeeded` untuk fetch, unit,
architecture, integration, report, dan build bila image baru dibutuhkan.
Security/SAST/SCA evidence yang belum muncul di TaskRun adalah gap, bukan
alasan untuk mengasumsikan gate lulus. Catat image `sha256:<64 hex>`; jangan
promosikan `latest` atau tag mutable.

## Acceptance checklist

```bash
rtk oc get cluster.postgresql.cnpg.io payu-database -n payu-dev
rtk oc get kafka payu-kafka -n payu-dev
rtk oc get infinispan payu-cache -n payu-dev
rtk oc get activemqartemis payu-broker -n payu-dev
rtk oc get deployments,pods -n payu-dev
rtk oc get hpa -n payu-dev
rtk env NAMESPACE=payu-dev scripts/deployment/verify-deployment.sh
```

Pass criteria:

- Semua CR data/messaging melaporkan kondisi ready/healthy.
- Tidak ada changed workload `CrashLoopBackOff`, `ImagePullBackOff`, atau
  restart berulang.
- Test/build PipelineRun wajib `Succeeded` dan digest tercatat.
- Warning event baru dan error log changed service sudah dijelaskan.

## Abort criteria

Stop dan jangan promote ke SIT bila database migration/outbox error, secret
belum synced, cache/broker belum ready, image bukan digest, atau test gate
failed/timeout. Simpan `oc describe`, events, TaskRun logs, commit SHA, dan
rendered manifests.

## Rollback

Untuk perubahan manifest, revert commit lalu biarkan Argo reconcile:

```bash
rtk git revert <bad_commit_sha>
rtk oc get applications.argoproj.io payu-dev -n openshift-gitops
rtk oc rollout status deployment/<service> -n payu-dev --timeout=10m
```

Jangan menjadikan `oc rollout undo` sebagai rollback final karena menghasilkan
drift dari Git. Jangan menghapus CNPG, Kafka, cache, atau PVC untuk rollback.
