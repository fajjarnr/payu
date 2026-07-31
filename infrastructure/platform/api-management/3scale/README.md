# Red Hat 3scale API Management — PayU Development Deployment

> **Status**: Deployable development configuration using external PostgreSQL
> and Redis, mandatory since 3scale 2.16. Production requires HA backing stores
> and Vault-managed secrets.

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
- External PostgreSQL and Redis
- ODF CephFS StorageClass `ocs-storagecluster-cephfs`
- Wildcard DNS configured for developer portal and APIcast routes
- Shared IngressController for `apps.fajjjar.my.id`

## Files

| File                  | Description                                          |
| :-------------------- | :--------------------------------------------------- |
| `operator-install.yaml` | 3scale Operator install pinned to `threescale-2.16` |
| `kustomization.yaml` | Deployable 3scale runtime resources |
| `apimanager.yaml`     | Development APIManager using external PostgreSQL/Redis |
| `redis.yaml`          | RHEL 9 Redis 7 development backing store |
| `externalsecrets.yaml` | Vault-backed runtime secrets managed by ESO |
| `secrets-3scale.example.yaml` | Vault key-shape reference; never apply this file |
| `apicast-policy.yaml` | Custom APIcast policy for PayU header forwarding     |

## Installation Steps

### 1. Install 3scale Operator

```bash
oc apply -f operator-install.yaml
oc get csv -n payu-api-management | grep 3scale
```

### 2. Deploy 3scale Platform

Seed these Vault KV-v2 paths before applying the runtime:

- `secret/payu/dev/3scale/system-seed`
- `secret/payu/dev/3scale/system-events-hook`
- `secret/payu/dev/3scale/redis-3scale-credentials`
- `secret/payu/dev/3scale/backend-redis`
- `secret/payu/dev/3scale/system-redis`
- `secret/payu/dev/3scale/system-database`
- `secret/payu/dev/3scale/apicast-payu-env`

The `payu-vault` ClusterSecretStore must report `Ready=True`. Set
`system-seed.MASTER_DOMAIN` to `master` (a prefix, not a fully qualified
domain). System file storage uses CephFS because two System replicas require
shared storage.

```bash
oc get clustersecretstore payu-vault
oc apply -k .
oc wait --for=condition=Ready externalsecret --all \
  -n payu-api-management --timeout=3m
oc wait --for=condition=Available apimanager/payu-apimanager \
  -n payu-api-management --timeout=30m
```

Generated routes use `*.apps.fajjjar.my.id`. The shared IngressController
admits this domain automatically; no Route-specific ingress annotation is
required.

If Redis was unavailable during initial seeding, restore Redis first, then
republish the domain events:

```bash
oc exec -n payu-api-management deployment/system-sidekiq \
  -c system-sidekiq -- bundle exec rake zync:resync:domains
```

### 3. Create Provider Account Secret

Do not commit provider tokens. Create `threescale-provider-account` from Vault
before applying `payu-capabilities.yaml`. Required keys:

- `adminURL`: `https://payu-admin.apps.fajjjar.my.id`
- `token`: scoped 3scale provider access token

### 4. Configure PayU Gateway as Backend

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

- Never commit 3scale passwords or provider access tokens
- Production secrets must come from Vault through External Secrets
- The in-cluster development Vault uses ephemeral dev-mode storage and is not a
  promotion target; SIT and above require HA Vault, durable storage, auto-unseal,
  backup, and environment-isolated paths
- Production must replace development PostgreSQL/Redis with dedicated HA external services
- Production must enforce mTLS between APIcast and the PayU gateway through
  OpenShift Service Mesh; this development overlay uses cluster-internal HTTP
- API keys are managed by 3scale; HMAC signing keys are managed by PayU gateway
- Never expose the 3scale Admin Portal externally in production

---

Template created: 2026-03-02 | See ADR-0014 for decision context.
