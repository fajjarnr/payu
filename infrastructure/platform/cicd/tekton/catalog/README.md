# PayU Tekton Catalog v1

Shared catalog for polyrepo per-service pipelines (ADR-0066).

- Source: `infrastructure/platform/cicd/tekton/catalog/`
- Tag: `payu-catalog-v1` (git tag on monorepo/payu-catalog repo)
- Tasks: buildah, gitleaks, semgrep, trivy, cosign, argocd-sync, gitops-writeback
- Usage per service `pipeline.yaml`:
  ```yaml
  tasks:
    - name: build
      taskRef:
        resolver: git
        params:
          - name: url
            value: https://github.com/fajjarnr/payu.git
          - name: revision
            value: payu-catalog-v1
          - name: pathInRepo
            value: infrastructure/platform/cicd/tekton/catalog/buildah-task.yaml
  ```
  or local `taskRef: catalog#v1` shorthand (Tekton Bundles).

Reuse verbatim from `../tasks/`; no duplication. Tag bump on task change.
