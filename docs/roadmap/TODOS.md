# 📋 PayU — Product Backlog

> **Jira-style backlog.** Hanya berisi item yang BELUM selesai dan perlu tindakan.
> **Aturan Pengembang**: Langsung hapus (delete) task list dari file ini jika sudah selesai dikerjakan (tidak perlu menandainya sebagai `CLOSED`).
> Item yang sudah selesai dipindahkan ke [`CHANGELOG.md`](../../CHANGELOG.md).
>
> 📈 Deployment history & scorecard → [`PROGRESS.md`](./PROGRESS.md)
> 🏦 Arsitektur gateway & gap analysis → [`GATEWAY_ARCH.md`](./GATEWAY_ARCH.md)
> 📖 Navigasi lengkap dokumentasi → [`../INDEX.md`](../INDEX.md)

---

## 📊 Board Summary

| Metric | Value |
|:---|:---|
| **Open P0s** | **0** |
| **Open P1s** | 1 (READY-076: Postgres HA image registry blocked) |
| **Open P2s** | **0** |
| **Production Score** | **payu-dev: 46/46 pods Ready, 0 Not-Ready, 0 CrashLoop, 0 ImagePullBackOff (100% healthy)** |
| **Last Release** | `:1.8.74` — ARCH-006 Phase 3 complete, properties-migrator removed, POM hygiene fixes |

---

## 🐛 Active Tickets

- **READY-076** Postgres HA — image registry blocked (Crunchy `ubi8-2.50.1` etc missing)

---

## 🚀 Framework & Infrastructure Upgrades

| Key | Priority | Category | Summary | Status |
|:---|:---:|:---|:---|:---|
| UPGRADE-012 | P2 | Mobile | Modernize Mobile App: Upgrade to Expo SDK 55 and React Native 0.85 | ⏸️ Skipped |
| UPGRADE-014 | P2 | Frontend | Next.js 16.2.9 Upgrade (Performance & Turbopack default) | ⏳ Planned |

---

## 🔍 Spikes (Research / Architecture Decision)

| Key | Type | Question | Impact | Status |
|:---|:---|:---|:---|:---|
| ARCH-005 | Spike | RHPAM/Kogito/Drools PoC: evaluate rules engine untuk credit scoring & fraud detection | ADR-0015, `rules-starter` shared lib | ❄️ Deferred |

---

## 🔮 Deferred (Icebox)

| Key | Type | Summary | Notes |
|:---|:---|:---|:---|
| LOG-001 | Spike | Evaluate OTLP log export vs current stdout JSON | ❌ Keep current: stdout JSON + LokiStack |
| P2-FE-003 | Story | Mobile App Feature Parity (Expo/RN) | ❄️ Deferred |
| OCP-007 | Story | Service Mesh mTLS enforcement | ❄️ Planned |
| OCP-010 | Story | API versioning headers | ❄️ Planned |
| DR-001 | Story | Disaster Recovery live test execution | ❄️ Scripts ready |
| DEFER-001 | Story | Card Tokenization & 3DS | ❄️ Requires PCI-DSS scope + card network kontrak |
| RHPAM-001 | Story | Create `shared/rules-starter` (Drools 9.x embedded) | ❄️ Depends on ARCH-005 PoC |
| RHPAM-002 | Story | Migrate `lending-service` credit scoring ke DRL rules | ❄️ Depends on RHPAM-001 |
| RHPAM-003 | Story | Payment routing DMN decision tables di `gateway-service` | ❄️ Depends on RHPAM-001 |
| RHPAM-004 | Story | Lending workflow + KYC/AML BPMN orchestration (Kogito) | ❄️ Depends on RHPAM-002 |

---

## ⏸️ Suspended — Operational Follow-Up (OpenShift Destroyed May 2)

