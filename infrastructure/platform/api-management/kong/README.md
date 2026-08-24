# Kong Gateway (OSS) — PayU Deployment Template

> **Status**: Template only — deploy when >=5 partners are active (see ADR-0014)

## Overview

This directory contains Helm values and plugin configuration for deploying Kong Gateway
(open-source) as the Tier 1 API management layer in front of the PayU gateway-service.

### Architecture (2-Tier)

```
Partner Apps
    │
    ▼
┌──────────────────────────────────┐
│  Kong Gateway (Tier 1)           │
│  - Kong Ingress Controller       │
│  - Rate Limiting (per partner)   │
│  - API Key Authentication        │
│  - Request Transform (headers)   │
│  - Logging & Analytics           │
└──────────────┬───────────────────┘
               │  X-PayU-Partner-Id
               │  X-PayU-Plan-Id
               │  X-PayU-Request-Id
               ▼
┌──────────────────────────────────┐
│  PayU Gateway Service (Tier 2)   │
│  - SNAP-BI Compliance            │
│  - HMAC Signing & JWT Auth       │
│  - Idempotency Enforcement       │
│  - Circuit Breaker / Retry       │
└──────────────┬───────────────────┘
               │
               ▼
         Backend Services
```

## Prerequisites

- OpenShift 4.22+ or Kubernetes cluster
- Helm 3.x installed
- `kong` Helm chart repo added
- PostgreSQL for Kong datastore (or DB-less mode for declarative config)

## Files

| File                    | Description                                          |
| :---------------------- | :--------------------------------------------------- |
| `values.yaml`           | Helm values override for Kong Ingress Controller     |
| `kong-plugin-payu.yaml` | KongPlugin CRD for PayU header injection and routing |

## Installation Steps

### 1. Add Kong Helm Repository

```bash
helm repo add kong https://charts.konghq.com
helm repo update
```

### 2. Create Namespace

```bash
oc new-project payu-api-management
# or: kubectl create namespace payu-api-management
```

### 3. Create Secrets

```bash
# PostgreSQL connection for Kong (if using DB mode)
oc create secret generic kong-postgresql \
  --from-literal=password=<KONG_DB_PASSWORD> \
  -n payu-api-management

# Kong admin API credentials
oc create secret generic kong-admin-token \
  --from-literal=token=<KONG_ADMIN_TOKEN> \
  -n payu-api-management
```

### 4. Deploy Kong via Helm

```bash
helm install kong kong/ingress \
  -f values.yaml \
  -n payu-api-management \
  --wait --timeout 5m
```

### 5. Apply PayU Plugin Configuration

```bash
oc apply -f kong-plugin-payu.yaml -n payu-api-management
```

### 6. Configure PayU Gateway as Upstream

```bash
# Create a Kong Service pointing to PayU gateway
# (if not using KongIngress CRDs — otherwise, use annotations on Ingress/Route)
# Default upstream below targets production. For non-prod, substitute one of:
# payu-dev, payu-sit, payu-uat, payu-preprod.
curl -s http://kong-admin.payu-api-management.svc:8001/services \
  -d name=payu-gateway \
  -d url=http://gateway-service.payu.svc.cluster.local:8080

# Create a route
curl -s http://kong-admin.payu-api-management.svc:8001/services/payu-gateway/routes \
  -d 'paths[]=/api/v1' \
  -d 'strip_path=false'
```

## DB-less Mode (GitOps-Friendly)

For declarative configuration without a database:

```yaml
# In values.yaml, set:
env:
  database: "off"
  declarative_config: /opt/kong/kong.yaml

# Mount a ConfigMap with the full Kong declarative config
dblessConfig:
  configMap: kong-declarative-config
```

## Partner Onboarding Flow (Kong OSS)

Since Kong OSS does not include a developer portal, partner onboarding is manual or via custom tooling:

1. Admin creates a **Consumer** in Kong for the partner
2. Admin provisions an **API Key** (key-auth plugin) for the consumer
3. Admin assigns a **Rate Limiting** tier via consumer-scoped plugin
4. Kong validates API key, injects partner headers via `request-transformer` plugin
5. PayU gateway-service handles banking-specific auth (HMAC, JWT)

For self-service portal, consider:

- Building a custom portal using the Kong Admin API
- Using an open-source portal like [Backstage](https://backstage.io) (already deployed as Red Hat Developer Hub)

## Security Notes

- All secrets use `<PLACEHOLDER>` values — replace before deployment
- Enable mTLS between Kong proxy and PayU gateway via Service Mesh sidecar
- Kong Admin API must NOT be exposed externally (ClusterIP only)
- Use OpenShift NetworkPolicy to restrict pod-to-pod communication

---

Template created: 2026-03-02 | See ADR-0014 for decision context.
