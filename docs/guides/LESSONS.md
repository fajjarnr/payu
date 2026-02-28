# 🧠 PayU Lessons Learned & Implementation Patterns

This document serves as a high-level index for the "Lessons Learned" and historical implementation patterns discovered during the PayU platform development.

To ensure maintenance and specialized access, all detailed patterns have been migrated to the **AI Agent Skill Ecosystem** in `.agent/skills/`.

## 📂 Pattern Directory

| Domain                    | Reference Document                                                                                        | Primary Skill             |
| :------------------------ | :-------------------------------------------------------------------------------------------------------- | :------------------------ |
| **Infrastructure & Ops**  | [INFRASTRUCTURE_PATTERNS.md](../../.agent/skills/platform-engineer/references/INFRASTRUCTURE_PATTERNS.md) | `platform-engineer`       |
| **Deployment & Release**  | [DEPLOYMENT_PATTERNS.md](../../.agent/skills/platform-engineer/references/DEPLOYMENT_PATTERNS.md)         | `platform-engineer`       |
| **Backend & JPA**         | [BACKEND_PATTERNS.md](../../.agent/skills/core-banking-engineer/references/BACKEND_PATTERNS.md)           | `core-banking-engineer`   |
| **Security & IAM**        | [SECURITY_PATTERNS.md](../../.agent/skills/cybersecurity-architect/references/SECURITY_PATTERNS.md)       | `cybersecurity-architect` |
| **API Standards**         | [API_STANDARDS.md](../../.agent/skills/api-architect/references/API_STANDARDS.md)                         | `api-architect`           |
| **Integration & Events**  | [EVENT_DRIVEN_PATTERNS.md](../../.agent/skills/integration-architect/references/EVENT_DRIVEN_PATTERNS.md) | `integration-architect`   |
| **Frontend Architecture** | [FRONTEND_PATTERNS.md](../../.agent/skills/frontend-architect/references/FRONTEND_PATTERNS.md)            | `frontend-architect`      |
| **Design System**         | [DESIGN_SYSTEM_PATTERNS.md](../../.agent/skills/product-designer/references/DESIGN_SYSTEM_PATTERNS.md)    | `product-designer`        |
| **Testing & Quality**     | [TESTING_PATTERNS.md](../../.agent/skills/quality-engineer/references/TESTING_PATTERNS.md)                | `quality-engineer`        |

## 🧩 Lessons Learned (Session Log)

### L-001: Python ML/AI Services — Stay on Debian Slim, Not UBI9

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Platform

UBI9 `python-312` has known compatibility issues with native ML/AI dependencies:

- PaddleOCR, OpenCV, PyTorch — prebuilt wheels expect Debian/glibc paths
- Missing shared libraries (`libGL`, `libglib`, `libgomp`) require different package names on RHEL
- `site-packages` path differs (`/opt/app-root/lib/` vs `/usr/local/lib/`)

**Decision**: Keep `python:3.12-slim` for `kyc-service` and `analytics-service`. All Java (UBI9 OpenJDK 21) and Node.js (UBI9 Node 20) services use UBI9.

**Rule**: Do not migrate Python ML services to UBI9 without full dependency compatibility testing first.

---

### L-002: Domain Routing Strategy — Gateway API + Istio Ingress (Updated)

**Date**: February 26, 2026 (Updated) | **Severity**: Critical | **Domain**: Infrastructure

**Dual-ingress architecture** separating application traffic from platform traffic:

| Traffic Type      | Ingress Controller               | Domain Pattern                                         | Example                                                        |
| :---------------- | :------------------------------- | :----------------------------------------------------- | :------------------------------------------------------------- |
| **App (Prod)**    | Istio Ingress Gateway            | `payu.fajjjar.my.id` + `*.payu.fajjjar.my.id`          | `api.payu.fajjjar.my.id`, `sso.payu.fajjjar.my.id`             |
| **App (Dev)**     | Istio Ingress Gateway            | `*.dev.payu.fajjjar.my.id`                             | `api.dev.payu.fajjjar.my.id`, `gateway.dev.payu.fajjjar.my.id` |
| **App (Staging)** | Istio Ingress Gateway            | `*.staging.payu.fajjjar.my.id`                         | `api.staging.payu.fajjjar.my.id`                               |
| **App (SIT/UAT)** | Istio Ingress Gateway            | `*.sit.payu.fajjjar.my.id`, `*.uat.payu.fajjjar.my.id` | `api.sit.payu.fajjjar.my.id`                                   |
| **OCP Platform**  | OCP Ingress Controller (HAProxy) | `*.apps.payu.ocp.fajjjar.my.id`                        | `console-openshift-console.apps.payu.ocp.fajjjar.my.id`        |
| **OCP API**       | Kubernetes API                   | `api.payu.ocp.fajjjar.my.id`                           | —                                                              |

