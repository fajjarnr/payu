# payu-service-template (ADR-0066)

Cookiecutter untuk polyrepo `fajjarnr/payu-<service>`. Monorepo `backend/` frozen setelah cutover pertama.

```bash
cookiecutter .agents/resources/templates/payu-service-template --no-input serviceName=account-service
# k8s di-copy dari infrastructure/workloads/base/<service> sebagai single source
cp -r infrastructure/workloads/base/account-service payu-account-service/k8s/base
cp -r infrastructure/workloads/overlays/payu-dev/account-service payu-account-service/k8s/overlays/dev
```

Isi template:
- `Containerfile.runtime` UBI9 UID 1001 readOnly FS (reuse `backend/<service>/Containerfile`)
- `.tekton/pipeline.yaml` + `trigger.yaml` (el-github-listener, ClusterTriggerBinding payu-service, always-run, catalog payu-catalog-v1)
- `.argocd/application.yaml` + `applicationset.yaml` (5 env, promote by digest `kustomize edit set image @sha256`, Image Updater disabled)
- `.github/CODEOWNERS` per-service

Pipeline 6 stages via `infrastructure/platform/cicd/tekton/catalog` (payu-catalog-v1). Secrets `payu/<env>/<service>/db-credentials` via ESO ClusterSecretStore payu-vault (VSO variant commented).
