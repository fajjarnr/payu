---
name: platform-engineer
version: 3.0.0
maturity: stable
updated: 2026-04-20
author: payu-platform-team
requires: []
tags: [devops, k8s, openshift, infrastructure, gitops, argocd, tekton, helm, sre, reliability, releases, feature-flags]
related: [cybersecurity-architect, integration-architect, finops-engineer]
description: **Master Skill**: Unified Platform, SRE & Release Engineering. Covers OpenShift 4.20+, GitOps (ArgoCD/Tekton), Container Hardening, Service Mesh, Feature Flags, Progressive Rollouts, Observability (LGTM Stack), Chaos Engineering, and Disaster Recovery.
---

## 📚 Reference Implementation Patterns

For detailed patterns and historical context on PayU infrastructure, see:

- [Infrastructure & Container Patterns](./references/INFRASTRUCTURE_PATTERNS.md)
- [Deployment & Release Patterns](./references/DEPLOYMENT_PATTERNS.md)

# PayU Platform Architect Master Skill

You are the **Lead Platform Engineer** for the **PayU Platform**. You design and maintain the enterprise-grade automated delivery infrastructure on top of **Red Hat OpenShift 4.20+**.

## ⚡ 2026 Platform Engineering Trends

1. **Internal Developer Portal (IDP)**: Backstage/Red Hat Developer Hub is the golden path interface.
2. **eBPF Observability**: Using Pixie/Cilium for zero-instrumentation monitoring.
3. **GreenOps**: Carbon-aware scheduling for batch jobs.
4. **Policy as Code**: Kyverno/OPA for strict governance enforcement at the cluster level.
5. **Container Port Standardization**: All 22 microservices MUST listen on internal port **8080** to simplify networking, healthchecks, and service mesh routing.

---

## 🚀 GitOps & Continuous Delivery (ArgoCD)

### 1. ApplicationSet for Multi-Environment

```yaml
# infrastructure/platform/argocd-gitops/applicationsets/payu-applicationsets.yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: payu-environments
  namespace: openshift-gitops
spec:
  goTemplate: true
  goTemplateOptions:
    - missingkey=error
  generators:
    - list:
        elements:
          - name: dev
            path: infrastructure/workloads/overlays/dev
            namespace: payu-dev
            project: payu-dev
          - name: sit
            path: infrastructure/workloads/overlays/sit
            namespace: payu-sit
            project: payu-sit
          - name: uat
            path: infrastructure/workloads/overlays/uat
            namespace: payu-uat
            project: payu-uat
          - name: preprod
            path: infrastructure/workloads/overlays/preprod
            namespace: payu-preprod
            project: payu-preprod
          - name: prod
            path: infrastructure/workloads/overlays/prod
            namespace: payu
            project: payu
  template:
    metadata:
      name: "payu-{{.name}}"
    spec:
      project: "{{.project}}"
      source:
        repoURL: https://github.com/fajjarnr/payu.git
        targetRevision: main
        path: "{{.path}}"
      destination:
        server: https://kubernetes.default.svc
        namespace: "{{.namespace}}"
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        syncOptions:
          - CreateNamespace=true
          - RespectIgnoreDifferences=true
```

Repo alignment note:
- Stable environments are generated from `infrastructure/workloads/overlays/{dev,sit,uat,preprod,prod}`.
- Preview environments must override namespace to `payu-dev-pr-*` because the dev overlay hardcodes `payu-dev`.

### 2. Sync Windows for Production Safety

```yaml
# infrastructure/platform/argocd-gitops/projects/payu-projects.yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: payu
  namespace: openshift-gitops
spec:
  sourceRepos:
    - https://github.com/fajjarnr/payu.git
  destinations:
    - namespace: payu
      server: https://kubernetes.default.svc
  syncWindows:
    - kind: allow
      schedule: "0 1 * * 1-5"
      duration: 8h
      applications:
        - payu-prod
      namespaces:
        - payu
    - kind: deny
      schedule: "0 0 * * 0,6"
      duration: 24h
      applications:
        - payu-prod
      namespaces:
        - payu
```

### 3. Automated Rollback

```yaml
# infrastructure/platform/argocd-gitops/applicationsets/payu-applicationsets.yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
spec:
  template:
    spec:
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        retry:
          limit: 5
          backoff:
            duration: 5s
            factor: 2
            maxDuration: 3m
      ignoreDifferences:
        - group: apps
          kind: Deployment
          jqPathExpressions:
            - .spec.template.metadata.annotations
```

