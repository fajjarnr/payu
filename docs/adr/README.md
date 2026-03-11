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

## 🚀 How to Create a New ADR

1. Copy `0001-template.md` to a new file: `XXXX-my-decision-title.md`. (Increment XXXX).
2. Fill out the template following the guidelines in `@principal-architect`.
3. Submit a PR and assign relevant engineers for review.
4. Once merged, update this `README.md` index.

---

> _Reference: PayU Agent Skills Guide -> @principal-architect_