**Rule**: ALL environments use Istio Ingress Gateway for application traffic. `*.apps.payu.ocp.*` is exclusively for OCP platform components (console, image registry, ArgoCD, Grafana).

**Gotcha**: Beware of `apps.cluster.payu` vs `apps.payu.ocp` inconsistency — standardize early. Always replace most-specific patterns first during migration.

---

### L-003: Domain Migration — Scope & Safe Replacement

**Date**: February 26, 2026 | **Severity**: High | **Domain**: DevOps

When doing bulk domain replacement across a monorepo (156 files, ~400 matches):

1. **Order matters**: Replace most-specific patterns first (`staging-api.payu.id` before `payu.id`)
2. **Preserve intentionally different domains**: `payu.local` (mesh trust), `payu.internal` (internal DNS), `payu.test` (test data), Java packages (`id.payu.*`)
3. **Java code is mostly unaffected**: Domain references in Java are OpenAPI metadata and CORS — both overridden by OpenShift configmaps at deploy time
4. **Always verify with negative grep**: After replacement, confirm zero stray references remain

**Regex used**: `sed 's/payu\.id/payu.fajjjar.my.id/g'` — safe because `id.payu` (Java packages) doesn't match `payu.id`

---

### L-004: Container Image Pinning

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Platform

Never use `:latest` in compose files or Quadlet containers. Pin to specific versions:

| Image       | Before         | After          |
| :---------- | :------------- | :------------- |
| Keycloak    | `:latest`      | `:26.1`        |
| kafka-ui    | `:latest`      | `:v0.7.2`      |
| timescaledb | `:latest-pg16` | `:2.17.2-pg16` |
| rustfs      | `:latest`      | `:0.3.0`       |

**Rule**: Every image reference must have an explicit version tag for reproducibility.

---

### L-005: Backstage catalog-info.yaml — Single Root File

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Developer Hub

For a monorepo with 22+ services, use a single root `catalog-info.yaml` with YAML multi-document (`---`) separators rather than per-service files. Benefits:

- Single import point in Backstage/RHDH
- Easier to maintain dependency graph (`dependsOn`, `providesApis`)
- System-level view of all components in one place

Include: Components (services, libraries, websites), Resources (databases, message brokers, caches), and System definition.

---

### L-006: OSS Version Compatibility Matrix

**Date**: February 26, 2026 | **Severity**: Medium | **Domain**: Architecture

Maintain a compatibility matrix between Red Hat products and OSS equivalents. Key validated mappings:

| Red Hat Product  | OSS Equivalent | PayU Version | Compatible |
| :--------------- | :------------- | :----------- | :--------- |
| Red Hat Runtimes | Spring Boot    | 3.4.1        | ✅         |
| RHBQ             | Quarkus        | 3.17.5       | ✅         |
| Crunchy PGO      | PostgreSQL     | 16           | ✅         |
| AMQ Streams      | Apache Kafka   | 3.5 (CP 7.5) | ✅         |
| RHBK             | Keycloak       | 26.1         | ✅         |
| Data Grid (RESP) | Redis          | 7.x          | ✅         |
| RHDH             | Backstage.io   | 1.25+        | ✅         |

**Rule**: Verify wire compatibility when client/broker versions differ (e.g., Kafka client 3.8 ↔ broker 3.5 is safe).

---

### L-007: Istio Ingress Gateway — Router Node Placement & Dual LoadBalancer VIP

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Infrastructure / Service Mesh

When running both OCP Ingress Controller and Istio Ingress Gateway on the same cluster with dedicated router nodes:

**Architecture**:

- 3 router nodes with taint `node-role.kubernetes.io/router:NoSchedule`
- OCP Ingress Controller pods (HAProxy) → already scheduled on router nodes by OpenShift
- Istio Ingress Gateway pods → must explicitly opt-in with `nodeSelector` + `tolerations`

**Configuration**:

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
| Component | Ports | DNS Target |
| :--------------------------- | :-------- | :------------------------------ |
| OCP Ingress Controller (HAProxy) | 80, 443 | `*.apps.payu.ocp.fajjjar.my.id` |
| Istio Ingress Gateway | 8080, 8443 | `*.payu.fajjjar.my.id` + env wildcards |

**Rule**: Use separate LB VIPs with different ports (80/443 vs 8080/8443). Both can coexist on the same router nodes because they bind different ports. Set `replicas: 3` and HPA `minReplicas: 3` (one per router node).

---

### L-008: Code Health Anti-Patterns in Multi-Pod Microservices

**Date**: February 26, 2026 | **Severity**: High | **Domain**: Backend / Architecture

