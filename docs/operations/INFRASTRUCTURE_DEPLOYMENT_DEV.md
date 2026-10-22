# PayU Development Deployment Runbook

Runbook ini untuk environment integrasi `payu-dev`. Baca [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) terlebih dahulu; kontrak teknisnya berasal dari [DevSecOps architecture](../architecture/DEVSECOPS_ARCHITECTURE.md).

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
  `payu-product` dengan mapping rules). **Hapus `providerAccountRef` dari
  `Application` dan `ProxyConfigPromote`** — CRD 0.13.4 tidak punya field tsb
  (hanya `Backend`/`Product`/`DeveloperAccount`/`DeveloperUser` yang pakai).
- `threescale-provider-account` secret — `adminURL` = `https://payu-admin.apps.fajjjar.my.id`
  (payu-admin route via **shared-ingress** `apps.fajjjar.my.id`, bukan
  `*.apps.cluster-*.sandbox` atau `*.apps.payu.ocp.fajjjar.my.id`) + `token`
  (dari `system-seed.ADMIN_ACCESS_TOKEN`). Wajib ada sebelum capabilities reconcile.
  File sumber: `secrets-3scale-dev.yaml` sudah `https://payu-admin.apps.fajjjar.my.id`.
- Namespace `payu-api-management` **wajib** label `payu.fajjjar.my.id/ingress: shared`
  agar zync-managed routes (`payu-admin`, `master`, `api-payu-*`) memakai
  `shared-ingress` (`router-shared-ingress.apps.fajjjar.my.id`) dengan cert
  `*.apps.fajjjar.my.id` (`shared-ingress-cert` via `letsencrypt-prod-issuer`);
  tanpa label, routes jatuh ke `router-default` (`*.apps.payu.ocp.fajjjar.my.id`)
  dan `Backend` gagal `x509: certificate is valid for *.apps.payu.ocp...`.
  **Blokir 2026-10-20:** shared domain belum bisa dipakai sama sekali
  (delegasi NS `apps.fajjjar.my.id` di parent Cloudflare masih menunjuk NS set
  Route53 lama -> SERVFAIL, cert tak terbit, `router-shared-ingress` 0/3), jadi
  3scale dev **belum bisa lolos** sampai `SHARED-DNS-001` diperbaiki. Setelah
  delegasi diperbaiki, `shared-ingress-cert` terbit dan route 3scale ikut hidup.
- `payu-partner-user-credentials` secret — `password` untuk `DeveloperUser`
  `payu-partner-admin` (referenced `payu-capabilities.yaml`). Buat manual
  `oc create secret generic payu-partner-user-credentials -n payu-api-management --from-literal=password=...`
  bila belum ada (operator akan `Failed: Secret not found`).
- `ProxyConfigPromote` — buat untuk **kedua** Product (`payu-api` dan
  `payu-product`), bukan hanya `payu-api`; promote via CR declarative
  `productCRName` + `production:true` (API `403 Access denied` jika via REST).
- ClusterIssuer `letsencrypt-prod-issuer` — DNS01 Route53, zone
  `apps.fajjjar.my.id` (`Z03524191B5L20ILPO48O`), credential secret
  `cert-manager-route53-fajjjar` (copy `kube-system/aws-creds`).
- Ingress HA: `ingresscontroller/default` `replicas:3` (was 2) agar ELB
  `a97cd582...` 3 AZ semua healthy; bila hanya 2, `describe-target-health`
  akan `unhealthy` untuk 1 AZ dan `curl` ke `payu-admin` via LB `503`
  round-robin. Validasi `aws elbv2 describe-target-health`.

Fakta lapangan (2026-08-11 → 2026-08-24):

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
  `oc exec -n payu-api-management <system-sidekiq-pod> -c system-sidekiq -- bundle exec rake zync:resync:domains`.
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
- **Namespace label**: tanpa `payu.fajjjar.my.id/ingress: shared` di
  `payu-api-management`, zync routes jatuh ke `router-default` dan TLS
  `x509` fail.
- **ELB health**: bila `payu-admin` `503` round-robin, cek
  `aws elbv2 describe-target-health` — deregister masters/workers tanpa router
  dari target group.
