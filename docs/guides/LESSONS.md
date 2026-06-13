# 🧠 PayU Lessons Learned (Session Log)

This document serves as a chronological log of "Lessons Learned" and critical architectural discoveries made during development sessions. Detailed implementation patterns have been migrated to the **AI Agent Skill Ecosystem** in `.agents/skills/`.

---

## L-027: Tekton Pipeline — `onError: continue` Not Supported in v1.9

**Date**: 2026-05-02  
**Domain**: CI/CD  
**Context**: Tekton v1.9.0 (OpenShift Pipelines 1.22) does not support `onError: continue` on pipeline tasks. This means security scanning tasks (Trivy, Grype, ZAP) that find vulnerabilities will block the entire pipeline — there's no way to make them "warning-only" at the pipeline level.

**Pattern**: Use `|| true` shell wrappers inside the task's `script` to absorb non-zero exit codes:
```yaml
- name: grype-scan
  taskRef:
    name: grype-scan
  params:
    - name: args
      value: |
        grype dir:/workspace/source -o json > /workspace/grype-report.json || true
```

**Lesson**: For security scanning tools in Tekton pipelines, always wrap the scan command with `|| true` at the script level. Pipeline-level `onError: continue` won't work until Tekton v1.10+. Also log a warning if the scan failed, so teams still have visibility into skipped findings.

## L-028: Tekton Pipeline — Registry Auth `unused:<token>` Format

**Date**: 2026-05-02  
**Domain**: CI/CD  
**Context**: OpenShift internal image registry (`image-registry.openshift-image-registry.svc:5000`) uses service account tokens for authentication. The `registry-credentials` Secret must use `unused:<token>` as the `auth` field value (base64-encoded), NOT `username:password`. Standard tools like Podman and Buildah accept this format: `echo -n "unused:$(oc whoami -t)" | base64 -w0`.

**Pattern**: Always use `unused:` prefix with the token as the "password" field:
```yaml
apiVersion: v1
kind: Secret
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: |
    {
      "auths": {
        "image-registry.openshift-image-registry.svc:5000": {
          "auth": "<base64 of unused:<token>>"
        }
      }
    }
```

**Lesson**: The `unused:` prefix signals Docker/Podman clients to use token-based auth with no username. This is the OpenShift convention. Don't try to guess a username — `unused` is the literal string.

## L-029: Tekton Pipeline — License Compliance PURL Filtering

**Date**: 2026-05-02  
**Domain**: CI/CD  
**Context**: Syft generates SPDX/CycloneDX SBOMs that include ALL packages including OS-level (RPM, DEB). When checking license compliance, filter to application-level dependencies only (Maven, npm, PyPI, Go modules) to avoid false positives from base image packages that are licensed separately.

**Pattern**: Filter SBOM components by `purl` prefix before checking licenses:
```bash
# Grype/Syft: only check app-level dependencies
syft packages -o cyclonedx-json dir:/workspace/source \
  | jq '[.components[] | select(.purl // "" | startswith("pkg:maven") or startswith("pkg:npm") or startswith("pkg:pypi") or startswith("pkg:golang"))]' \
  > /workspace/app-sbom.json
```

**Lesson**: OS-level packages in UBI9/RHEL have their own license compliance lifecycle managed by Red Hat. The pipeline should only gate application dependencies. Use `purl` (Package URL) prefixes to distinguish dependency types.

---

## L-030: Podman DevSecOps — k6 Local Smoke Testing

**Date**: 2026-05-05  
**Domain**: DevOps  
**Context**: k6 local smoke test verified against podman compose stack. **918/918 requests passed, 0% failure rate, p(95) 1.71ms** against `gateway-service:8080/q/health`. The compose file uses `profiles: [devsecops]` — invoke with `podman compose -f infrastructure/local/podman/podman-compose.yml --profile devsecops run --rm k6`.

**Pattern**: Always use `-f` with explicit compose file path for non-default locations:
```bash
podman compose -f infrastructure/local/podman/podman-compose.yml --profile devsecops run --rm k6 run /tests/local-smoke.js
```

**Lesson**: `podman compose` without `-f` looks for `compose.yaml`/`docker-compose.yml` in the current directory only. The PayU compose file is at `infrastructure/local/podman/podman-compose.yml` — always pass `-f`.

---
*Last Updated: May 5, 2026*
