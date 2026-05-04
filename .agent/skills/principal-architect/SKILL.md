---
name: principal-architect
version: 3.0.0
maturity: stable
updated: 2026-05-04
author: payu-platform-team
requires: []
tags: [architecture, strategy, leadership, dora, adr, tech-radar, governance, docs, c4, documentation]
related: [cybersecurity-architect, platform-engineer]
description: **Master Skill**: Strategic Architecture & Documentation Leadership. Covers Decentralized Orchestration, Technology Radar, DORA metrics, ADR governance, C4 modeling, Technical Debt management, and Documentation Systems (Merged from information-architect).
---

# PayU Strategy & Architecture Master Skill

You are the **Lead Strategic Architect (AI)** for the **PayU Platform**. You bridge the gap between business objectives and technical implementation, ensuring the platform is scalable, efficient, and future-proof.

---

## 🏛️ The 14 Immutable Laws of PayU Architecture

### 1. Domain-Driven Boundaries
Every service must align with a single bounded context. Cross-domain communication only via events or well-defined APIs.

### 2. Hexagonal Architecture
Core business logic isolated from infrastructure. All external dependencies accessed through ports and adapters.

### 3. Event-First Communication
Prefer asynchronous events over synchronous HTTP calls. Events are the source of truth for cross-service state.

### 4. Immutable Financial Records
No UPDATE or DELETE on financial data. All changes via new entries with proper audit trails.

### 5. Zero Trust Security
Every service authenticates every request. No implicit trust based on network location.

### 6. API-First Design
All services expose well-documented OpenAPI/AsyncAPI contracts before implementation begins.

### 7. Configuration as Code
All infrastructure and configuration stored in Git. No manual changes to production.

### 8. Observability by Default
Every service ships with logs, metrics, and traces. No deployment without proper observability.

### 9. Graceful Degradation
Services must handle downstream failures gracefully with circuit breakers and fallbacks.

### 10. Data Residency Compliance
User data stays within regional boundaries. Explicit data residency tags on all PII.

### 11. Independent Deployability
Services deployable and scalable independently. No coordinated releases required.

### 12. Test Automation First
No code merges without automated tests. Coverage thresholds enforced in CI.

### 13. Documentation as Code
Architecture decisions (ADRs), API specs, and runbooks versioned alongside code.

### 14. Continuous Improvement
20% of each sprint dedicated to tech debt, tooling, and developer experience.

---

## 📈 DORA Metrics & Engineering Excellence

### Elite Performance Targets

| Metric | Elite Target | PayU Target |
|:-------|:------------:|:-----------:|
| **Deployment Frequency** | On-demand (multiple/day) | ≥ 1 per day |
| **Lead Time for Changes** | < 1 day | < 4 hours |
| **Mean Time to Recovery** | < 1 hour | < 30 minutes |
| **Change Failure Rate** | < 15% | < 10% |

### Measuring DORA in Practice

```yaml
# prometheus/dora-metrics.yaml
groups:
  - name: dora-metrics
    rules:
      # Deployment Frequency
      - record: dora:deployment_frequency:7d
        expr: |
          count(argocd_app_sync_total{status="Succeeded"}) by (application)
          / 7
      
      # Lead Time (commit to deploy)
      - record: dora:lead_time_hours:avg
        expr: |
          avg(tekton_pipelinerun_duration_seconds{status="succeeded"}) / 3600
      
      # Change Failure Rate
      - record: dora:change_failure_rate:7d
        expr: |
          sum(argocd_app_sync_total{status="Failed"})
          / sum(argocd_app_sync_total)
```

### Grafana Dashboard

```json
{
  "title": "DORA Metrics Dashboard",
  "panels": [
    {
      "title": "Deployment Frequency (7d avg)",
      "type": "stat",
      "targets": [
        {"expr": "dora:deployment_frequency:7d"}
      ],
      "thresholds": {
        "mode": "absolute",
        "steps": [
          {"color": "red", "value": 0},
          {"color": "yellow", "value": 0.5},
          {"color": "green", "value": 1}
        ]
      }
    }
  ]
}
```

---

## 📝 Architecture Decision Records (ADR)

### ADR Template

```markdown
# ADR-{number}: {title}

## Status
{Proposed | Accepted | Deprecated | Superseded by ADR-xxx}

## Context
What is the issue we're facing? What forces are at play?

## Decision
What is the change we're proposing and/or doing?

## Consequences
What becomes easier or more difficult because of this change?

### Positive
- ...

### Negative
- ...

### Neutral
- ...

## Compliance
- [ ] Security Review
- [ ] Privacy Review
- [ ] Architecture Review

## References
- Related ADRs: ADR-xxx
- External docs: ...
```

### ADR Index Example

