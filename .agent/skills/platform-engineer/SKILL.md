---
name: platform-engineer
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: []
tags: [devops, k8s, openshift, infrastructure, gitops, argocd, tekton, helm]
related: [sre, cybersecurity-architect, integration-architect]
description: **Master Skill**: Platform & DevOps Architect for PayU. Expert in OpenShift 4.20+, Tekton Pipelines, ArgoCD (GitOps), Helm Charts, Container Hardening (UBI9), Istio Service Mesh, and Multi-Region DR.
---

# PayU Platform Architect Master Skill

You are the **Lead Platform Engineer** for the **PayU Platform**. You design and maintain the enterprise-grade automated delivery infrastructure on top of **Red Hat OpenShift 4.20+**.

---

## 🚀 GitOps & Continuous Delivery (ArgoCD)

### 1. ApplicationSet for Multi-Environment

```yaml
# argocd/applicationsets/payu-services.yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: payu-services
  namespace: argocd
spec:
  generators:
    - matrix:
        generators:
          - list:
              elements:
                - service: wallet-service
                  path: backend/wallet-service
                - service: transaction-service
                  path: backend/transaction-service
                - service: account-service
                  path: backend/account-service
          - list:
              elements:
                - env: dev
                  cluster: https://dev.ocp.payu.internal
                  namespace: payu-dev
                - env: staging
                  cluster: https://staging.ocp.payu.internal
                  namespace: payu-staging
                - env: prod
                  cluster: https://prod.ocp.payu.internal
                  namespace: payu-prod
  template:
    metadata:
      name: "{{service}}-{{env}}"
    spec:
      project: payu
      source:
        repoURL: https://github.com/payu/platform
        targetRevision: "{{env}}"
        path: "infrastructure/helm/{{path}}"
        helm:
          valueFiles:
            - values-{{env}}.yaml
      destination:
        server: "{{cluster}}"
        namespace: "{{namespace}}"
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        syncOptions:
          - CreateNamespace=true
```

### 2. Sync Windows for Production Safety

```yaml
# argocd/appproject-payu.yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: payu
  namespace: argocd
spec:
  syncWindows:
    # Allow syncs only during business hours
    - kind: allow
      schedule: "0 9 * * 1-5"  # Mon-Fri 9AM
      duration: 8h
      applications:
        - "*-prod"
      namespaces:
        - payu-prod
    # Deny weekend deployments
    - kind: deny
      schedule: "0 0 * * 0,6"  # Sat-Sun
      duration: 48h
      applications:
        - "*-prod"
  sourceRepos:
    - https://github.com/payu/*
  destinations:
    - namespace: payu-*
      server: "*"
```

### 3. Automated Rollback

```yaml
# Application with automated rollback
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
  # Health checks
  ignoreDifferences:
    - group: apps
      kind: Deployment
      jsonPointers:
        - /spec/replicas  # Allow HPA to manage
```

---

## 🔧 Tekton CI/CD Pipelines

### 1. Modular Pipeline Structure

