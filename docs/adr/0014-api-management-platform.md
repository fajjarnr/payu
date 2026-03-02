# ADR-0014: API Management Platform Selection

**Status**: Proposed  
**Date**: 2026-03-02  
**Deciders**: Platform Engineering, API Architecture

## Context

PayU operates as a payment gateway serving multiple external partners (TokoBapak, Nobar, Dolan, Sinau, Maca). As the partner count grows beyond 5, we need a dedicated API management layer for:

- **Developer portal** with self-service API key provisioning
- **API monetization** and usage metering
- **Advanced rate limiting** with partner-specific plans
- **API lifecycle management** (versioning, deprecation notices)
- **Analytics and SLA monitoring** per partner

The PayU `gateway-service` (Quarkus-based) currently handles routing, rate limiting, JWT authentication, IP whitelisting, and HMAC signing. An API management platform would sit **in front** of this gateway in a 2-tier architecture:

```
┌─────────────────────────────────────────────────────────────┐
│  Partner Apps (TokoBapak, Nobar, Dolan, Sinau, Maca)       │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Tier 1: API Management Platform                            │
│  (3scale / Kong / Gravitee)                                 │
│  ┌─────────────┐ ┌──────────────┐ ┌───────────────────────┐│
│  │ Dev Portal   │ │ Rate Plans   │ │ Analytics & Metering  ││
│  │ (API Keys)   │ │ (Quotas)     │ │ (Usage, SLA)          ││
│  └─────────────┘ └──────────────┘ └───────────────────────┘│
└──────────────────────────┬──────────────────────────────────┘
                           │ Internal (mTLS)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Tier 2: PayU Gateway Service (Quarkus)                     │
│  ┌─────────────┐ ┌──────────────┐ ┌───────────────────────┐│
│  │ SNAP-BI      │ │ HMAC Signing │ │ Idempotency           ││
│  │ Compliance   │ │ & JWT Auth   │ │ & Circuit Breaker     ││
│  └─────────────┘ └──────────────┘ └───────────────────────┘│
└──────────────────────────┬──────────────────────────────────┘
                           │ Internal (mTLS)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  Backend Microservices                                      │
│  (account, transaction, wallet, billing, etc.)              │
└─────────────────────────────────────────────────────────────┘
```

## Decision Drivers

- **OpenShift alignment**: PayU runs on Red Hat OpenShift 4.20+; native operator support reduces ops overhead
- **Partner scale**: Trigger threshold is >=5 active partners
- **Budget constraints**: Lab/startup phase; minimize licensing costs
- **Banking-specific logic**: SNAP-BI compliance, HMAC signing, and idempotency must remain in PayU gateway regardless
- **Developer experience**: Partners need self-service onboarding, sandbox environments, and API documentation

## Considered Options

### Option A: Red Hat 3scale API Management (IMP-019)

Red Hat's native API management platform, deployed via OperatorHub on OpenShift.

**Pros:**
- Native OpenShift integration via Operator (OperatorHub install)
- Red Hat support included in OpenShift subscription
- APIcast gateway (nginx-based) with admin portal and developer portal
- OIDC/Keycloak integration out-of-the-box (aligns with existing Red Hat SSO)
- Built-in rate plans, API contracts, and billing/metering
- Consistent with existing Red Hat technology stack (AMQ Streams, Crunchy PG, SSO)

**Cons:**
- Higher resource footprint (~3 pods minimum: system-app, system-sidekiq, apicast-staging/production)
- Learning curve for APIcast policy chain configuration
- Some features overlap with existing gateway-service (rate limiting, auth)
- Requires MySQL/PostgreSQL + Redis for 3scale system components

### Option B: Kong Gateway (OSS) (IMP-020)

Lightweight, high-performance API gateway built on OpenResty/Lua.

