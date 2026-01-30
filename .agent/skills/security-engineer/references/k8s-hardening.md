# Kubernetes Security Hardening Guide

Standard security constraints for PayU workloads on OpenShift/Kubernetes.

## 1. Network Policies (Zero Trust)

PayU adopts a "Default Deny" posture. All traffic must be explicitly allowed.

### Template: Default Deny All (Apply to all namespaces)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

### Template: Allow Essential Infra (DNS & Monitoring)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-infra
spec:
  podSelector: {}
  policyTypes:
  - Egress
  - Ingress
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: kube-system
    ports:
    - protocol: UDP
      port: 53 # DNS
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: openshift-monitoring # or monitoring
    ports:
    - protocol: TCP
      port: 9090 # Prometheus Metrics
```

### Template: Service-to-Service (Frontend -> Backend)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-frontend-to-backend
spec:
  podSelector:
    matchLabels:
      app: backend-service
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: frontend-service
    ports:
    - protocol: TCP
      port: 8080
```

## 2. RBAC Best Practices (Least Privilege)

### Anti-Patterns
*   ❌ **ClusterAdmin**: Never use `cluster-admin` for application ServiceAccounts.
*   ❌ **Wildcards**: Avoid `verbs: ["*"]` or `resources: ["*"]`.
*   ❌ **User Impersonation**: Apps should not have `impersonate` permissions.

### Pattern: Secret Reader (Scoped)
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
  namespace: production
rules:
  - apiGroups: [""]
    resources: ["secrets"]
    verbs: ["get"]
    resourceNames: ["app-credentials"] # Constrain to specific secret
```

## 3. Pod Security Standards (Restricted)

All PayU workloads must comply with the `Restricted` profile (enforced via OpenShift SCC).

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 10001 # Assigned by OpenShift
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: app
    image: payu/app:1.0.0
    securityContext:
      allowPrivilegeEscalation: false
      capabilities:
        drop: ["ALL"]
      readOnlyRootFilesystem: true
```