- **Operator hostAliases**: bila `payu-admin` masih `503` dari dalam cluster
  (operator pod), patch CSV+Deployment `hostAliases: [{ip: <shared-LB-IP>,
  hostnames: [payu-admin, master]}]` (Red Hat docs: `Recreate` strategy via
  JSON patch `replace type` + `remove rollingUpdate`).

Bootstrap API management:

```bash
rtk oc label namespace payu-api-management payu.fajjjar.my.id/ingress=shared --overwrite
rtk oc apply -k infrastructure/platform/api-management/3scale
# tunggu APIManager ready (ready:[apicast,backend,system,memcache,searchd,sidekiq,zync,db,que] meski Available:False)
rtk oc get secret system-seed -n payu-api-management -o jsonpath='{.data.ADMIN_ACCESS_TOKEN}' | base64 -d # catat
rtk oc get secret threescale-provider-account -n payu-api-management -o jsonpath='{.data.adminURL}' | base64 -d # harus https://payu-admin.apps.fajjjar.my.id
# bila tidak ada, buat payu-partner-user-credentials
rtk oc get secret payu-partner-user-credentials -n payu-api-management || rtk oc create secret generic payu-partner-user-credentials -n payu-api-management --from-literal=password=partner_dev_secret_2026
rtk oc apply -f infrastructure/platform/api-management/3scale/payu-capabilities.yaml
rtk oc get backend,product -n payu-api-management
# bila Product gagal "backend usage does not have valid backend reference":
rtk oc annotate product payu-api payu-product -n payu-api-management \
  apps.3scale.net/reconcile="$(date +%s)" --overwrite
# bila Backend 503 / x509, cek ELB health dan resync zync:
aws elbv2 describe-target-health --target-group-arn $(aws elbv2 describe-target-groups --load-balancer-arn $(aws elbv2 describe-load-balancers --names a97cd5823fd9b47d6851f0b90908e0d2 --query 'LoadBalancers[0].LoadBalancerArn' --output text) --query 'TargetGroups[?Port==`443`].TargetGroupArn' --output text) --query 'TargetHealthDescriptions[?TargetHealth.State!=`healthy`].[Target.Id,TargetHealth.State]' --output table
rtk oc exec -n payu-api-management $(rtk oc get pods -n payu-api-management -l app=payu-3scale -o jsonpath='{.items[?(@.metadata.labels.threescale_component_element=="sidekiq")].metadata.name}' | head -n1) -c system-sidekiq -- bundle exec rake zync:resync:domains
```

Verifikasi edge:

```bash
# 3scale admin/master harus 200 login (bukan 503)
curl -skL -o /dev/null -w '%{http_code}\n' --resolve payu-admin.apps.fajjjar.my.id:443:$(dig +short payu-admin.apps.fajjjar.my.id | head -n1) https://payu-admin.apps.fajjjar.my.id/p/login # 200
curl -sk -o /dev/null -w '%{http_code}\n' \
  --resolve api-payu-apicast-production.apps.fajjjar.my.id:443:$(dig +short api-payu-apicast-production.apps.fajjjar.my.id | head -n1) \
  "https://api-payu-apicast-production.apps.fajjjar.my.id/v1/partner/payment-links?user_key=test"
# 403 = APIcast auth boundary aktif (bukan 404/503); 404 dari gateway berarti mapping belum promote
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

Untuk bootstrap/dev recovery yang disetujui (urutan terverifikasi di cluster
baru `payu-drrpz` 2026-10-20):

```bash
# 1. data: tunggu CNPG ready=3/3 SEBELUM lanjut
rtk oc apply --server-side -k infrastructure/platform/data/overlays/dev
rtk oc get cluster.postgresql.cnpg.io payu-database -n payu-dev

# 2. messaging, lalu amq-broker (size 1 harus menang setelah size 2)
rtk oc apply --server-side -k infrastructure/platform/messaging/overlays/payu-dev
rtk oc apply --server-side -k infrastructure/platform/amq-broker/overlays/dev