```yaml
# tekton/pipelines/java-service-pipeline.yaml
apiVersion: tekton.dev/v1beta1
kind: Pipeline
metadata:
  name: java-service-pipeline
spec:
  params:
    - name: git-url
      type: string
    - name: git-revision
      type: string
      default: main
    - name: image-name
      type: string
    - name: service-name
      type: string
  workspaces:
    - name: source
    - name: maven-cache
    - name: docker-credentials
  tasks:
    - name: git-clone
      taskRef:
        name: git-clone
        kind: ClusterTask
      params:
        - name: url
          value: $(params.git-url)
        - name: revision
          value: $(params.git-revision)
      workspaces:
        - name: output
          workspace: source

    - name: maven-build
      taskRef:
        name: maven
        kind: ClusterTask
      runAfter:
        - git-clone
      params:
        - name: GOALS
          value: ["clean", "package", "-DskipTests"]
      workspaces:
        - name: source
          workspace: source
        - name: maven-settings
          workspace: maven-cache

    - name: unit-test
      taskRef:
        name: maven
        kind: ClusterTask
      runAfter:
        - maven-build
      params:
        - name: GOALS
          value: ["test", "-Dmaven.test.failure.ignore=false"]
      workspaces:
        - name: source
          workspace: source

    - name: sonar-scan
      taskRef:
        name: sonarqube-scanner
      runAfter:
        - unit-test
      params:
        - name: SONAR_HOST_URL
          value: https://sonar.payu.internal
      workspaces:
        - name: source
          workspace: source

    - name: trivy-scan
      taskRef:
        name: trivy-scanner
      runAfter:
        - maven-build
      params:
        - name: IMAGE
          value: $(params.image-name):$(params.git-revision)
        - name: SEVERITY
          value: "HIGH,CRITICAL"
        - name: EXIT_CODE
          value: "1"  # Fail on vulnerabilities

    - name: build-push-image
      taskRef:
        name: buildah
        kind: ClusterTask
      runAfter:
        - trivy-scan
      params:
        - name: IMAGE
          value: $(params.image-name):$(params.git-revision)
        - name: DOCKERFILE
          value: ./Dockerfile
      workspaces:
        - name: source
          workspace: source
        - name: dockerconfig
          workspace: docker-credentials

    - name: update-manifests
      taskRef:
        name: git-update-deployment
      runAfter:
        - build-push-image
      params:
        - name: GIT_REPOSITORY
          value: https://github.com/payu/platform-manifests
        - name: IMAGE_TAG
          value: $(params.git-revision)
        - name: SERVICE_NAME
          value: $(params.service-name)
```

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

## 🏗️ Container Hardening (UBI9)

### 1. Production Dockerfile Template

```dockerfile
# Dockerfile - Multi-stage build for Java service
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

### 3. OCI & Metadata Standards (Legacy Container Engineer)

Semua container image PayU **WAJIB** memiliki metadata standar untuk auditability dan traceability.

#### Dockerfile Labels (Build Time)

```dockerfile
# Standard OCI Labels
LABEL org.opencontainers.image.vendor="PayU Digital Banking" \
      org.opencontainers.image.authors="platform@payu.id" \
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
    image.openshift.io/triggers: "[{'from':{'kind':'ImageStreamTag','name':'wallet-service:latest'},'fieldPath':'spec.template.spec.containers[?(@.name==\"app\")].image'}]"
    
    # Ownership & Contact
    start.payu.id/owner: "Wallet Team <wallet@payu.id>"
    start.payu.id/slack-channel: "#dev-wallet"
    
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
    ├── values-staging.yaml
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
  namespace: payu-prod
spec:
  mtls:
    mode: STRICT
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: wallet-service-authz
  namespace: payu-prod
spec:
  selector:
    matchLabels:
      app: wallet-service
  rules:
    - from:
        - source:
            principals:
              - cluster.local/ns/payu-prod/sa/gateway-service
              - cluster.local/ns/payu-prod/sa/transaction-service
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

## 🛡️ Platform Integrity Checklist

### Security
- [ ] Dockerfile uses UBI9-minimal and non-root USER
- [ ] SecurityContext drops all capabilities
- [ ] NetworkPolicies isolate service traffic
- [ ] Secrets managed via Vault/SealedSecrets (not Git)

### Delivery
- [ ] Service deployed via ArgoCD (GitOps)
- [ ] Sync windows configured for production
- [ ] Automated rollback enabled
- [ ] Tekton pipeline includes security scanning

### Observability
- [ ] PodMonitor/ServiceMonitor configured
- [ ] Distributed tracing enabled (Jaeger)
- [ ] Log aggregation configured (Loki)

### Resilience
- [ ] PodDisruptionBudget defined
- [ ] HPA configured with appropriate thresholds
- [ ] Multi-region DR tested quarterly

---

## 📚 References

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

---
*Last Updated: January 2026*
