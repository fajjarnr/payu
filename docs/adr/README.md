# Architecture Decision Records (ADR)

This directory contains the historical record of architectural decisions made for the **PayU Digital Banking Platform**. Our goal is to preserve the "Why" behind our technical choices.

## 📋 ADR Index

| ID                                              | Title                                    | Status   | Date       |
| :---------------------------------------------- | :--------------------------------------- | :------- | :--------- |
| [0000](0000-adr-guidelines.md)                  | ADR Guidelines                           | Accepted | 2026-01-28 |
| [0001](0001-template.md)                        | ADR Template                             | Accepted | 2026-01-28 |
| [0002](0002-spring-boot-for-core-banking.md)    | Spring Boot for Core Banking             | Accepted | 2026-01-30 |
| [0003](0003-quarkus-for-supporting-services.md) | Quarkus for Supporting Services          | Accepted | 2026-01-30 |
| [0004](0004-hexagonal-architecture.md)          | Hexagonal Architecture                   | Accepted | 2026-01-30 |
| [0005](0005-kafka-event-streaming.md)           | Kafka Event Streaming                    | Accepted | 2026-01-30 |
| [0006](0006-postgresql-primary-database.md)     | PostgreSQL Primary Database              | Accepted | 2026-01-30 |
| [0007](0007-database-per-service.md)            | Database per Service                     | Accepted | 2026-01-30 |
| [0008](0008-resilience-patterns.md)             | Resilience Patterns                      | Accepted | 2026-01-30 |
| [0009](0009-caching-strategy.md)                | Caching Strategy                         | Accepted | 2026-01-30 |
| [0010](0010-security-standards.md)              | Security Standards                       | Accepted | 2026-01-30 |
| [0011](0011-frontend-architecture.md)           | Frontend Architecture                    | Accepted | 2026-01-30 |
| [0012](0012-container-standardization.md)       | Container Standardization                | Accepted | 2026-01-30 |
| [0013](0013-testing-strategy.md)                | Testing Strategy                         | Accepted | 2026-01-30 |
| [0014](0014-api-management-platform.md)         | API Management Platform Selection        | Proposed | 2026-03-02 |
| [0015](0015-process-automation-rhpam.md)        | Process Automation (RHPAM/Kogito/Drools) | Accepted | 2026-03-11 |
| [0016](0016-arch-006-phase-a-strategy.md)       | ARCH-006 Phase A: Spring Boot 4.1.0 Platform Migration | Deferred | 2026-06-14 |
| [0017](0017-infinispan-hotrod-migration.md)     | Native Hot Rod Migration with REST Interoperability    | Accepted | 2026-07-17 |
| [0018](0018-kyc-hybrid-model.md)                | KYC — Hybrid Model (PayU as KYC Service)               | Accepted | 2026-05-07 |
| [0019](0019-statement-dual-format.md)           | Statement Format — Dual Output (PDF + JSON/CSV)        | Accepted | 2026-05-07 |
| [0020](0020-support-centralized.md)             | Support — PayU Handles All (Single-Tenant)             | Accepted | 2026-05-07 |
| [0021](0021-cms-single-tenant.md)               | CMS — Single-Tenant                                    | Accepted | 2026-05-07 |
| [0022](0022-money-idempotency-standard.md)       | Money & Idempotency Standard (Financial Integrity)     | Accepted | 2026-08-11 |
| [0023](0023-mvp-scope.md)                        | MVP Scope Definition (Core Banking)                    | Accepted | 2026-08-11 |
| [0024](0024-chaos-engineering-and-fault-injection-strategy.md) | Tiered Chaos Engineering & Fault Injection Strategy | Accepted | 2026-08-18 |
| [0025](0025-snap-bi-and-partner-gateway-security-standard.md)  | SNAP-BI & Partner Gateway Security Standards         | Accepted | 2026-08-18 |
| [0026](0026-kafka-topic-governance-and-dlq-strategy.md)        | Kafka Topic Governance & Dead Letter Queue Strategy  | Accepted | 2026-08-18 |
| [0027](0027-notification-service-architecture-and-multi-channel-delivery.md) | Notification Service Architecture & Multi-Channel Delivery | Accepted | 2026-08-18 |
| [0028](0028-step-up-authentication-and-dynamic-linking-standard.md) | Step-Up Authentication, Dynamic Linking & Transaction PIN Security Standard | Accepted | 2026-08-18 |
| [0029](0029-iso20022-interbank-clearing-and-suspense-ledgering.md) | ISO 20022 Interbank Clearing, Suspense Account Ledgering & Central Bank Settlement Standard | Accepted | 2026-08-18 |
| [0030](0030-realtime-transaction-velocity-and-aml-risk-scoring.md) | Real-Time Transaction Velocity Counter, Fraud Risk Pre-Check & AML Decision Pipeline | Accepted | 2026-08-18 |
| [0031](0031-database-resilience-pitr-and-disaster-recovery.md) | Database High-Availability, Continuous Point-In-Time Recovery (PITR) & Disaster Recovery Standard | Accepted | 2026-08-18 |
| [0032](0032-perimeter-security-waf-coraza-and-siem-wazuh.md) | Perimeter Security: Tiered WAF (AWS WAF & Coraza OWASP CRS) and Centralized SIEM (Wazuh & CLF Syslog) | Accepted | 2026-08-18 |
| [0033](0033-database-row-level-security-and-multi-tenant-isolation-standard.md) | Database Row-Level Security (PostgreSQL RLS) & Multi-Tenant Isolation Standard (PARTNER-PROD-006) | Accepted | 2026-08-18 |
| [0034](0034-end-to-end-observability-slo-sli-and-distributed-tracing-standard.md) | End-to-End Observability, Distributed Tracing (W3C/OTel/Tempo), and Multi-Window Multi-Burn-Rate SLI/SLO Standard (PARTNER-PROD-009) | Accepted | 2026-08-18 |
| [0035](0035-dual-control-partner-onboarding-and-sla-runbook.md) | Dual-Control (Maker-Checker) Partner Onboarding, SLA & Runbook (PARTNER-PROD-011) | Accepted | 2026-08-19 |
| [0036](0036-python-fastapi-microservice-architecture-for-ai-ml-kyc-analytics.md) | Python FastAPI Microservice Architecture for AI/ML, KYC & Analytics (QAMVP-004/ARCH-GLOBAL-004/READY-062) | Accepted | 2026-08-19 |
| [0037](0037-internal-synchronous-inter-service-communication-via-grpc-and-protobuf-governance.md) | Internal Synchronous Inter-Service Communication via gRPC & Protobuf Governance (grpc-starter, ARCH-BESTP-002) | Accepted | 2026-08-19 |
| [0038](0038-distributed-transaction-management-orchestrated-saga-pattern-with-persistent-state-machine.md) | Distributed Transaction Management: Orchestrated Saga Pattern with Persistent State Machine (saga-starter) | Accepted | 2026-08-19 |
| [0039](0039-nextjs-app-router-bff-security-token-relay-and-session-management-standard.md) | Next.js App Router BFF Security, Token Relay & Session Management Standard | Accepted | 2026-08-19 |
| [0040](0040-field-level-encryption-searchable-encryption-via-hmac-blind-indexing-and-key-lifecycle.md) | Field-Level Encryption, Searchable Encryption via HMAC Blind Indexing & Key Lifecycle (PARTNER-PROD-002) | Accepted | 2026-08-19 |
| [0041](0041-transactional-outbox-pattern-with-polling-skip-locked-dispatcher-vs-debezium-cdc.md) | Transactional Outbox Pattern with Polling SKIP LOCKED Dispatcher vs Debezium CDC | Accepted | 2026-08-19 |
| [0042](0042-distributed-job-scheduling-and-cluster-wide-concurrency-lock-standard-using-shedlock.md) | Distributed Job Scheduling & Cluster-Wide Concurrency Lock Standard using ShedLock (GW-CONCUR-001) | Accepted | 2026-08-19 |
| [0043](0043-enterprise-integration-patterns-and-core-banking-protocol-bridging-with-apache-camel.md) | Enterprise Integration Patterns & Core Banking Protocol Bridging with Apache Camel | Accepted | 2026-08-19 |
| [0044](0044-secrets-lifecycle-and-zero-trust-secrets-management-with-vault-and-eso.md) | Secrets Lifecycle & Zero-Trust Secrets Management with Vault & ESO (DEVSECOPS-017) | Accepted | 2026-08-19 |
| [0045](0045-gitops-continuous-delivery-infrastructure-as-code-and-supply-chain-security.md) | GitOps Continuous Delivery, Infrastructure as Code & Supply Chain Security | Accepted | 2026-08-19 |
| [0046](0046-time-series-financial-telemetry-via-timescaledb-hypertables.md) | Time-Series Financial Telemetry via TimescaleDB Hypertables | Accepted | 2026-08-19 |
| [0047](0047-frontend-nominal-branded-types-and-strict-financial-money-precision-standard.md) | Frontend Nominal Branded Types & Strict Financial Money Precision Standard (DX-TS-BRANDED-001) | Accepted | 2026-08-19 |
| [0048](0048-lending-eligibility-and-pricing-via-dmn-decision-tables.md) | Lending Eligibility and Pricing via DMN Decision Tables (ARCH-BESTP-003) | Accepted | 2026-08-20 |
| [0049](0049-wallet-immutable-ledger-and-double-entry-standard.md) | Wallet Immutable Ledger and Double-Entry Standard (WALLET-001) | Accepted | 2026-08-20 |
| [0050](0050-fx-provider-and-rate-governance-standard.md) | FX Provider and Rate Governance Standard (PROD-002) | Accepted | 2026-08-20 |
| [0051](0051-support-ticket-and-faq-lifecycle-standard.md) | Support Ticket and FAQ Lifecycle Standard (BE-SUPP-001) | Accepted | 2026-08-20 |
| [0052](0052-qris-and-virtual-account-integration-standard.md) | QRIS and Virtual Account Integration Standard (FE-STUB-003) | Accepted | 2026-08-20 |
| [0053](0053-investment-and-gold-portfolio-standard.md) | Investment and Gold Portfolio Standard | Accepted | 2026-08-20 |
| [0054](0054-dispute-and-chargeback-standard.md) | Dispute and Chargeback Standard | Accepted | 2026-08-20 |
| [0055](0055-promotion-cashback-and-reward-saga-standard.md) | Promotion, Cashback and Reward Saga Standard | Accepted | 2026-08-20 |
| [0056](0056-simulator-fidelity-and-contract-testing-standard.md) | Simulator Fidelity and Contract Testing Standard (SIM-001) | Accepted | 2026-08-20 |
| [0057](0057-billing-provider-and-biller-catalogue-governance-standard.md) | Billing Provider & Biller Catalogue Governance (BILLING) | Accepted | 2026-08-22 |
| [0058](0058-backoffice-rbac-and-admin-audit-trail-standard.md) | Backoffice RBAC & Admin Audit Trail (BACKOFFICE) | Accepted | 2026-08-22 |
| [0059](0059-product-catalog-and-partner-product-governance-standard.md) | Product Catalog & Partner Product Governance (CATALOG) | Accepted | 2026-08-22 |
| [0060](0060-transaction-orchestration-idempotency-reconciliation-and-callback-hardening-standard.md) | Transaction Orchestration — Idempotency, Reconciliation & Callback Hardening (TXN-HARDEN Q1-Q6, PADG 14/2025) | Accepted | 2026-08-22 |
| [0061](0061-account-service-lifecycle-multi-tenancy-and-pii-protection-standard.md) | Account Service — Lifecycle, Multi-Tenancy & PII Protection (ACC-HARDEN, Crassula/AWS CLM, UU PDP) | Accepted | 2026-08-22 |
| [0062](0062-auth-service-oauth2-dpop-refresh-rotation-and-device-binding-standard.md) | Auth Service — OAuth2 DPoP, Refresh Rotation & Device Binding (AUTH-HARDEN, RHBK 26.4 RFC9449) | Accepted | 2026-08-22 |
| [0063](0063-compliance-service-aml-cft-pci-dss-audit-trail-standard.md) | Compliance Service — AML/CFT, PCI-DSS Req10 & Audit-Trail (COMPLIANCE-HARDEN) | Accepted | 2026-08-22 |
| [0064](0064-gateway-service-rate-limiting-3scale-and-edge-protection-standard.md) | Gateway Service — 3scale APIcast Edge & Rate Limiting (GATEWAY-HARDEN) | Accepted | 2026-08-22 |
| [0065](0065-api-portal-service-openapi-aggregation-and-developer-experience-standard.md) | API Portal Service — OpenAPI Aggregation & DX (PORTAL-HARDEN) | Accepted | 2026-08-22 |
| [0066](0066-polyrepo-pipeline-per-service-devsecops-standard.md) | Polyrepo Per-Service DevSecOps Pipeline (Monorepo→Polyrepo, 6 stages, SLSA) | Accepted | 2026-08-22 |
| [0067](0067-llm-integration-for-payu-services-standard.md) | LLM Integration for PayU Services — RAG, Guardrails & Private Deployment (BPPD, FinRAG-12B) | Deferred | 2026-08-24 |
| [0068](0068-keda-autoscaling-kafka-and-prometheus-standard.md) | KEDA Autoscaling — Kafka Lag & Prometheus Triggers (HPA++) | Accepted | 2026-08-24 |
| [0069](0069-openshift-4-22-platform-standard.md) | Red Hat OpenShift 4.22 Platform Standard | Accepted | 2026-08-24 |
| [0070](0070-e2e-test-environment-strategy-standard.md) | E2E Test Environment Strategy — Tiered Local → SIT → Preprod Smoke | Accepted | 2026-08-26 |

