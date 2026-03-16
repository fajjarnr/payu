# Infrastructure & Containerization Patterns

## 🐳 Podman & Container Orchestration
*   **Volume Syntax**: Use standard bind-mount or named volumes. Advanced Docker types may fail.
*   **Fully Qualified Names**: Prepend `docker.io/` to avoid interactive prompts.
*   **Local Tagging**: Always provide an `image:` tag (e.g., `localhost/payu-service`) when using `build`.
*   **Port Standardization**: All microservices MUST listen on internal port **8080**.
    *   **Dockerfile**: `EXPOSE 8080`.
    *   **App**: `server.port=8080`.
    *   **Compose**: Map host ports (8001:8080) but healthcheck `localhost:8080`.

## 🏗️ Monorepo Build Optimization
*   **Shared Library Access**: Set build `context` to `backend/` root, not service subfolder.
*   **Maven Flags**: Use `-pl :service-name -am` to build only what's needed.
*   **Decoupled Build**: For resource-constrained environments, build JAR on host and only `COPY` it in the Dockerfile.

## 🐍 Python ML Containerization (L-001)
*   **Base Image**: Use `python:3.12-slim` for ML/AI services (`kyc-service`, `analytics-service`). UBI9 `python-312` has known incompatibilities:
    *   PaddleOCR, OpenCV, PyTorch — prebuilt wheels expect Debian/glibc paths, not RHEL
    *   Missing shared libraries: `libGL.so.1` (`libGL`), `libglib-2.0.so` (`libglib`), `libgomp.so.1` (`libgomp`) require different package names on RHEL vs Debian
    *   `site-packages` path differs: `/opt/app-root/lib/` (UBI9) vs `/usr/local/lib/` (Debian slim)
*   **Rule**: Do NOT migrate Python ML services to UBI9 without full dependency compatibility testing. All Java (UBI9 OpenJDK 21) and Node.js (UBI9 Node 20) services use UBI9.
*   **Build Tool**: Use `uv` for 5x faster package installation compared to `pip`.
*   **Memory**: ML services (KYC, Analytics) need ~2GB RAM limits to avoid "Killed" (137) errors during model loading.

## 🌐 Domain Routing Strategy — Dual Ingress Architecture (L-002)

**Dual-ingress architecture** separating application traffic from platform traffic:

| Traffic Type      | Ingress Controller               | Domain Pattern                                         |
| :---------------- | :------------------------------- | :----------------------------------------------------- |
| **App (Prod)**    | Istio Ingress Gateway            | `payu.fajjjar.my.id` + `*.payu.fajjjar.my.id`          |
| **App (Dev)**     | Istio Ingress Gateway            | `*.dev.payu.fajjjar.my.id`                             |
| **App (Staging)** | Istio Ingress Gateway            | `*.staging.payu.fajjjar.my.id`                         |
| **App (SIT/UAT)** | Istio Ingress Gateway            | `*.sit.payu.fajjjar.my.id`, `*.uat.payu.fajjjar.my.id` |
| **OCP Platform**  | OCP Ingress Controller (HAProxy) | `*.apps.payu.ocp.fajjjar.my.id`                        |
| **OCP API**       | Kubernetes API                   | `api.payu.ocp.fajjjar.my.id`                           |

*   **Rule**: ALL environments use Istio Ingress Gateway for application traffic. `*.apps.payu.ocp.*` is exclusively for OCP platform components (console, image registry, ArgoCD, Grafana).
*   **Gotcha**: Beware of `apps.cluster.payu` vs `apps.payu.ocp` inconsistency — standardize early. Always replace most-specific patterns first during migration.

## 📌 Container Image Pinning (L-004)

Never use `:latest` in compose files or Quadlet containers. Pin to specific versions:

| Image       | Before         | After          |
| :---------- | :------------- | :------------- |
| Keycloak    | `:latest`      | `:26.1`        |
| kafka-ui    | `:latest`      | `:v0.7.2`      |
| timescaledb | `:latest-pg16` | `:2.17.2-pg16` |
| rustfs      | `:latest`      | `:0.3.0`       |

*   **Rule**: Every image reference must have an explicit version tag for reproducibility.