# 3. identity: tunggu RHBK Ready=True dan job sync admin Complete
rtk oc apply --server-side -k infrastructure/platform/identity/overlays/dev
rtk oc get keycloak payu-keycloak -n payu-sso
rtk oc get job keycloak-admin-password-sync -n payu-sso

# 4. REALM payu = langkah MANUAL (dev overlay menghapus KeycloakRealmImport, L-377)
#    + selaraskan client secret realm dengan nilai ESO. Lihat:
#    docs/operations/runbooks/dev-seed.md bagian 4-5

# 5. images (build + push), baru workload: SATU apply cukup (OVERLAY-STRUCT-001
#    CLOSED 2026-10-22 — root + per-service overlay berbagi kontrak OIDC/cache
#    via Kustomize Components, root kini 30/30 Deployment). Pilih SALAH SATU:
#    (a) root untuk bootstrap fleet penuh, atau (b) loop per-service untuk
#    deploy bertarget. Keduanya membawa kontrak env yang sama (guard paritas di
#    verify-overlay-drift.sh menjaganya).
rtk oc apply --server-side --force-conflicts --field-manager=payu-dev-root -k infrastructure/workloads/overlays/payu-dev
# ATAU bertarget:
# for d in infrastructure/workloads/overlays/payu-dev/*/; do
#   [ -f "$d/kustomization.yaml" ] && rtk oc apply --server-side --force-conflicts -k "$d"
# done
```

Setelah ApplicationSet aktif, perubahan workload normal harus melalui Git dan
Application `payu-dev`; direct apply hanya untuk bootstrap atau incident record.

```bash
rtk oc get applications.argoproj.io payu-dev -n openshift-gitops
rtk oc get applications.argoproj.io identity-dev -n openshift-gitops
rtk oc get events -n payu-dev --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -60
```

### Jebakan bootstrap cluster baru (terverifikasi 2026-10-20; overlay CLOSED 2026-10-22)

- **~~Root overlay dan per-service overlay adalah dua deploy unit berbeda~~ CLOSED 2026-10-22
  (`OVERLAY-STRUCT-001`)**: kontrak OIDC/cache/shared-endpoints kini di Kustomize Components
  (`overlays/payu-dev/components/dev-cache`, `dev-oidc`) yang dipakai root + 30 per-service —
  satu `oc apply -k` dari unit mana pun membawa kontrak env yang sama (root 30/30 Deployment,
  128 objek; guard paritas + `test_dev_workload_overlays_are_single_apply_units`). Sebelumnya:
  root (29 Deployment) men-strip env per-service dan sebaliknya; live = union tak reproducible.
- **Pod instance CNPG yang Pending bisa membawa anti-afinitas lawas.** Job
  `payu-database-2-join` macet ~20 menit karena pod template-nya punya
  **required** anti-afinitas zona dari generasi Cluster lama, padahal keempat node
  ada di AZ `ap-southeast-1a` (`0/4 nodes are available: … 3 node(s) didn't match
  pod anti-affinity rules`); `payu-database-1` membawa aturan sama sehingga
  instance-2 ikut terblokir. Yang berhasil: hapus Job satu-shot + 2 PVC Pending
  kosong (tanpa data) + pod primary yang membawa aturan lawas, biarkan operator
  membuat ulang (render mengikuti spec `podAntiAffinityType: preferred`).
  **Pod Pending yang membawa anti-afinitas lawas harus dibuat ulang, bukan di-wait.**
- **ResourceQuota `secrets` bisa mengunci operator.** Ceiling lama 150 kena
  150/150 saat rollout 30 workload, lalu CNPG operator gagal total
  (`cannot create Cluster auxiliary objects: secrets "payu-database-pull" is
  forbidden: exceeded quota`) -> instance tak bisa dibuat ulang, DB turun, dan 30
  Deployment ikut mati. `payu-dev-quota` kini `secrets 150->300` (+ `pods
  100->150`, `requests.cpu 16->24`, `requests.memory 32Gi->64Gi`, `limits.cpu
  64->96`, `limits.memory 80Gi->120Gi`, `pvc 30->40`, `requests.storage
  200Gi->400Gi`, `services 50->80`, `configmaps 100->200`).
- **Ingress class: `spec.ingress.className` bukan nama IngressController.**
  OpenShift me-prefix nama IC jadi `openshift-<nama>`; untuk IC `shared-ingress`
  IngressClass-nya `openshift-shared-ingress`. Dengan class yang tak dikenal
  (`shared-ingress`) Route **tidak pernah terbentuk** sama sekali, bukan
  "terbentuk tapi gagal admit". Verifikasi dengan `oc get ingressclass`.
- **Host/DNS reality (dev sekarang di domain cluster).** `apps.fajjjar.my.id`
  **tak resolve**: parent zone `fajjjar.my.id` ada di Cloudflare
  (`NS lovisa/clayton.ns.cloudflare.com`) dan delegasi `apps` masih menunjuk NS
  set Route53 lama, sementara zone `Z05387333BISN5F64PPM5` dilayani
  `ns-690.awsdns-22.net`, `ns-77.awsdns-09.com`, `ns-1500.awsdns-59.org`,
  `ns-1712.awsdns-22.co.uk` -> SERVFAIL/REFUSED, challenge ACME DNS-01 tak
  pernah tervalidasi, `router-shared-ingress` 0/3 dan `shared-ingress-cert` tak
  terbit. AWS admin tak bisa memperbaiki (Route53 hanya bisa mendelegasikan
  subdomain dari zone yang di-host-nya). Dev berjalan di domain cluster
  `apps.payu.ocp.fajjjar.my.id`: web-app `payu-dev.apps.payu.ocp.fajjjar.my.id`,
  analytics-ws `analytics-ws.apps.payu.ocp.fajjjar.my.id`, Keycloak hostname +
  issuer `https://sso-dev.apps.payu.ocp.fajjjar.my.id`. Sertifikat dipercaya
  comes dari `infrastructure/platform/security/cert-manager/letsencrypt/5-ingressController.yaml`
  (IngressController `default`, `defaultCertificate: app-router-certs` ->
  cert Let's Encrypt `CN=*.apps.payu.ocp.fajjjar.my.id`). Langkah manual +
  verifikasi ada di `docs/roadmap/TODOS.md` -> `SHARED-DNS-001`.

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
rtk oc get kafkatopic -n payu-dev --no-headers | rtk wc -l      # 118
rtk oc get infinispan payu-cache -n payu-dev
rtk oc get activemqartemis payu-broker -n payu-dev
rtk oc get keycloak payu-keycloak -n payu-sso
rtk oc get deployments,pods -n payu-dev
rtk oc get hpa -n payu-dev
rtk oc get route -n payu-sso -o wide
rtk env NAMESPACE=payu-dev scripts/deployment/verify-deployment.sh
./scripts/verify-overlay-drift.sh payu-dev                       # OK: no overlay drift in payu-dev
```

Pass criteria:

- Semua CR data/messaging melaporkan kondisi ready/healthy.
- Tidak ada changed workload `CrashLoopBackOff`, `ImagePullBackOff`, atau
  restart berulang.
- Test/build PipelineRun wajib `Succeeded` dan digest tercatat.
- Warning event baru dan error log changed service sudah dijelaskan.
- `keycloak-admin-password-sync` Job `Complete` dan `oc get secret
  payu-keycloak-admin -n payu-sso` nilainya yang dipakai operator.
- Realm `payu` ter-import: user `customer1`/`customer2`/`admin` punya `sub` yang
  **sama** dengan `users.external_id` di DB, `accountId` terisi, dan
  `unmanagedAttributePolicy=ENABLED`. Detail + perintahnya:
  `docs/operations/runbooks/dev-seed.md` bagian 4-6.
- Route SSO admitted (`status.ingress[0].routerName` non-empty) dan host-nya
  sama dengan `spec.hostname.hostname` CR Keycloak, supaya issuer yang
  dibaked ke token cocok dengan yang dilihat browser.
- Drift guard `verify-overlay-drift.sh payu-dev` -> `OK: no overlay drift`.
  Ingat guard ini membandingkan live vs **per-service** overlay saja.

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