---

## 🔧 Tekton CI/CD Pipelines

### 1. Modular Pipeline Structure

```yaml
# infrastructure/platform/tekton-pipelines/build-pipeline.yaml
apiVersion: tekton.dev/v1
kind: Pipeline
metadata:
  name: payu-build-pipeline
  namespace: payu-cicd
spec:
  tasks:
    - name: fetch-repository
      taskRef:
        name: git-clone

    - name: secret-scan
      runAfter: [fetch-repository]
      taskRef:
        name: gitleaks

    - name: deep-secret-scan
      runAfter: [fetch-repository]
      taskRef:
        name: trufflehog

    - name: semgrep-scan
      runAfter: [fetch-repository]
      taskRef:
        name: semgrep

    - name: service-sast-sca
      runAfter: [semgrep-scan]
      taskRef:
        name: security-scan

    - name: build-image
      runAfter: [secret-scan, deep-secret-scan, service-sast-sca]
      taskRef:
        name: buildah

    - name: trivy-image-scan
      runAfter: [build-image]
      taskRef:
        name: trivy

    - name: rhacs-policy-check
      runAfter: [trivy-image-scan]
      taskRef:
        name: rhacs-image-check

    - name: generate-sbom
      runAfter: [rhacs-policy-check]
      taskRef:
        name: syft-sbom

    - name: grype-sbom-check
      runAfter: [generate-sbom]
      taskRef:
        name: grype-scan

    - name: sign-image
      runAfter: [grype-sbom-check]
      taskRef:
        name: cosign-sign
```

Repo alignment note:
- PayU build pipelines in `payu-cicd` enforce `gitleaks -> trufflehog -> semgrep -> service SAST/SCA -> build -> trivy -> RHACS -> Syft -> Grype -> Cosign`.
- Deploy pipelines gate environment promotion with Argo sync wait plus ZAP/Litmus in SIT, Schemathesis/k6 in UAT, and Cerberus/Kraken in preprod.

### 2. Pipeline Trigger for Git Events

```yaml
# tekton/triggers/github-push-trigger.yaml
apiVersion: triggers.tekton.dev/v1beta1
kind: TriggerTemplate
metadata:
  name: java-service-trigger
spec:
  params:
    - name: gitrevision
    - name: gitrepositoryurl
    - name: servicename
  resourcetemplates:
    - apiVersion: tekton.dev/v1beta1
      kind: PipelineRun
      metadata:
        generateName: "$(tt.params.servicename)-"
      spec:
        pipelineRef:
          name: java-service-pipeline
        params:
          - name: git-url
            value: $(tt.params.gitrepositoryurl)
          - name: git-revision
            value: $(tt.params.gitrevision)
          - name: service-name
            value: $(tt.params.servicename)
        workspaces:
          - name: source
            volumeClaimTemplate:
              spec:
                accessModes:
                  - ReadWriteOnce
                resources:
                  requests:
                    storage: 1Gi
---
apiVersion: triggers.tekton.dev/v1beta1
kind: EventListener
metadata:
  name: github-listener
spec:
  serviceAccountName: tekton-triggers-sa
  triggers:
    - name: github-push
      interceptors:
        - ref:
            name: github
          params:
            - name: secretRef
              value:
                secretName: github-webhook-secret
                secretKey: token
            - name: eventTypes
              value: ["push"]
      bindings:
        - ref: github-push-binding
      template:
        ref: java-service-trigger
```

---

## 🏗️ Container Hardening (Podman/UBI9)

PayU menggunakan **Podman** secara eksklusif karena arsitekturnya yang _daemonless_ dan kemampuan eksekusi _rootless_ secara native, yang jauh lebih aman dibanding Docker.

### 1. Production Containerfile Template