| Key | Summary | Notes |
|:---|:---|:---|
| OPS-2026-04-08-01 | Validate wallet-service cache rollout | Cache-starter compatibility fix applied; probe interrupted |
| OPS-2026-04-08-02 | Re-run k6 crud-stress-test.js via k6 Operator | Need `kubectl apply` TestRun CRD |
| OPS-2026-04-08-03 | If stress breaches p(99) < 10s — isolate slow endpoint | k6 Operator runner logs available |
| OPS-2026-04-08-04 | Re-run k6 crud-data-consistency-test.js after stress revalidation | Use TestRun CRD |
| OPS-2026-04-08-05 | Decide GATEWAY_RATE_LIMIT_TEST_MODE on/off after validation | Test mode currently enabled |
| OPS-2026-04-09-01 | Re-run k6 with in-cluster service URLs | k6 Operator lifecycle verified |
| OPS-2026-04-09-07 | Create admin Keycloak user for admin-only endpoints | Smart Routing returns 404 |

---

## 🏗️ DevSecOps Architecture (Suspended — OCP Destroyed)

> Full detail: [`infrastructure/DEVSECOPS_ARCHITECTURE.md`](../../infrastructure/DEVSECOPS_ARCHITECTURE.md) v1.3.1

### Phase 1 — Remaining DR Tasks

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-007 | P1 | Document DR runbook for Vault, ArgoCD, ACS, Wazuh |

### Phase 2 — Hardening (Paused)

| Key | Priority | Summary |
|:---|:---:|:---|
| INFRA-001 | P0 | Fix trivy-image-scan registry auth for OpenShift |
| INFRA-010 | P1 | Configure ComplianceOperator CIS scan |
| INFRA-011 | P1 | Deploy Wazuh manager + agent for SIEM |
| INFRA-013 | P1 | Enable Tekton Chains for SLSA provenance |
| INFRA-014 | P1 | Configure Tekton Results for audit trail |
| INFRA-015 | P1 | Deploy Coraza WAF with OWASP CRS v4.x |
| INFRA-020 | P1 | Define severity P1-P4 + escalation path |
| INFRA-022 | P1 | Setup PagerDuty/Opsgenie for P1/P2 alerting |
| INFRA-018 | P2 | Setup registry GC policy |
| INFRA-019 | P2 | Configure Quay.io auto-prune policy |

---

## 🛡️ DevSecOps Gaps — Untracked

| Key | Priority | § | Summary |
|:---|:---:|:---:|:---|
| DEVSECOPS-001 | P1 | §9.2 | Vault Raft auto-snapshot → S3 bucket terenkripsi |
| DEVSECOPS-002 | P1 | §9.2 | Vault auto-unseal via Transit secret engine / AWS KMS |
| DEVSECOPS-003 | P1 | §14.3 | Global rate limit 1000 req/s per IP |
| DEVSECOPS-004 | P1 | §14.4 | API security headers (HSTS, CSP, X-Frame-Options) |
| DEVSECOPS-005 | P2 | §13.2 | EgressNetworkPolicy + Istio egress gateway |
| DEVSECOPS-006 | P2 | §13.3 | DNS query logging + blok DNS tunneling |
| DEVSECOPS-007 | P2 | §16.2 | LUKS encryption PV + Vault DEK rotation |
| DEVSECOPS-008 | P2 | §16.3 | Wazuh rule data egress ke non-Indonesia |
| DEVSECOPS-009 | P2 | §15 | Schedule quarterly pen test |
| DEVSECOPS-010 | P2 | §9.4 | DNS failover procedure doc |
| DEVSECOPS-011 | P2 | §4.1.4 | Renovate Bot deployment |
| DEVSECOPS-012 | P2 | §10.2 | Monthly cost report workflow |
| DEVSECOPS-013 | P2 | §18.3 | ChatOps Slack/Teams bot commands |
| DEVSECOPS-014 | P3 | §21.2 | Local Pipeline Simulation (`tkn pipeline start --dry-run`) |
| DEVSECOPS-015 | P3 | §21.2 | Security Findings Dashboard Grafana |
| DEVSECOPS-016 | P3 | §21.3 | Service template scaffolder |

---

## 🎯 Production Readiness Gap Analysis

> Overall: ~45% production ready. Target OJK/PCI-DSS: 80%+ on critical paths.

### 🟠 P1 — Critical