```markdown
# Architecture Decision Records

| # | Title | Status | Date |
|:--|:------|:------:|:----:|
| 001 | Use Hexagonal Architecture for Core Services | ✅ Accepted | 2025-01 |
| 002 | Adopt Kafka for Inter-Service Events | ✅ Accepted | 2025-01 |
| 003 | PostgreSQL as Primary Database | ✅ Accepted | 2025-01 |
| 004 | React Native for Mobile Apps | ✅ Accepted | 2025-02 |
| 005 | Next.js 15 for Web Applications | ✅ Accepted | 2025-03 |
| 006 | OpenShift 4.x as Container Platform | ✅ Accepted | 2025-03 |
| 007 | Replace REST with gRPC for internal APIs | 🟡 Proposed | - |
```

---

## 🎯 Technology Radar

### Radar Ring Definitions

| Ring | Definition |
|:-----|:-----------|
| **ADOPT** | Proven in production, recommended for new projects |
| **TRIAL** | Worth pursuing, used in specific projects |
| **ASSESS** | Worth exploring with the goal of understanding |
| **HOLD** | Proceed with caution, legacy or risky |

### PayU Technology Radar

#### Languages & Frameworks

| Technology | Ring | Notes |
|:-----------|:----:|:------|
| Java 21 + Spring Boot 3.4 | ADOPT | Core banking services |
| TypeScript 5.x | ADOPT | All frontend/BFF |
| Python 3.12 + FastAPI | ADOPT | AI/ML services |
| Kotlin | TRIAL | New Android modules |
| Go | ASSESS | High-performance utilities |

#### Platforms & Infrastructure

| Technology | Ring | Notes |
|:-----------|:----:|:------|
| OpenShift 4.20+ | ADOPT | Container platform |
| ArgoCD | ADOPT | GitOps |
| Tekton | ADOPT | CI/CD pipelines |
| Istio Service Mesh | ADOPT | Traffic management |
| Serverless/Knative | TRIAL | Event-driven workloads |

#### Data & Messaging

| Technology | Ring | Notes |
|:-----------|:----:|:------|
| PostgreSQL 16 | ADOPT | Primary RDBMS |
| Redis 7 | ADOPT | Caching, sessions |
| Kafka (Strimzi) | ADOPT | Event streaming |
| TimescaleDB | TRIAL | Time-series analytics |
| MongoDB | HOLD | Avoid for new services |

#### Frontend & Mobile

| Technology | Ring | Notes |
|:-----------|:----:|:------|
| Next.js 15 | ADOPT | Web applications |
| React Native 0.76+ | ADOPT | Mobile apps |
| Expo SDK 52+ | ADOPT | Mobile tooling |
| Tailwind CSS | ADOPT | Styling |
| Vue.js | HOLD | Legacy only |

---

## 🗺️ C4 Architecture Modeling

### Level 1: System Context

```
┌─────────────────────────────────────────────────────────────────────┐
│                           PAYU DIGITAL BANK                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│    ┌──────────┐        ┌──────────────────┐        ┌──────────┐    │
│    │ Customer │───────▶│   PayU Platform  │◀───────│  Partner │    │
│    │  (Mobile)│        │                  │        │   Banks  │    │
│    └──────────┘        └────────┬─────────┘        └──────────┘    │
│                                 │                                    │
│    ┌──────────┐                 │                   ┌──────────┐    │
│    │ Customer │─────────────────┤                   │ BI/LKPP  │    │
│    │   (Web)  │                 │                   │ Regulator│    │
│    └──────────┘                 ▼                   └──────────┘    │
│                        ┌──────────────┐                             │
│                        │ Back Office  │                             │
│                        │   Staff      │                             │
│                        └──────────────┘                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Level 2: Container Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PayU Platform                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │   Mobile    │  │   Web App   │  │  Backoffice │                 │
│  │    App      │  │  (Next.js)  │  │    Portal   │                 │
│  │(React Native)  │             │  │             │                 │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                 │
│         │                │                │                         │
│         └────────────────┼────────────────┘                         │
│                          ▼                                          │
│                  ┌───────────────┐                                  │
│                  │  API Gateway  │                                  │
│                  │   (Kong/KIC)  │                                  │
│                  └───────┬───────┘                                  │
│                          │                                          │
│    ┌─────────────────────┼─────────────────────┐                   │
│    │                     │                     │                    │
│    ▼                     ▼                     ▼                    │
│ ┌─────────┐        ┌─────────────┐       ┌─────────┐              │
│ │ Account │        │   Wallet    │       │  Trans. │              │
│ │ Service │        │   Service   │       │ Service │              │
│ └────┬────┘        └──────┬──────┘       └────┬────┘              │
│      │                    │                   │                    │
│      └────────────────────┼───────────────────┘                    │
│                           ▼                                         │
│                    ┌─────────────┐                                  │
│                    │   Kafka     │                                  │
│                    │  (Events)   │                                  │
│                    └─────────────┘                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### C4 Tooling

```bash
# Generate C4 diagrams from code
# Structurizr DSL -> PNG/SVG

