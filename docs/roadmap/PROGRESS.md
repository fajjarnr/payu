# 📈 PayU Platform — Progress & Engineering Scorecard

> **Dokumen ini adalah historical record & status snapshot PayU Platform.**
> Untuk open bugs dan actionable items → lihat [`TODOS.md`](./TODOS.md)
> Untuk arsitektur gateway & integrasi → lihat [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)

---

## 🏁 Current Status Snapshot

| Attribute | Value |
| :--- | :--- |
| **Last Deployment** | February 23, 2026 (commit `dc46723c`) |
| **OpenShift Tag** | `v1.3.0` |
| **Namespace** | `payu-dev` |
| **Total Pods** | 36/36 running |
| **Services Deployed** | 22/22 |
| **E2E Tests** | 399/399 passing |

> ⚠️ **Note**: Scorecard di bawah mencerminkan posisi deployment dan test coverage,
> **bukan** kualitas logic/business correctness. Code review aktif menemukan 90+ bugs
> di level logic dan frontend-backend mismatch. Lihat `TODOS.md` untuk detail.

---

## 🎯 Platform Maturity Scorecard

| Category | Weight | Infra/Deploy Score | Notes |
| :--- | :--- | :--- | :--- |
| **Backend Services** | 25% | 22/22 deployed | Logic bugs teridentifikasi — lihat TODOS |
| **Shared Libraries** | 10% | 7/7 starters | BUG-BE-090 (RateLimitAspect race condition) |
| **Frontend Web-App** | 15% | Deployed & running | 30+ cross-service mismatches ditemukan |
| **Frontend Mobile** | 5% | Expo setup only | Belum diimplementasikan |
| **Testing** | 15% | 399/399 E2E pass | Unit test coverage varies per service |
| **Security** | 10% | JWT + OIDC active | BUG-BE-001 (gateway JWT placeholder!) |
| **Infrastructure** | 10% | OpenShift HA | HPA + PDB untuk semua critical services |

---

## 📈 DORA Metrics (Current Target)

| Metric | Target | Current | Alignment |
| :--- | :--- | :--- | :--- |
| **Deployment Frequency** | ≥ 1/day | Multiple/day (CI) | 🟢 **Elite** |
| **Lead Time for Changes** | < 4 hours | ~30 mins | 🟢 **Elite** |
| **Mean Time to Recovery** | < 30 mins | ~15 mins | 🟢 **Elite** |
| **Change Failure Rate** | < 10% | ~8% | 🟢 **Elite** |

---

## 🏗️ Architectural Compliance

| Standard | Status | Detail |
| :--- | :--- | :--- |
| **Hexagonal Architecture** | ✅ 19/19 services | All Java/Quarkus services |
| **Event-First** | ✅ Active | `outbox-starter`, `events-starter`, `saga-starter` |
| **ArchUnit Governance** | ✅ 18/19 | 1 service exempt with documented reason |
| **Zero Trust** | ✅ Per-service | JWT + OIDC validation per endpoint |
| **API-First** | ✅ 22/22 | OpenAPI spec per service |
| **Doc-as-Code** | ✅ 13 ADRs | `/docs/adr/` |

---

## 📦 Deployment Log

### v1.3.0 — February 23, 2026

**Infrastructure:**
- ✅ **22/22 Services Running** — All services deployed and healthy on OpenShift
- ✅ **Auth Refresh Fixed** — Resolved 500 errors in refresh token endpoint (delegated to Keycloak)
- ✅ **OIDC Config & JPA Fixed** — Updated `OIDC_ISSUER` across core services, fixed auto-commit DB issues in `wallet-service`
- ✅ **High Availability** — 2 replicas + HPA + PDB for all critical services
- ✅ **HPA** — 12 HorizontalPodAutoscalers (CPU 70% target, min 1-2, max 3-5)
- ✅ **PDB** — 22 PodDisruptionBudgets (minAvailable: 1) for zero-downtime maintenance
- ✅ **4 Failed Services Recovered** — `billing-service`, `investment-service`, `promotion-service`, `statement-service`
- ✅ **Rate Limiting Enhanced** — Best practices: auth 30/min, burst 50
- ✅ **Keycloak User Seeder** — `scripts/keycloak-seeder.sh` with test users (customer1, customer2, admin)
- ✅ **Image Registry** — defaultRoute enabled, all images tagged `1.3.0` pushed
- ✅ **Login Fixed** — Invalid credentials resolved, `payu-backend` client configured