| Key | Category | Summary |
|:---|:---|:---|
| READY-010 | Security | Vault integration verified end-to-end |
| READY-011 | Security | Pen-test: mTLS strict, CSP headers, secret scan |
| READY-012 | Security | `@Sensitive` annotation enforced via ArchUnit |
| READY-013 | Cache | `GenericJackson2JsonRedisSerializer` config platform-wide |
| READY-014 | Cache | Cache metrics to Prometheus |
| READY-019 | Observability | Distributed tracing (OTel → Tempo) |
| READY-020 | Observability | Loki log shipping verified |
| READY-021 | Observability | Prometheus scrape config + alerting rules |
| READY-022 | Test coverage | Unit test coverage 80%+ core domain |
| READY-023 | Test coverage | Contract tests (Pact/SCC) for all public APIs |
| READY-026 | HA | Kafka 3-broker cluster |
| READY-028 | HA | AMQ broker pair |
| READY-029 | Performance | Gatling load test: 1000 concurrent users |
| READY-030 | Performance | Stress: SOAK test 24h |
| READY-033 | Test infra | `wallet-service` ThemeResolver / ContractVerifier |

### 🟡 P2 — Important

| Key | Category | Summary |
|:---|:---|:---|
| READY-040 | Compliance | PCI-DSS audit: encryption-at-rest verified |
| READY-041 | Compliance | UU PDP: data retention + right-to-erasure |
| READY-043 | Compliance | Audit trail: append-only + actor + timestamp |
| READY-044 | CI/CD | Tekton Chains (SLSA provenance) |
| READY-045 | CI/CD | Tekton Results (audit trail) |
| READY-046 | CI/CD | ArgoCD sync verified (GitOps) |
| READY-047 | Security | Coraza WAF with OWASP CRS v4.x |
| READY-048 | Security | ComplianceOperator CIS scan |
| READY-049 | Security | Wazuh SIEM (manager + agent) |
| READY-050 | Ops | PagerDuty/Opsgenie for P1/P2 alerting |
| READY-051 | Ops | Severity P1-P4 + escalation path documented |
| READY-052 | Docs | DR runbook tested |

### 🟢 P3 — Nice to have

| Key | Category | Summary |
|:---|:---|:---|
| READY-060 | Card | Card tokenization + 3DS |
| READY-061 | Mobile | Expo SDK 55 + RN 0.85 upgrade |
| READY-062 | ML | ONNX fraud detection model in `fraud-service` |
| READY-063 | Frontend | Premium Emerald design system pass |

### 🎯 Sprint Plan — Open Items

**Sprint 1 (Security Foundation)**:
- [ ] **GAP-10**: Vault E2E audit + auto-unseal + auto-snapshot (READY-010, DEVSECOPS-001/002)

**Sprint 2 (Observability)**:
- [ ] **GAP-2**: OpenTelemetry → Tempo distributed tracing (READY-019)
- [ ] **GAP-3**: Prometheus alerting rules + Loki E2E (READY-020/021)

**Sprint 3 (Compliance + Testing)**:
- [ ] **GAP-14**: UU PDP — data retention + right-to-erasure endpoints (P2-READY-041, DEVSECOPS-007/008)
- [ ] **GAP-4**: Contract tests for core services (READY-023)
- [ ] **GAP-5**: Load test baseline 1K concurrent (READY-029)
- [ ] **GAP-17**: Core domain test coverage 80% (READY-022)

**Sprint 4 (Security Hardening + Ops)**:
- [ ] **GAP-6**: WAF deployment — Coraza + OWASP CRS v4 (INFRA-015)
- [ ] **GAP-7**: SIEM deployment — Wazuh (INFRA-011)
- [ ] **GAP-11**: CI/CD security — Tekton Chains + Results + ArgoCD (READY-044/045/046)
- [ ] **GAP-12**: Incident response — severity P1-P4 + PagerDuty (INFRA-020/022)
- [ ] **GAP-13**: DR runbook + live test (INFRA-007, DR-001)

---

_Last Updated: July 2, 2026 — ARCH-006 Phase 3 complete. Cleaned up all CLOSED/DONE items per backlog rules._
_Partners: TokoBapak, Nobar, Dolan, Sinau, Maca_