## 📚 Backstage / RHDH — Monorepo Catalog Strategy (L-005)

For a monorepo with 22+ services, use a single root `catalog-info.yaml` with YAML multi-document (`---`) separators rather than per-service files.

*   **Benefits**: Single import point in Backstage/RHDH, easier dependency graph (`dependsOn`, `providesApis`), system-level view in one place.
*   **Include**: Components (services, libraries, websites), Resources (databases, message brokers, caches), and System definition.

## 🔄 OSS Version Compatibility Matrix (L-006)

Maintain a compatibility matrix between Red Hat products and OSS equivalents:

| Red Hat Product  | OSS Equivalent | PayU Version | Compatible |
| :--------------- | :------------- | :----------- | :--------- |
| Red Hat Runtimes | Spring Boot    | 3.4.1        | ✅         |
| RHBQ             | Quarkus        | 3.17.5       | ✅         |
| Crunchy PGO      | PostgreSQL     | 16           | ✅         |
| AMQ Streams      | Apache Kafka   | 3.5 (CP 7.5) | ✅         |
| RHBK             | Keycloak       | 26.1         | ✅         |
| Data Grid (RESP) | Redis          | 7.x          | ✅         |
| RHDH             | Backstage.io   | 1.25+        | ✅         |

*   **Rule**: Verify wire compatibility when client/broker versions differ (e.g., Kafka client 3.8 ↔ broker 3.5 is safe).

## 🌐 Istio Ingress Gateway — Router Node Placement & Dual LB VIP (L-007)

When running both OCP Ingress Controller and Istio Ingress Gateway on the same cluster with dedicated router nodes:

**Architecture**: 3 router nodes with taint `node-role.kubernetes.io/router:NoSchedule`. OCP Ingress Controller pods (HAProxy) already scheduled on router nodes by OpenShift. Istio Ingress Gateway pods must explicitly opt-in:

```yaml
nodeSelector:
  node-role.kubernetes.io/router: ""
tolerations:
  - key: node-role.kubernetes.io/router
    operator: Exists
    effect: NoSchedule
podAntiAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchLabels:
          app: istio-ingressgateway
      topologyKey: kubernetes.io/hostname
```

**Dual LoadBalancer VIP separation**:

| Component                          | Ports      | DNS Target                                     |
| :--------------------------------- | :--------- | :--------------------------------------------- |
| OCP Ingress Controller (HAProxy)   | 80, 443    | `*.apps.payu.ocp.fajjjar.my.id`                |
| Istio Ingress Gateway              | 8080, 8443 | `*.payu.fajjjar.my.id` + env wildcards          |

*   **Rule**: Use separate LB VIPs with different ports (80/443 vs 8080/8443). Both coexist on the same router nodes because they bind different ports. Set `replicas: 3` and HPA `minReplicas: 3` (one per router node).

## 🧹 Podman Local Infrastructure — Storage Management (L-014)

Local development with 22+ microservices, Postgres, Kafka, and large ML images rapidly consumes disk space.

**Symptoms**: `mvn` build failures during artifact download, Postgres failing to write temp files, `podman-compose up` failing to pull/build images.

**Recommended Cleanup Ritual**:
1.  `podman system prune -f` (removes unused containers/networks)
2.  `podman builder prune -f` (cleans build cache)
3.  `rm -rf ~/.m2/repository` (if repo is corrupted or too large)
4.  `rm -rf /tmp/*` (cleans temporary build artifacts)

*   **Rule**: Monitor disk space with `df -h` and keep at least 10GB free for stable local multi-service orchestration.

## ☁️ OpenShift Specifics
*   **NetworkPolicy**: Ensure pods have `app.kubernetes.io/part-of: payu-banking` label to allow intra-namespace traffic. Default router policy might block pod-to-pod calls.
*   **DataGrid (Infinispan)**: RESP connector does NOT support custom `port: 6379`. It uses the default `11222`.
*   **Vault Dev Mode**: Use simple `Deployment` (not StatefulSet). Set `VAULT_ADDR=http://127.0.0.1:8200` for healthchecks.
*   **ExternalName DNS**: Always use FQDN (e.g., `...svc.cluster.local`) for ExternalName services to avoid NXDOMAIN errors.