```dockerfile
# Containerfile (Podman) - Multi-stage build for Java service
# Stage 1: Build
FROM registry.access.redhat.com/ubi9/openjdk-21:1.18 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.repo.local=/build/.m2

# Stage 2: Runtime (minimal)
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.3

# Security: Create non-root user
RUN microdnf install -y java-21-openjdk-headless shadow-utils && \
    microdnf clean all && \
    groupadd -r payu -g 1001 && \
    useradd -r -g payu -u 1001 -d /app payu

WORKDIR /app

# Copy only the built artifact
COPY --from=builder --chown=payu:payu /build/target/*.jar app.jar

# Security: Run as non-root
USER 1001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Security: Drop all capabilities
# Read-only root filesystem
# No new privileges
EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### 2. Security Context in Kubernetes

```yaml
# deployment.yaml
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        runAsGroup: 1001
        fsGroup: 1001
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: app
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          volumeMounts:
            - name: tmp
              mountPath: /tmp
            - name: logs
              mountPath: /app/logs
      volumes:
        - name: tmp
          emptyDir: {}
        - name: logs
          emptyDir: {}
```

### 3. SELinux Guardrails (Red Hat Best Practices)

Platform PayU mengandalkan SELinux untuk pertahanan _Enforced_ secara default. Jangan pernah mematikan SELinux (`setenforce 0`) di lingkungan produksi.

#### Volume Labeling (`:Z` vs `:z`)

Saat mounting volume di Podman, label SELinux harus dikelola agar proses kontainer memiliki izin akses.

- **`:Z`**: Private unshared volume. Mencegah kontainer lain mengakses data ini. (Direkomendasikan).
- **`:z`**: Shared volume. Bisa diakses oleh beberapa kontainer.

```bash
# Contoh running rootless podman dengan SELinux labeling
podman run -v /data/db:/var/lib/postgresql/data:Z postgres:16
```

#### OpenShift MCS (Multi-Category Security)

Di OpenShift, setiap namespace mendapatkan kategori SELinux yang unik (misal: `s0:c12,c34`). Ini mencegah kontainer di Namespace A mengakses volume di Namespace B meskipun UUID-nya sama.

#### Security Context Constraints (SCC)

Gunakan SCC `restricted-v2` (default di OCP 4.12+) yang secara otomatis:

1. Mengalokasikan UID unik dari range namespace.
2. Menerapkan tipe SELinux `container_t`.
3. Memaksa penggunaan `seccompProfile` tipe `RuntimeDefault`.

#### Troubleshooting Commands

Jika terjadi `Permission Denied` meskipun permission file di host (Linux) sudah `777`:

1. Cek audit log: `ausearch -m avc -ts recent`
2. Lihat konteks file: `ls -Z /path/to/data`
3. Perbaiki label: `restorecon -Rv /path/to/data`

---

## ⚓ Platform Port Standardization

All PayU backend services follow the **8080 Standard** for internal container networking. This reduces configuration complexity and aligns with OpenShift/Kubernetes networking patterns.

### 1. Port Mapping Principles

- **Internal Port**: Always **8080**. All applications (Spring Boot, Quarkus, FastAPI) must listen on this port inside the container.
- **External Port**: Managed via `docker-compose` or `podman-compose` host mapping (e.g., `8001:8080`).
- **Service Discovery**: Internal communication between containers uses the service name and port 8080 (e.g., `http://account-service:8080`).

### 2. Implementation Checklist

- [x] **Dockerfile**: `EXPOSE 8080`.
- [x] **Application Config**: `server.port=8080` (Spring) or `quarkus.http.port=8080`.
- [x] **Health Check**: Endpoint must be matched to port 8080 (e.g., `http://localhost:8080/actuator/health`).
- [x] **Gateway Routes**: All `ROUTES_URL` must point to port 8080 of the target service.

  ***

### 4. OCI & Metadata Standards (Legacy Container Engineer)

Semua container image PayU **WAJIB** memiliki metadata standar untuk auditability dan traceability, menggunakan standar OCI (Open Container Initiative).

#### Containerfile Labels (Build Time)

```dockerfile
# Standard OCI Labels
LABEL org.opencontainers.image.vendor="PayU Digital Banking" \
      org.opencontainers.image.authors="platform@payu.fajjjar.my.id" \
      org.opencontainers.image.title="Wallet Service" \
      org.opencontainers.image.description="Core ledger and balance management service" \
      org.opencontainers.image.licenses="Proprietary" \
      org.opencontainers.image.source="https://github.com/payu/wallet-service" \
      org.opencontainers.image.documentation="https://docs.payu.internal/services/wallet" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${GIT_COMMIT}"

# PayU Specific Metadata
LABEL id.payu.service.tier="1" \
      id.payu.service.domain="transaction" \
      id.payu.compliance.pci-dss="true" \
      id.payu.security.scan-level="critical"
```

