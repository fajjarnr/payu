# GitOps Workflow with OpenShift GitOps (ArgoCD)

PayU uses OpenShift GitOps (ArgoCD) for continuous delivery.
Follow these patterns to manage Applications and AppProjects declarative.

## 1. Application Definition

Standard Application manifest for deploying a microservice.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: wallet-service
  namespace: openshift-gitops
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: payu-services
  source:
    repoURL: https://github.com/payu/wallet-service.git
    targetRevision: HEAD
    path: charts/wallet-service
    helm:
      valueFiles:
        - values.yaml
        - values-prod.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: payu-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - PrunePropagationPolicy=foreground
```

## 2. App-of-Apps Pattern (Cluster Bootstrapping)

Manage the entire cluster configuration via a single entry point.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: cluster-bootstrap
  namespace: openshift-gitops
spec:
  project: default
  source:
    repoURL: https://github.com/payu/cluster-config.git
    targetRevision: HEAD
    path: clusters/prod
    directory:
      recurse: true
  destination:
    server: https://kubernetes.default.svc
    namespace: openshift-gitops
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

## 3. High Availability Sync Policy

For critical production workloads, we use a conservative sync policy to avoid accidental outages.

```yaml
  syncPolicy:
    automated:
      prune: false # Manual prune for safety
      selfHeal: true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

## 4. RBAC Configuration (`AppProject`)

Restrict what the application can do.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: payu-services
  namespace: openshift-gitops
spec:
  description: PayU Microservices
  sourceRepos:
  - "https://github.com/payu/*"
  destinations:
  - namespace: payu-*
    server: https://kubernetes.default.svc
  clusterResourceWhitelist:
  - group: ''
    kind: Namespace
  namespaceResourceWhitelist:
  - group: '*'
    kind: '*'
```
