# Deployment & Release Engineering Patterns

## 🚀 High Availability Best Practices - HPA, PDB, and Multi-Replica
*   **Context**: Single-replica deployments are risky. PDB ensures availability during voluntary disruptions. HPA handles traffic spikes.
*   **The "Production HA Trio"**:
    1.  **Multiple Replicas**: Minimum 2 replicas for critical services.
    2.  **Pod Disruption Budget (PDB)**: Ensure at least 1 pod remains during disruptions.
    3.  **Horizontal Pod Autoscaler (HPA)**: Auto-scale based on CPU utilization (70% target).
*   **Configuration Snippets**:
    ```yaml
    # Deployment Strategy
    strategy:
      type: RollingUpdate
      rollingUpdate:
        maxSurge: 1
        maxUnavailable: 0
    ```
    ```yaml
    # HPA
    minReplicas: 2
    maxReplicas: 5
    metrics:
      - type: Resource
        resource:
          name: cpu
          target:
            type: Utilization
            averageUtilization: 70
    ```
*   **Lessons**: Use `maxUnavailable: 0` during rolling updates. Set `terminationGracePeriodSeconds: 60` for Spring Boot.

## 🔄 Zero-Downtime Deployment Strategies
*   **Blue-Green Deployment**: Swap traffic via route patch. (~10s switch, ~30s rollback).
*   **Canary Releases**: Progressive traffic split (e.g., 10% -> 100%).
*   **Database Migration (Expand-Contract)**:
    1.  **Expand**: Add new columns/tables.
    2.  **Migrate**: Dual-write and backfill.
    3.  **Contract**: Remove old columns in next release.

## 🛡️ Rollback Decision Matrix
| Metric | Threshold | Action |
|--------|-----------|--------|
| Error Rate | > 1% | Immediate rollback |
| P95 Latency | > 500ms | Immediate rollback |
| Pod Restarts | > 2 | Immediate rollback |

## 🏗️ OpenShift Image Management
*   **Internal Registry**: Pin images to internal registry with semver (e.g., `:1.0.0` NOT `:latest`).
*   **Image Pinning Script**: Use `sed` to bulk update manifests from `:latest` to specific versions.
*   **Kustomize Transformers**: Use `images:` in overlay to remap external images to internal ones.

## ⚙️ Tekton CI/CD Tasks
Standard task set for PayU:
1.  `maven-task`: Build JAR from UBI9 OpenJDK.
2.  `buildah-task`: Rootless image build (OpenShift compatible).
3.  `deploy-task`: Patch manifest + rollout wait + health check.
4.  `trivy-task`: Security scan with severity gate.
5.  `pytest-task`: Python regression tests.