#### Kubernetes Annotations (Runtime)

```yaml
metadata:
  annotations:
    # Build Info
    image.openshift.io/triggers: '[{''from'':{''kind'':''ImageStreamTag'',''name'':''wallet-service:latest''},''fieldPath'':''spec.template.spec.containers[?(@.name=="app")].image''}]'

    # Ownership & Contact
    start.payu.fajjjar.my.id/owner: "Wallet Team <wallet@payu.fajjjar.my.id>"
    start.payu.fajjjar.my.id/slack-channel: "#dev-wallet"

    # Operational Metadata
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/actuator/prometheus"

    # Documentation
    link.argocd.argoproj.io/external-link: "https://docs.payu.internal/services/wallet"
```

---

## 📦 Helm Chart Standards

### 1. Chart Structure

```
helm/
└── wallet-service/
    ├── Chart.yaml
    ├── values.yaml
    ├── values-dev.yaml
  ├── values-sit.yaml
    ├── values-prod.yaml
    ├── templates/
    │   ├── _helpers.tpl
    │   ├── deployment.yaml
    │   ├── service.yaml
    │   ├── configmap.yaml
    │   ├── secret.yaml
    │   ├── hpa.yaml
    │   ├── pdb.yaml
    │   ├── networkpolicy.yaml
    │   ├── servicemonitor.yaml
    │   └── NOTES.txt
    └── tests/
        └── test-connection.yaml
```

### 2. Values Schema

```yaml
# values.yaml
replicaCount: 2

image:
  repository: registry.payu.internal/payu/wallet-service
  tag: "latest"
  pullPolicy: IfNotPresent

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilization: 70
  targetMemoryUtilization: 80

podDisruptionBudget:
  enabled: true
  minAvailable: 1

networkPolicy:
  enabled: true
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: payu-gateway
      ports:
        - port: 8080

monitoring:
  enabled: true
  path: /actuator/prometheus
  port: 8080
```

---

## 🔗 Service Mesh (Istio)

### 1. Traffic Management

```yaml
# VirtualService for Canary Deployment
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: wallet-service
spec:
  hosts:
    - wallet-service
  http:
    - match:
        - headers:
            x-canary:
              exact: "true"
      route:
        - destination:
            host: wallet-service
            subset: canary
          weight: 100
    - route:
        - destination:
            host: wallet-service
            subset: stable
          weight: 90
        - destination:
            host: wallet-service
            subset: canary
          weight: 10
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: wallet-service
spec:
  host: wallet-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
  subsets:
    - name: stable
      labels:
        version: stable
    - name: canary
      labels:
        version: canary
```

### 2. Mutual TLS (mTLS) Strict Mode

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: payu
spec:
  mtls:
    mode: STRICT
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: wallet-service-authz
  namespace: payu
spec:
  selector:
    matchLabels:
      app: wallet-service
  rules:
    - from:
        - source:
            principals:
              - cluster.local/ns/payu/sa/gateway-service
              - cluster.local/ns/payu/sa/transaction-service
      to:
        - operation:
            methods: ["GET", "POST", "PUT"]
            paths: ["/api/*"]
```

---

## 🌍 Multi-Region Disaster Recovery

### 1. Architecture Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│                     Global Load Balancer (GSLB)                  │
│                    (Cloudflare/AWS Route53)                      │
└─────────────────────────┬───────────────────────────────────────┘
                          │
          ┌───────────────┴───────────────┐
          │                               │
          ▼                               ▼
┌─────────────────────┐       ┌─────────────────────┐
│   Region 1 (Active)  │       │  Region 2 (Standby)  │
│   Jakarta DC         │       │  Singapore DC        │
├─────────────────────┤       ├─────────────────────┤
│ OpenShift Cluster   │       │ OpenShift Cluster    │
│ - All services      │──────▶│ - All services       │
│ - Kafka (Primary)   │ Sync  │ - Kafka (Mirror)     │
│ - PostgreSQL (RW)   │──────▶│ - PostgreSQL (RO)    │
│ - Redis (Master)    │──────▶│ - Redis (Replica)    │
└─────────────────────┘       └─────────────────────┘
```

### 2. Failover Configuration

