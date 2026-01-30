# External Secrets Operator (ESO) Pattern

PayU uses External Secrets Operator to synchronize secrets from HashiCorp Vault to OpenShift/Kubernetes `Secret` objects.

## 1. SecretStore Configuration

Connects OpenShift namespace to Vault.

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: vault-backend
  namespace: payu-prod
spec:
  provider:
    vault:
      server: "https://vault.payu.internal:8200"
      path: "secret"
      version: "v2"
      auth:
        # Authenticate via Kubernetes ServiceAccount
        kubernetes:
          mountPath: "kubernetes"
          role: "payu-prod-role"
          serviceAccountRef:
            name: output-vault-sa
```

## 2. ExternalSecret Definition

Maps specific Vault paths to Kubernetes Secrets.

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: db-credentials
  namespace: payu-prod
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: db-credentials-k8s # Name of the K8s Secret to create
    creationPolicy: Owner
  data:
    - secretKey: username
      remoteRef:
        key: payu/database/config
        property: username
    - secretKey: password
      remoteRef:
        key: payu/database/config
        property: password
```

## 3. Usage in Deployment

Mount the generated K8s Secret.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wallet-service
spec:
  template:
    spec:
      containers:
        - name: app
          env:
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-credentials-k8s
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-credentials-k8s
                  key: password
```

## Best Practices
1.  **Least Privilege**: Vault Roles should restrict access to only specific paths (e.g., `secret/data/payu/prod/*`).
2.  **Refresh Interval**: Avoid setting `refreshInterval` too low (e.g., < 1m) to prevent overloading Vault.
3.  **Immutable Secrets**: If possible, treat K8s secrets as immutable and restart pods on rotation (using Reloader or Stakater).