**Pros:**
- Lightweight, high-performance (Lua/OpenResty, sub-millisecond latency overhead)
- Large plugin ecosystem (100+ plugins: auth, rate-limit, logging, transforms)
- Kong Ingress Controller for Kubernetes/OpenShift
- Community edition is free and open-source (Apache 2.0)
- Well-documented, widely adopted (10k+ GitHub stars), large community
- DB-less mode available for declarative configuration (GitOps-friendly)

**Cons:**
- No native OpenShift operator (deploy via Helm chart)
- Developer portal requires Kong Enterprise (paid) or building a custom portal
- No built-in API monetization or usage metering
- Plugin development requires Lua knowledge (or Go for custom plugins)

### Option C: Gravitee.io (IMP-020)

Open-source API management with event-native capabilities.

**Pros:**
- Open-source with developer portal included in community edition
- API designer and documentation tools built-in
- Event-native: supports Kafka, MQTT, and WebSocket APIs (aligns with AMQ Streams)
- Lower resource footprint than 3scale (~2 pods for gateway + management API)
- Policy studio with visual drag-and-drop policy chain editor

**Cons:**
- Smaller community than Kong (~3k GitHub stars)
- No Red Hat support or OpenShift operator
- OpenShift deployment requires manual Helm/manifest configuration
- Enterprise features (audit logging, custom roles) require paid tier

## Decision

**Deferred** — Evaluate when partner count reaches 5+ active integrations. Current recommendation matrix:

| Criterion                  | 3scale (A)  | Kong OSS (B) | Gravitee (C) |
| :------------------------- | :---------- | :------------ | :------------ |
| OpenShift integration      | ★★★★★       | ★★★☆☆         | ★★☆☆☆         |
| Resource efficiency        | ★★☆☆☆       | ★★★★★         | ★★★★☆         |
| Developer portal (built-in)| ★★★★★       | ★☆☆☆☆         | ★★★★☆         |
| API monetization           | ★★★★★       | ★☆☆☆☆         | ★★★☆☆         |
| Event-native (Kafka)       | ★★☆☆☆       | ★★☆☆☆         | ★★★★★         |
| Community & ecosystem      | ★★★☆☆       | ★★★★★         | ★★★☆☆         |
| Cost (OSS)                 | ★★★☆☆       | ★★★★★         | ★★★★☆         |

**Guidance:**
- **Red Hat ecosystem alignment** (recommended default): **3scale** (Option A) — operational consistency, single-vendor support
- **Budget-constrained / performance-critical**: **Kong OSS** (Option B) with custom developer portal
- **Event-driven APIs primary**: **Gravitee.io** (Option C) — native Kafka/MQTT protocol support

## Rationale

The decision is deferred because:
1. Current partner count (3-4) does not yet justify the operational overhead
2. PayU gateway-service adequately handles current traffic patterns
3. Premature adoption would increase infrastructure complexity without proportional benefit

Template configurations for 3scale and Kong are prepared in `infrastructure/3scale/` and `infrastructure/kong/` to enable rapid deployment when the trigger threshold is met.

## Consequences

**Positive:**
- Clear evaluation criteria documented for future decision
- Deployment templates ready for rapid adoption when needed
- Banking-specific logic (SNAP-BI, HMAC, idempotency) remains decoupled in PayU gateway regardless of choice
- 2-tier architecture ensures API management concerns are separated from banking protocol concerns

**Negative:**
- Maintaining deployment templates for multiple options adds minor overhead
- Team needs to build expertise in chosen platform when trigger is reached
- Potential feature overlap between API management layer and existing gateway-service rate limiting

## Implementation Notes

1. **Trigger**: Activate when >=5 partners are live and onboarding friction becomes measurable
2. **PoC scope**: Deploy chosen platform in staging with 1 partner (TokoBapak) for 2-week evaluation
3. **Migration path**: API management handles partner-facing concerns; PayU gateway-service retains all SNAP-BI compliance, HMAC signing, and idempotency enforcement
4. **Templates**: See `infrastructure/3scale/` and `infrastructure/kong/` for deployment configurations

---
*Created: 2026-03-02 | Relates to: IMP-019, IMP-020*