### B4.6 Go/No-Go Decision 2026-08-24 (ADR-0067 / ADR-0068)

> Evaluasi cost vs benefit + infra check (LLM 0 artifacts, KEDA 0 manifests) per B4.6 — ponytail.

| ADR | Decision | Rationale (cost vs benefit) | Artifacts |
|:---|:---|:---|:---|
| **ADR-0067 LLM RAG + BPPD guardrails** | **DEFERRED (NO-GO)** | Cost: 1× GPU (RTX 4090/ROCm) + OpenShift AI ServingRuntime vLLM + pgvector on CNPG `payu_analytics` + FPE 300ms + Wazuh 1y/7y + drift review 12-24m, lab `ExceededNodeResources 23 svcs` no GPU quota, 0 manifests (`values.yaml`, `LlmAssistPort`, `python-llm-proxy`, `llm-redteam.sh` absent audit 2026-08-24). Benefit: 5k tickets/mo triage + compliance narrative + statement/promo + kyc draft — semua masih rule/heuristic tanpa SLA breach, no validated KPI LLM > rules, residency sudah `EncryptedStringConverter` + blind index + RLS tanpa LLM, vLLM 20-50× cheaper baru scale 40 FIs. | No code — `infrastructure/platform/mlops/README.md` decision log, `infrastructure/platform/data/pgvector/README.md` deferred, ADR stays Proposed→Deferred; re-evaluasi Q when GPU quota + validated demand. |
| **ADR-0068 KEDA Kafka lagThreshold 10** | **GO (Accepted)** | Cost: KEDA 2.14 operator HA 2 replicas + PDB tiny, cluster-wide `watchNamespace=""`, no GPU. Benefit: langsung solves `ExceededNodeResources` via scale-to-zero `va/biller` idle 90% + lag-aware burst `transaction/wallet/gateway` (BI-FAST 1000 req/s) yang HPA CPU 80% gagal `P95 <500ms`; Strimzi 3.7 + `prometheus-operated:9090` sudah live, CNCF best practice, no Knative Istio. | Minimal manifests created: `infrastructure/platform/keda/base/` (`namespace`, `keda-operator`, `TriggerAuthentication` Vault VSO, `scaledobject-core` lag 10 min3 max10 + prometheus 1000 QPS, `scaledobject-sim` min0 lag5) + overlays `dev (min1 max3)` / `prod (min3 max10)` + kustomizations. Next: `oc apply -k` + `kcat produce 1000 → HPA 3→10 <30s` (acceptance). |


## 🚀 How to Create a New ADR

1. Copy `0001-template.md` to a new file: `XXXX-my-decision-title.md`. (Increment XXXX).
2. Fill out the template following the guidelines in `@principal-architect`.
3. Submit a PR and assign relevant engineers for review.
4. Once merged, update this `README.md` index.

---

> _Reference: PayU Agent Skills Guide -> @principal-architect_
