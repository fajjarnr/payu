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

## 🚀 How to Create a New ADR

1. Copy `0001-template.md` to a new file: `XXXX-my-decision-title.md`. (Increment XXXX).
2. Fill out the template following the guidelines in `@principal-architect`.
3. Submit a PR and assign relevant engineers for review.
4. Once merged, update this `README.md` index.

---

> _Reference: PayU Agent Skills Guide -> @principal-architect_
