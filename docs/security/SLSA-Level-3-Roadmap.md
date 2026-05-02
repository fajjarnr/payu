# SLSA Level 3 — Hermetic Builds & Build Isolation
## PayU Tekton Pipeline Enhancement

SLSA Level 3 requires:
1. **Hermetic builds**: Build process cannot access external network
2. **Reproducible builds**: Same inputs produce same outputs
3. **Provenance attestation**: Signed build metadata (already enabled via Tekton Chains)
4. **Build isolation**: Build steps run in isolated environments

---

## Implementation

### 1. Hermetic Build NetworkPolicy

During `TaskRun` execution, network access is restricted to internal proxy only.

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: hermetic-build-policy
  namespace: payu-cicd
spec:
  podSelector:
    matchLabels:
      tekton.dev/pipelineRun: "*"
  policyTypes:
    - Egress
  egress:
    # Allow Nexus/Artifactory internal proxy only
    - to:
        - namespaceSelector:
            matchLabels:
              name: payu-cicd
      ports:
        - protocol: TCP
          port: 8081  # Nexus proxy
    # Allow internal registry
    - to:
        - namespaceSelector:
            matchLabels:
              name: openshift-image-registry
      ports:
        - protocol: TCP
          port: 5000
    # Allow DNS resolution (cluster-internal only)
    - to:
        - namespaceSelector: {}
      ports:
        - protocol: UDP
          port: 53
```

### 2. Maven Settings for Internal Proxy

`settings.xml` di-redirect ke internal Nexus/Artifactory proxy:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>internal-nexus</id>
      <url>http://nexus.payu-cicd.svc:8081/repository/maven-public/</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### 3. Reproducible Build Labels

Buildah task di-patch untuk inject `SOURCE_DATE_EPOCH`:

```yaml
- name: BUILD_ARGS
  value:
    - SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)
    - GIT_COMMIT=$(git rev-parse HEAD)
    - BUILD_ID=$(context.pipelineRun.uid)
```

### 4. Tekton Chains Provenance (Already Enabled)

Current config:
- `artifacts.pipelinerun.format: in-toto`
- `artifacts.pipelinerun.storage: oci`

This satisfies SLSA Level 2+ provenance requirements.

### 5. Build Isolation via Affinity Assistants

Tekton affinity assistants already ensure:
- Each PipelineRun has dedicated workspace volume
- Workspace is not shared across concurrent runs
- Clean workspace per build

---

## Gap Analysis

| Requirement | Status | Gap |
|-------------|--------|-----|
| Hermetic builds | ⚠️ Partial | NetworkPolicy configured but not enforced (Nexus proxy not yet deployed) |
| Reproducible builds | ✅ | SOURCE_DATE_EPOCH injection configured |
| Provenance attestation | ✅ | Tekton Chains in-toto + OCI storage |
| Build isolation | ✅ | Tekton affinity assistants + emptyDir per TaskRun |

---

## Next Steps

1. Deploy Nexus/Artifactory internal proxy in `payu-cicd`
2. Apply `hermetic-build-policy` NetworkPolicy
3. Update `maven-java21` task to use internal proxy
4. Validate no external network access during build
5. Document reproducible build verification procedure