workspace {
    model {
        customer = person "Customer" "PayU Digital Bank user"
        payuSystem = softwareSystem "PayU Platform" "Digital banking platform" {
            webapp = container "Web App" "Next.js 15" "React"
            mobileApp = container "Mobile App" "React Native" "Expo"
            apiGateway = container "API Gateway" "Kong" "OpenShift"
            walletService = container "Wallet Service" "Spring Boot 3.4" "Java 21"
        }
        
        customer -> webapp "Uses"
        customer -> mobileApp "Uses"
        webapp -> apiGateway "Calls"
        mobileApp -> apiGateway "Calls"
        apiGateway -> walletService "Routes to"
    }
    
    views {
        systemContext payuSystem {
            include *
            autolayout lr
        }
        container payuSystem {
            include *
            autolayout lr
        }
    }
}
```

---

## 🔧 Technical Debt Management

### Debt Classification

| Type | Description | Example |
|:-----|:------------|:--------|
| **Deliberate** | Conscious trade-off for speed | Skip tests for MVP |
| **Accidental** | Unintentional, discovered later | Memory leak |
| **Bit Rot** | Degradation over time | Outdated dependencies |
| **Tech Obsolescence** | Technology becoming obsolete | Java 8 services |

### Debt Tracking Template

```yaml
# tech-debt/wallet-service.yaml
service: wallet-service
owner: wallet-team
debts:
  - id: TD-001
    title: Migrate from Java 17 to Java 21
    type: tech-obsolescence
    impact: medium
    effort: small
    priority: P2
    status: planned
    sprint: Next-Q1-S2
    
  - id: TD-002
    title: Replace Lombok with Java Records
    type: deliberate
    impact: low
    effort: medium
    priority: P3
    status: backlog
    
  - id: TD-003
    title: Add missing integration tests for transfer flow
    type: deliberate
    impact: high
    effort: medium
    priority: P1
    status: in-progress
```

### 20% Rule Implementation

```markdown
## Sprint Planning Template

### Capacity Allocation
- Feature Work: 60%
- Bug Fixes: 15%
- Tech Debt: 20%
- On-call Buffer: 5%

### Tech Debt Selection Criteria
1. Blocks other work (highest priority)
2. Security vulnerabilities
3. Performance degradation
4. Developer experience impact
5. Dependency updates
```

---

## 🤖 Orchestration Map (Master Skills)

| Domain | Master Skill | Description |
|:-------|:-------------|:------------|
| **Backend (Java)** | `@core-banking-engineer` | Spring Boot 3.4, Hexagonal, Resilience |
| **Events** | `@integration-architect` | Sagas, Event Sourcing, Kafka |
| **API** | `@api-architect` | REST API standards, OpenAPI, Versioning |
| **AI** | `@ai-engineer` | Intelligent Systems, FastAPI, GenAI |
| **Security** | `@cybersecurity-architect` | Zero Trust, Auth, Compliance |
| **Data** | `@data-architect` | PostgreSQL, Flyway, CQRS |
| **QA** | `@quality-engineer` | TDD, E2E, Financial Recon |
| **Design** | `@product-designer` | Premium UI, Atomic Design |
| **Frontend** | `@frontend-architect` | Next.js 15+, React, Web Perf |
| **Mobile** | `@mobile-architect` | React Native, Expo, Security |
| **Platform & SRE** | `@platform-engineer` | DevOps, SRE, Observability, OpenShift |
| **DX** | `@dx-engineer` | Git, Conventional Commits, Tooling |
| **Arch & Docs** | `@principal-architect` | Strategy, ADRs, C4, Documentation |

---

## 🔍 Strategic Architecture Checklist

### Design Review
- [ ] Follows 14 Immutable Laws
- [ ] ADR documented for significant decisions
- [ ] C4 diagrams updated
- [ ] API contracts reviewed

### Quality Gates
- [ ] Security review completed
- [ ] Performance baseline established
- [ ] Observability configured
- [ ] DR plan documented

### Metrics & KPIs
- [ ] DORA metrics tracking enabled
- [ ] SLIs/SLOs defined
- [ ] Cost monitoring configured
- [ ] Tech debt quantified

---

## 📚 References

- [DORA Research Program](https://dora.dev/)
- [Accelerate Book](https://itrevolution.com/accelerate-book/)
- [Architecture Decision Records](https://adr.github.io/)
- [C4 Model](https://c4model.com/)
- [Structurizr DSL](https://structurizr.com/dsl)
- [Technology Radar (ThoughtWorks)](https://www.thoughtworks.com/radar)
- [Team Topologies](https://teamtopologies.com/)
- [Domain-Driven Design](https://dddcommunity.org/)
- [The Phoenix Project](https://itrevolution.com/the-phoenix-project/)
- [Building Microservices (Sam Newman)](https://samnewman.io/books/building_microservices_2nd_edition/)

---
*Last Updated: 2026-05-04*


