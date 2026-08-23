# PayU SIT Deployment Runbook

`payu-sit` adalah system-integration gate setelah `payu-dev`. Kontraknya
diturunkan dari [shared MOP](INFRASTRUCTURE_DEPLOYMENT.md) dan [DevSecOps architecture](../architecture/DEVSECOPS_ARCHITECTURE.md).

## As-built 1.18.0 (2026-08-23) — baca dulu

Stack **hidup dan terverifikasi** dengan profil lab berikut (menunggu Vault/VSO,
lihat DEVSECOPS-017):

- Secret **plain**, bukan VaultStaticSecret: `cnpg-secrets.yaml`,
  `console-oidc-secret.yaml` (messaging), `keycloak-secrets.yaml` (identity).
- Cache = **Infinispan standalone StatefulSet** (Hot Rod plaintext, operator CR
  di-skip karena webhook panics); workload `PAYU_CACHE_HOTROD_USE_SSL=false`.
- Kafka listener **plaintext :9092** (patch TLS-only dilepas dari common).
- SSO = Keycloak bersama dev (`sso-dev.apps…` issuer + JWK internal payu-sso);
  Keycloak sit tetap ter-deploy untuk isolasi mendatang (SSO-ENV-002).
- Gate chaos (Litmus/Kraken) **skip eksplisit** selama agent belum terpasang di
  namespace (CHAOS-ENV-001). Schemathesis mengecek 5xx; conformance penuh
  menunggu kredensial uji (SX-AUTH-001).

## Environment contract

| Item | Kontrak saat ini |
|:---|:---|
| Namespace | `payu-sit` |
| Workload | `infrastructure/workloads/overlays/payu-sit` |
| Data | `infrastructure/platform/data/overlays/sit` |
| Messaging | `infrastructure/platform/messaging/overlays/sit` |
| Argo Applications | `data-sit`, `messaging-sit`, `identity-sit`, `payu-sit` |
| External DAST route | `gateway-sit.apps.fajjjar.my.id` |
| Gate | Argo sync-wait → ZAP → Schemathesis → LitmusChaos → k6 smoke |
| Approval | Dev evidence + immutable digest; chaos waiver harus terdokumentasi |

Overlay SIT saat ini adalah profil lab/capacity-reduced: CNPG, cache, Kafka,
dan broker dapat dipatch lebih kecil/single-zone. Itu valid untuk cluster lab,
tetapi bukan bukti production capacity atau multi-zone resilience.

## Preconditions

```bash
rtk oc get applications.argoproj.io data-sit messaging-sit identity-sit payu-sit -n openshift-gitops
rtk oc get secret payu-database-app payu-database-superuser -n payu-sit
rtk oc get cluster.postgresql.cnpg.io payu-database -n payu-sit
rtk oc get kafka payu-kafka -n payu-sit
rtk oc get sts payu-cache -n payu-sit
rtk oc get deployments,pods -n payu-sit
rtk oc get route gateway-sit -n payu-sit
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/data/overlays/sit
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/platform/messaging/overlays/sit
rtk oc apply --server-side --dry-run=server --field-manager=argocd-controller --force-conflicts -k infrastructure/workloads/overlays/payu-sit
```

Jangan mulai promotion bila Application `OutOfSync` karena error, secret plain
(cnpg/keycloak/console-oidc) tidak lengkap, gateway belum ready, atau warning
baru belum dijelaskan.

## Promotion procedure

**Jalur terbukti (v1.18.0)**: per-service pipeline dengan `target-env` — lihat
`account-service-promote-sit-*` (run `q92mw`, 17/17 task hijau). Buat
PipelineRun dari template `infrastructure/platform/cicd/tekton/pipeline-runs/`
dengan parameter: `target-env=sit`, `image=…/payu-sit/<service>`,
`image-tag=<SemVer>`, `push-changes=true`. Rantai tugas:
clone→gitleaks→trufflehog→semgrep→compile→build→syft→grype→trivy→rhacs-scan→
cosign-sign→argocd-sync→zap-baseline→schemathesis→k6-smoke→litmus(skip bila
tanpa agent)→gitops-writeback (commit+push overlay `payu-sit`).

Mode digest `payu-deploy-gitops-pipeline` di bawah tetap valid sebagai alternatif
production-grade setelah Vault/Rollouts live:

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

`push-changes=false` hanya untuk validation run yang tidak menulis Git. Untuk
promotion sungguhan, write-back commit dan Argo reconciliation wajib terjadi.
Litmus wajib aktif pada jalur production-grade; waiver lab perlu alasan,
approver, expiry, dan tidak boleh menjadi default.

## Gate evidence

```bash
rtk tkn pipelinerun list -n payu-cicd -l type=deploy,environment=sit
rtk oc get taskrun -n payu-cicd -l tekton.dev/pipelineRun=<pipelinerun>
rtk oc get istag -n payu-sit | rtk rg '<service>|sha256-'
rtk oc get chaosengine,chaosresult -n payu-sit
rtk oc get deployments,pods -n payu-sit
rtk oc get events -n payu-sit --field-selector type=Warning --sort-by=.lastTimestamp | rtk tail -80
```

Pass criteria:

- `promote-image` membuktikan digest source dan SIT sama persis.
- `gitops-writeback`, `argocd-sync-wait`, ZAP, Schemathesis, Litmus, k6, dan
  notify semuanya `Succeeded`.
- No high/critical ZAP finding, Schemathesis crash/unauthorized result, atau
  k6 threshold failure.
- Workload dan route sehat setelah gate; warning baru terjelaskan.

## Abort and rollback

Abort bila digest mismatch, Argo sync failed, security gate failed, chaos
verdict bukan Pass, k6 threshold gagal, atau data integrity/error migration
muncul. Simpan PipelineRun, TaskRun logs, Argo revision, ImageStream digest,
chaos result, dan route response.

```bash
rtk tkn pipeline start payu-rollback-pipeline -n payu-cicd \
  -w name=source,claimName=tekton-workspace-pvc \
  -p service-name=<service> -p environment=sit \
  -p image-digest=<last-known-good-sha256> -p push-changes=true
```

Revert write-back commit adalah fallback authoritative. Jangan memakai
`oc rollout undo` sebagai hasil akhir dan jangan menghapus stateful CR/PVC.