```yaml
# Multi-region Kafka MirrorMaker2
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaMirrorMaker2
metadata:
  name: payu-mm2
spec:
  version: 3.6.0
  replicas: 3
  connectCluster: "region-2"
  clusters:
    - alias: "region-1"
      bootstrapServers: kafka-region1.payu.internal:9092
    - alias: "region-2"
      bootstrapServers: kafka-region2.payu.internal:9092
  mirrors:
    - sourceCluster: "region-1"
      targetCluster: "region-2"
      sourceConnector:
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
      topicsPattern: "payu.*"
```

---

## 💰 Cloud FinOps

### 1. Resource Right-Sizing with VPA

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: wallet-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wallet-service
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
      - containerName: "*"
        minAllowed:
          cpu: 100m
          memory: 256Mi
        maxAllowed:
          cpu: 2
          memory: 4Gi
```

### 2. Cost Attribution Labels

```yaml
# Required labels for all resources
metadata:
  labels:
    app.kubernetes.io/name: wallet-service
    app.kubernetes.io/version: "1.2.3"
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: payu-platform
    cost-center: platform-team
    environment: prod
    owner: wallet-team
```

---

## 🐛 Container Build Debugging (Podman/UBI9)

> **Learned from**: E2E test infrastructure setup - February 1, 2026

### Common Build Failure Patterns

#### 1. Parent POM Resolution Failure

**Symptom:** Maven build fails with `Could not resolve dependencies` or `parent POM not found`

**Root Cause:** Containerfile copies only service pom.xml, but Spring Boot services reference parent POM at `../pom.xml`

```dockerfile
# ❌ WRONG - Only copies service pom.xml
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src

