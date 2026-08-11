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
| API management | `infrastructure/platform/api-management/3scale` di `payu-api-management` |
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

## API management (3scale)

3scale di `payu-api-management` melayani public edge untuk workload PayU.
Kontrak deployment (lihat `infrastructure/platform/api-management/3scale/`):

- `apimanager.yaml` — APIManager dengan storage `system` di **EFS CSI**
  (`efs-csi` StorageClass, RWX). APIManager ini tidak pakai MinIO S3 untuk
  system file storage; `simpleStorageService` tidak dipakai.
- `payu-capabilities.yaml` — Backend/Product/ApplicationPlan declarative
  (Backend `payu-backend` → `gateway-service`, Product `payu-api` +
  `payu-product` dengan mapping rules).
- `threescale-provider-account` secret — `adminURL` (payu-admin route) +
  `token` (dari `system-seed`). Wajib ada sebelum capabilities reconcile.
- ClusterIssuer `letsencrypt-prod-issuer` — DNS01 Route53, zone
  `apps.fajjjar.my.id` (`Z03524191B5L20ILPO48O`), credential secret
  `cert-manager-route53-fajjjar` (copy `kube-system/aws-creds`).

Fakta lapangan (2026-08-11):

- APIManager recreate membutuhkan re-deploy proxy config + promote ke
  production agar zync membuat route; bila routes zync hilang, operator
  `Product`/`Backend` gagal dengan 503 karena `payu-admin` unreachable.
- `threescale-provider-account` **tidak** dibuat ulang otomatis oleh operator
  setelah APIManager recreate; buat manual dari `system-seed.ADMIN_ACCESS_TOKEN`.
- Product error `backend usage does not have valid backend reference` adalah
  race saat Backend baru Synced; fix dengan annotate force reconcile
  (`apps.3scale.net/reconcile`).
- Operator 3scale kadang tetap `Reconciling not finished` walaupun semua
  deployment ready; API tetap live lewat APIcast. Jangan mengejar
  `Available=True` sebagai satu-satunya bukti.
- Zync-managed routes tidak otomatis ter-recreate setelah APIManager
  recreate (Red Hat Solution 7139600). Resync resmi:
  `oc rsh <system-sidekiq-pod> bash -c 'bundle exec rake zync:resync:domains'`.
- Promote proxy config via API sering `403 Access denied`; gunakan
  `ProxyConfigPromote` CR (declarative) — `productCRName` + `production: true`.
- Backend service sync (app key tidak dikenal → APIcast 403) butuh
  `BackendStorageRewriteWorker.perform_async("Service", [<service_id>])`
  via rails runner di system-app; arg pertama **class name string**, bukan id.
- Route manual yang host-nya sama dengan route zync akan membuat route zync
  `Rejected` ("already exposes ... and is older"). Hapus route manual, jangan
  duplikasi host dengan zync-managed route.
- `backend-cron` crash `Errno::ENOENT dev/stdout` bila `CONFIG_LOG_PATH`
  menunjuk path yang tidak ada; set `CONFIG_LOG_PATH=/tmp`.

Bootstrap API management:

```bash
rtk oc apply -k infrastructure/platform/api-management/3scale
rtk oc apply -f infrastructure/platform/api-management/3scale/payu-capabilities.yaml
rtk oc get backend,product -n payu-api-management
# bila Product gagal "backend usage does not have valid backend reference":
rtk oc annotate product payu-api payu-product -n payu-api-management \
  apps.3scale.net/reconcile="$(date +%s)" --overwrite
```

Verifikasi edge:

```bash
curl -sk -o /dev/null -w '%{http_code}\n' \
  --resolve api-payu-apicast-production.apps.fajjjar.my.id:443:$(dig +short api-payu-apicast-production.apps.fajjjar.my.id) \
  "https://api-payu-apicast-production.apps.fajjjar.my.id/v1/partner/payment-links?user_key=test"
# 403 = APIcast auth boundary aktif (bukan 404/503)
```

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
