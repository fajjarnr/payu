# Red Hat 3scale API Management — PayU Deployment Template

> **Status**: Template only — deploy when >=5 partners are active (see ADR-0014)

## Overview

This directory contains OpenShift deployment templates for Red Hat 3scale API Management
as the Tier 1 API management layer in front of the PayU gateway-service.

### Architecture (2-Tier)

```
Partner Apps
    │
    ▼
┌──────────────────────────────────┐
│  3scale APIcast (Tier 1)         │
│  - Developer Portal              │
│  - Rate Plans & Quotas           │
│  - API Key Provisioning          │
│  - Usage Metering & Analytics    │
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

- OpenShift 4.20+ cluster with admin access
- Red Hat 3scale Operator installed from OperatorHub
- PostgreSQL (for 3scale system database, can reuse Crunchy Postgres Operator)
- Redis (for 3scale backend storage, can reuse Red Hat Data Grid in RESP mode)
- Wildcard DNS configured for developer portal and APIcast routes

## Files

| File                  | Description                                          |
| :-------------------- | :--------------------------------------------------- |
| `operator-install.yaml` | 3scale Operator install pinned to `threescale-2.16` |
| `apimanager.yaml`     | 3scale APIManager Custom Resource (Operator-managed) |
| `secrets-3scale.example.yaml` | Example secret shape only; copy to a private manifest or create secrets via CLI/Vault |
| `apicast-policy.yaml` | Custom APIcast policy for PayU header forwarding     |

## Installation Steps

### 1. Install 3scale Operator

```bash
oc apply -f operator-install.yaml
oc get csv -n payu-api-management | grep 3scale
```

### 2. Create Namespace and Secrets

```bash
# Create namespace
oc new-project payu-api-management

# Create required secrets (replace placeholders with actual values)
oc create secret generic system-seed \
  --from-literal=MASTER_DOMAIN=master.payu-api.example.com \
  --from-literal=MASTER_USER=admin \
  --from-literal=MASTER_PASSWORD=<MASTER_PASSWORD> \
  --from-literal=MASTER_ACCESS_TOKEN=<MASTER_ACCESS_TOKEN> \
  --from-literal=ADMIN_ACCESS_TOKEN=<ADMIN_ACCESS_TOKEN> \
  --from-literal=TENANT_NAME=payu \
  -n payu-api-management

oc create secret generic system-database \
  --from-literal=URL=postgresql://threescale:<DB_PASSWORD>@crunchy-primary.payu-db.svc:5432/threescale \
  -n payu-api-management

oc create secret generic backend-redis \
  --from-literal=REDIS_STORAGE_URL=redis://:<REDIS_PASSWORD>@payu-cache-resp.payu-dev.svc.cluster.local:11222/0 \
  --from-literal=REDIS_QUEUES_URL=redis://:<REDIS_PASSWORD>@payu-cache-resp.payu-dev.svc.cluster.local:11222/1 \
  -n payu-api-management

oc create secret generic system-redis \
  --from-literal=URL=redis://:<REDIS_PASSWORD>@payu-cache-resp.payu-dev.svc.cluster.local:11222/2 \
  -n payu-api-management
```

### 3. Deploy 3scale Platform

Apply only after replacing all secret placeholders and verifying target DB/cache/gateway services exist:

```bash
oc apply -f system-storage-pvc.yaml
oc apply -f apicast-policy.yaml
oc apply -f 3scale-network-policy.yaml
oc apply -f apimanager.yaml
```

### 5. Configure PayU Gateway as Backend

In the 3scale Admin Portal:

1. Create a **Backend** pointing to `http://gateway-service.payu.svc.cluster.local:8080` for prod, or switch the namespace to `payu-dev`, `payu-sit`, `payu-uat`, or `payu-preprod` for non-prod.
2. Create a **Product** (e.g., "PayU Payment API")
3. Configure **Application Plans** per partner tier (Basic, Premium, Enterprise)
4. Set rate limits per plan
5. Enable the `payu-header-forwarding` custom policy in the policy chain

## Partner Onboarding Flow

1. Partner signs up on the 3scale Developer Portal
2. Admin approves the partner application
3. Partner receives API key + secret
4. 3scale validates API key on each request
5. 3scale injects `X-PayU-Partner-Id` and `X-PayU-Plan-Id` headers
6. PayU gateway-service handles banking-specific auth (HMAC, JWT)

## Security Notes

- All secrets use `<PLACEHOLDER>` values — replace before deployment
- mTLS between 3scale APIcast and PayU gateway is enforced via OpenShift Service Mesh
- API keys are managed by 3scale; HMAC signing keys are managed by PayU gateway
- Never expose the 3scale Admin Portal externally in production

---

Template created: 2026-03-02 | See ADR-0014 for decision context.