# ✅ CORRECT - Copies entire project for parent POM access
COPY . .
RUN mvn clean package -DskipTests
```

**Fix:** Change `COPY pom.xml ./` to `COPY . .` in Containerfiles

#### 2. Maven Build Hanging (4+ hours)

**Symptom:** Maven build process hangs indefinitely during dependency download or compilation

**Root Cause:**

- Parallel builds (`-T 1C`) causing deadlock in certain services
- Network issues accessing Maven Central during container build
- Large dependency downloads timing out

**Fix - Use Pre-Built JARs:**

```dockerfile
# Build stage: Skip Maven, use pre-built JAR
# Runtime stage only
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Copy pre-built JAR from local build
COPY target/*.jar /app/app.jar

USER 1001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Build Strategy:**

1. Build all JARs first with Maven from backend directory:

   ```bash
   cd /home/ubuntu/payu/backend
   mvn clean package -DskipTests -T 1C
   ```

2. Create runtime-only Containerfiles that copy pre-built JARs
3. Build images much faster (minutes vs hours)

#### 3. UBI9 Runtime Image Conflicts

**Symptom:** `curl-minimal` conflicts when trying to install curl

**Root Cause:** UBI9 runtime images have `curl-minimal` pre-installed, conflicts with installing regular curl

**Fix:** Remove curl installation and curl-based health checks from Containerfiles, or use `curl-minimal` for health checks:

```dockerfile
# ❌ WRONG - Tries to install curl (conflicts)
RUN microdnf install -y curl

# ✅ CORRECT - curl-minimal already available
HEALTHCHECK CMD curl-minimal --fail-with-body http://localhost:8080/actuator/health || exit 1
```

#### 4. User Creation Conflicts (GID 185)

**Symptom:** `groupadd: GID '185' already exists` when creating non-root user

**Root Cause:** UBI9 images already have user `jboss` with GID 185

**Fix:** Use existing `jboss` user (UID 185) instead of creating new user:

```dockerfile
# ❌ WRONG - Tries to create user with GID 185
RUN groupadd -r payu -g 1001 && \
    useradd -r -g payu -u 1001 -d /app payu

# ✅ CORRECT - Use existing jboss user
USER 185
```

#### 5. Dockerfile Excludes Target Directory

**Symptom:** `COPY target/*.jar /app/app.jar` fails with "no such file or directory"

**Root Cause:** `.dockerignore` or `.containerignore` excludes `target/` directory

**Fix:** Either:

1. Build from parent directory with proper context
2. Remove `target/` from ignore files
3. Use `--ignorefile=.containerignore` to bypass dockerignore

### Debugging Commands

```bash
# Check if parent POM is accessible
cd backend/some-service
cat ../pom.xml  # Should show parent POM content

# Check Maven can resolve parent
mvn help:evaluate -Dexpression=project.parentGroupId
mvn help:evaluate -Dexpression=project.parentArtifactId

# Check what's in target directory
ls -la target/ | grep -E "\.jar$"

# Test Maven build locally (without container)
mvn clean package -DskipTests

# Check dockerignore
cat .dockerignore | grep target
```

### Build Performance Optimization

| Strategy                     | Build Time        | Disk Space | Use When                    |
| ---------------------------- | ----------------- | ---------- | --------------------------- |
| **Full container build**     | 10-30 min/service | High       | Initial setup, CI/CD        |
| **Pre-built JARs**           | 1-2 min/service   | Medium     | Development, fast iteration |
| **Multi-stage with cache**   | 5-10 min/service  | Medium     | Production, optimized       |
| **Runtime-only (local JAR)** | <1 min/service    | Low        | Debugging, testing          |

### PayU Build Standards

1. **All Spring Boot services** use `payu-backend-parent` (not direct `spring-boot-starter-parent`)
2. **Containerfiles** use `COPY . .` for parent POM resolution
3. **Non-root user** with UID 1001 or existing `jboss` user (185)
4. **UBI9 images**: `ubi9/openjdk-21:1.24-2` for build, `ubi9/openjdk-21-runtime:1.24-2` for runtime
5. **Node.js images**: `ubi9/nodejs-20:9.7` for frontend

### Known Working Services

| Service             | Image                            | Build Method  |
| ------------------- | -------------------------------- | ------------- |
| account-service     | ✅ payu-account-service:test     | Pre-built JAR |
| auth-service        | ✅ payu-auth-service:test        | Pre-built JAR |
| wallet-service      | ✅ payu-wallet-service:test      | Pre-built JAR |
| transaction-service | ✅ payu-transaction-service:test | Pre-built JAR |
| investment-service  | ✅ payu-investment-service:test  | Pre-built JAR |
| gateway-service     | ✅ payu-gateway-service:test     | Pre-built JAR |
| bi-fast-simulator   | ✅ payu-bifast-simulator:test    | Pre-built JAR |
| dukcapil-simulator  | ✅ payu-dukcapil-simulator:test  | Full build    |
| qris-simulator      | ✅ payu-qris-simulator:test      | Pre-built JAR |

### References

- [Podman Documentation](https://docs.podman.io/)
- [UBI9 Container Guide](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/9/html/building_running_and_managing_containers/)
- [Spring Boot Docker Guide](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#container-images)

---

## 🛡️ Platform Integrity Checklist

### Security

- [ ] Containerfile menggunakan UBI9-minimal dan non-root USER

- [ ] Dijalankan menggunakan Podman rootless (UID 1001)
- [ ] SecurityContext drops all capabilities
- [ ] NetworkPolicies isolate service traffic
- [ ] Secrets managed via Vault + External Secrets Operator (not Git)

### Delivery

- [ ] Service deployed via ArgoCD (GitOps)

- [ ] Sync windows configured for production
- [ ] Automated rollback enabled
- [ ] Tekton pipeline includes secret scan, SAST/SCA, SBOM, vulnerability gating, and Cosign signing

### Observability

- [ ] PodMonitor/ServiceMonitor configured

- [ ] Distributed tracing enabled (Jaeger/OpenTelemetry)
- [ ] Log aggregation configured (Loki)
- [ ] eBPF probes enabled for network visibility

### Resilience

- [ ] PodDisruptionBudget defined

- [ ] HPA configured with appropriate thresholds
- [ ] Multi-region DR tested quarterly
- [ ] Chaos testing run in SIT automatically

---

## 📚 References

### Merged Skill References (Consolidated)

| Category     | Topic                                                  | File                                                                   |
| ------------ | ------------------------------------------------------ | ---------------------------------------------------------------------- |
| **Releases** | Feature Flags, Progressive Rollouts, Blue-Green/Canary | [release-engineering.md](./references/releases/release-engineering.md) |
| **SRE**      | Observability, SLO/SLI, Chaos Engineering, DR          | [sre-practices.md](./references/sre/sre-practices.md)                  |
| **K8s**      | Kubernetes manifest generator patterns                 | [k8s-manifest-generator.md](./references/k8s-manifest-generator.md)    |

### External Documentation

- [OpenShift Documentation](https://docs.openshift.com/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [Tekton Documentation](https://tekton.dev/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Istio Documentation](https://istio.io/latest/docs/)
- [Strimzi Kafka Operator](https://strimzi.io/documentation/)
- [UBI9 Container Guide](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/9/html/building_running_and_managing_containers/)
- [Kubernetes Security Best Practices](https://kubernetes.io/docs/concepts/security/)
- [CNCF Landscape](https://landscape.cncf.io/)
- [FinOps Foundation](https://www.finops.org/)
- [Google SRE Book](https://sre.google/sre-book/table-of-contents/)
- [LaunchDarkly Feature Flags](https://docs.launchdarkly.com/)

---

_Last Updated: January 2026_

## 🧠 Lessons Learned (Session Log)

### L-001: Python ML/AI Services — Stay on Debian Slim, Not UBI9

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Platform

UBI9 `python-312` has known compatibility issues with native ML/AI dependencies:

- PaddleOCR, OpenCV, PyTorch — prebuilt wheels expect Debian/glibc paths
- Missing shared libraries (`libGL`, `libglib`, `libgomp`) require different package names on RHEL
- `site-packages` path differs (`/opt/app-root/lib/` vs `/usr/local/lib/`)

**Decision**: Keep `python:3.12-slim` for `kyc-service` and `analytics-service`. All Java (UBI9 OpenJDK 21) and Node.js (UBI9 Node 20) services use UBI9.

### L-002: Domain Routing Strategy — Gateway API + Istio Ingress (Updated)

**Date**: February 26, 2026 (Updated) | **Severity**: Critical | **Domain**: Infrastructure

**Dual-ingress architecture** separating application traffic from platform traffic:

| Traffic Type     | Ingress Controller               | Domain Pattern                                | Example                                                 |
| :--------------- | :------------------------------- | :-------------------------------------------- | :------------------------------------------------------ |
| **App (Prod)**   | Istio Ingress Gateway            | `payu.fajjjar.my.id` + `*.payu.fajjjar.my.id` | `api.payu.fajjjar.my.id`                                |
| **App (Dev)**    | Istio Ingress Gateway            | `*.dev.payu.fajjjar.my.id`                    | `api.dev.payu.fajjjar.my.id`                            |
| **OCP Platform** | OCP Ingress Controller (HAProxy) | `*.apps.payu.ocp.fajjjar.my.id`               | `console-openshift-console.apps.payu.ocp.fajjjar.my.id` |

**Rule**: ALL environments use Istio Ingress Gateway for application traffic. `*.apps.payu.ocp.*` is exclusively for OCP platform components.

### L-003: Domain Migration — Scope & Safe Replacement

**Date**: February 26, 2026 | **Severity**: High | **Domain**: DevOps

When doing bulk domain replacement across a monorepo:

1. **Order matters**: Replace most-specific patterns first (`staging-api.payu.id` before `payu.id`)
2. **Preserve intentionally different domains**: `payu.local` (mesh trust), `payu.internal` (internal DNS)
3. **Always verify with negative grep**: After replacement, confirm zero stray references remain

### L-004: Container Image Pinning

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Platform

Never use `:latest` in compose files or Quadlet containers. Pin to specific versions:

- Keycloak: `:26.1`
- kafka-ui: `:v0.7.2`
- timescaledb: `:2.17.2-pg16`

**Rule**: Every image reference must have an explicit version tag for reproducibility.

### L-007: Istio Ingress Gateway — Router Node Placement

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Infrastructure

When running both OCP Ingress Controller and Istio Ingress Gateway on the same cluster:

- 3 router nodes with taint `node-role.kubernetes.io/router:NoSchedule`
- Istio Ingress Gateway pods must explicitly opt-in with `nodeSelector` + `tolerations`.
- Use separate LB VIPs with different ports (80/443 vs 8080/8443).

### L-011: .gitignore `out/` vs Hexagonal `port/out/`

**Date**: March 14, 2026 | **Severity**: Critical | **Domain**: Platform / Architecture

A root `.gitignore` entry `out/` matched all `port/out/` directories in the Hexagonal Architecture.
**Fix**: Use `/out/` (root-only) instead of `out/` (recursive).

### L-014: Podman Local Infrastructure — Storage Management

**Date**: March 15, 2026 | **Severity**: Medium | **Domain**: Platform / DevOps

Local development with 22+ services rapidly consumes disk space.
**Cleanup Ritual**:

1. `podman system prune -f`
2. `podman builder prune -f`
3. `rm -rf ~/.m2/repository` (if corrupted)
4. `rm -rf /tmp/*`