### v1.2.0 — February 20, 2026

**Initial OpenShift Deployment:**
- ✅ **OpenShift Deployed** — 22 services + web-app on OCP 4.20+ (`payu-dev` namespace)
- ✅ **Infrastructure via Operators** — Crunchy PGO, AMQ Streams (KRaft), DataGrid, RHSSO, Vault, cert-manager
- ✅ **Kustomize IaC** — Complete manifests (`operators/` + `infra/` + `overlays/`) for reproducible deployments
- ✅ **TLS** — Let's Encrypt certs via cert-manager DNS01/Route53
- ✅ **Images Built** — All 22 services via Podman, pushed to OCP internal registry (`tag 1.3.0` for web-app)
- ✅ **NetworkPolicies Simplified** — Removed 7 custom policies, kept only Kafka operator policies
- ✅ **Keycloak Realm Imported** — `payu` realm: 4 clients, 5 roles, 4 users, E2E login verified
- ✅ **PostgreSQL Connection Fix** — Workaround for connection exhaustion (scale down/up pattern)
- ✅ **Web-App v1.3.0** — TypeScript errors fixed, `sonner`/`radix-ui` deps added, Transaction types aligned
- 🟢 **Status** — Running: 36/36 pods, 22 services + infra

---

## ✅ Major Completed Tech Debt Items (19/19 Closed)

> Previously tracked as P0-P3 blockers, all resolved prior to Feb 20 deployment.

| # | Item | Resolution |
| :--- | :--- | :--- |
| 1 | Gateway JWT Validation (BUG-BE-001) | ⚠️ **Re-opened** — Found to be placeholder, see TODOS |
| 2 | Auth in-memory state | ⚠️ **Re-opened** — Still in-memory, needs Redis migration |
| 3 | Transaction reference number collision | ⚠️ **Re-opened** — Pattern found across 6 services |
| 4 | Wallet cache invalidation | ⚠️ **Re-opened** — Incomplete invalidation keys |
| 5 | HPA + PDB enabled | ✅ Done — All 22 services |
| 6 | Keycloak realm configured | ✅ Done — `payu` realm live |
| 7 | E2E test suite | ✅ Done — 399/399 passing |
| 8 | TLS certificates | ✅ Done — cert-manager + Let's Encrypt |
| 9 | Image registry | ✅ Done — All images pushed `v1.3.0` |
| 10–19 | Infrastructure (PGO, KRaft, DataGrid, etc.) | ✅ Done — All operators running |

> Items 1-4 were marked complete but code review (Feb 24) found underlying issues still present.
> They have been re-opened and documented in `TODOS.md`.

---

## 🌐 Infrastructure Topology

```
Internet → Route → NGINX Ingress → OpenShift Service → gateway-service (Quarkus)
                                                      → auth-service (Spring Boot)
                                                      → [22 microservices]

Data Layer:
  PostgreSQL (Crunchy PGO): 22 databases (1 per service)
  Redis (DataGrid RESP): cache + session + rate-limit
  Kafka (AMQ Streams KRaft): event streaming
  Keycloak (RHSSO): identity & access management
  Vault: secret management
```

---

## 📊 Test Coverage Summary

| Layer | Framework | Status |
| :--- | :--- | :--- |
| E2E | Playwright | ✅ 399/399 |
| Performance | Gatling | ✅ Configured |
| Contract | Pact | ✅ Configured |
| Integration | Testcontainers | ✅ Per service |
| Architecture | ArchUnit | ✅ 18/19 services |
| Unit | JUnit 5 + Mockito | Varies (see TODOS for coverage gaps) |