Three critical anti-patterns discovered during E-20 Code Health & Tech Hygiene epic:

**1. In-Memory State (ConcurrentHashMap) in Stateless Services**
`WalletServiceAdapter` used a `ConcurrentHashMap<String, ReservationInfo>` to store reservation data between `reserveBalance()` and `commitBalance()` calls. This fails catastrophically in multi-pod deployments because the commit call may hit a different pod than the reserve call. **Fix**: Pass `reservationId` through method signatures; the saga context (persisted in DB as JSONB) already had this field.

**2. Spring Boot Config Namespace Gotcha**
`transaction-service/application.yml` had a top-level `kafka:` block. Spring Boot silently ignores this — the correct path is `spring.kafka.*`. No error, no warning, just silent misconfiguration. **Rule**: Always verify config properties are under the correct Spring namespace. Use `@ConfigurationProperties` binding validation.

**3. `.gitignore` Pattern Matching `port/out/` Directories**
A root `.gitignore` entry `out/` matched any path containing `out/`, including valid hexagonal architecture paths like `domain/port/out/AccountServicePort.java`. Required `git add -f` to force-add. **Rule**: Use more specific patterns like `/out/` (root only) instead of `out/` (recursive match).

**Bonus**: `spring.jpa.open-in-view` defaults to `true` in Spring Boot, which keeps DB sessions open during HTTP response rendering — an anti-pattern that causes lazy-loading surprises and connection pool exhaustion. Always set `spring.jpa.open-in-view: false` explicitly.

---

## 🚀 How to use these patterns

1. **AI Agents**: Should read the `SKILL.md` of their respective domain. The reference documents are explicitly linked in the "Reference Implementation Patterns" section of the skill.
2. **Human Developers**: Can access the patterns directly via the links above or by navigating the `.agent/skills/` directory.
3. **Session Lessons** (L-001+): Captured from live development sessions. Review before starting related work.

---

### L-009: Payment Gateway Implementation — Webhook Delivery Patterns

**Date**: February 28, 2026 | **Severity**: High | **Domain**: Backend / Gateway

Lessons from implementing E-15 Payment Gateway Features (7 stories, 25 SP):

**1. VA Simulator Architecture**
- External bank simulators should be **deterministic** — same VA number + amount = same response
- Use **fixed prefixes per bank** (BCA: 12345, BNI: 67890) untuk memudahkan testing
- Quarkus Native ideal untuk simulators: sub-second startup, low memory footprint

**2. Payment Link Webhook Reliability**
- **HMAC-SHA256 signing** wajib untuk webhook payload integrity
- Implement **exponential backoff retry** (3x) dengan jitter untuk failed deliveries
- Store webhook delivery attempts di DB untuk audit trail

**3. Scheduler-Based Expiry Pattern**
- Gunakan **single centralized scheduler** (`PaymentExpiryScheduler`) daripada multiple schedulers per payment type
- Implement **optimistic locking** pada status updates untuk prevent race conditions
- Release reserved balance **sebelum** publish Kafka event untuk maintain consistency

**4. Mobile Deeplink Security**
- **Signed URLs dengan expiry** — jangan trust client-side params
- Support **universal links** (iOS) dan **app links** (Android) sebagai fallback
- Expo Linking + React Native Hooks pattern untuk clean separation

---

### L-010: Settlement & Revenue Share — Financial Engine Patterns

**Date**: February 28, 2026 | **Severity**: High | **Domain**: Backend / FinOps

Lessons from implementing E-12 Settlement & Financial Operations:

**1. Rate Card Engine Design**
- Support **3 fee types**: FLAT (fixed amount), PERCENTAGE (of transaction), TIERED (volume-based brackets)
- **Min/max caps** essential untuk percentage fees (prevent Rp100 juta fee on Rp1M transaction)
- Link: Partner → Rate Card (1:1 untuk simplicity, 1:N jika complex pricing tiers)

**2. Settlement State Machine**
- PENDING → PROCESSING → COMPLETED/FAILED/OVERRIDDEN
- **Never delete** settlement batches — soft delete dengan status untuk audit
- Manual override capability dengan **dual-authorization** untuk amount > threshold

**3. Revenue Split Calculation**
- **Priority-based stakeholder ordering** — primary stakeholder dapat payout pertama
- Handle **remaining amount** (rounding errors) — assign ke platform atau distribute proportional
- **Monthly royalty statements** auto-generated dengan breakdown per transaction

**4. Multi-Currency Settlement**
- **FX rate locking window** (15 menit) — prevent rate fluctuation during settlement processing
- Partner currency preference per settlement batch
- Auto-conversion hanya pada settlement time, bukan saat transaction

---

_Last Updated: February 28, 2026_
