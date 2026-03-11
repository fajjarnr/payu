# ADR-0015: Process Automation with Red Hat Process Automation Manager (RHPAM/Kogito)

**Status**: Accepted  
**Date**: 2026-03-11  
**Deciders**: Platform Engineering, Core Banking Engineering, Risk & Compliance

## Context

PayU sebagai platform core banking & payment gateway memiliki banyak **business logic yang berupa rules dan workflows** yang saat ini **di-hardcode dalam Java services**. Contoh konkret:

1. **Credit Scoring** (`lending-service`): Rules scoring KYC, tenure, dan transaction history di-hardcode sebagai `if-else` chains di `EnhancedCreditScoringService.java` (126 LOC of scoring rules).
2. **Payment Routing** (`gateway-service`): Route resolution di `RouteRegistry.java` menggunakan static mappings. Fee calculation dan channel selection (BI-FAST vs RTGS vs SKN) belum dynamic.
3. **Fraud Detection** (`analytics-service`): Velocity checks dan geo-anomaly detection rules embedded di application code.
4. **KYC/Compliance Workflows** (`kyc-service`, `compliance-service`): Multi-step verification flows hardcoded dalam service methods.
5. **Promotion Eligibility** (`promotion-service`): Complex eligibility rules yang sering berubah mengikuti campaign.

### Masalah dengan Pendekatan Saat Ini

- **Setiap perubahan rules** membutuhkan code change → build → test → deploy cycle (~30-60 menit).
- **Business/compliance team** tidak bisa mengubah rules tanpa engineering involvement.
- **Audit trail** untuk rule changes hanya melalui Git history, tidak real-time queryable.
- **Regulatory compliance** (OJK/BI) sering membutuhkan rapid rule updates yang tidak bisa menunggu release cycle.

## Decision Drivers

- **Regulatory agility**: OJK/BI sering mengubah threshold dan compliance rules. Perlu update tanpa full redeploy.
- **Red Hat ecosystem alignment**: PayU sudah menggunakan Red Hat stack (OpenShift, AMQ Streams, Red Hat SSO). RHPAM/Kogito adalah natural extension.
- **Business empowerment**: Risk team dan compliance team perlu self-service rule management.
- **Auditability**: Semua rule changes harus traceable untuk audit OJK/PCI-DSS.
- **Performance**: Rules engine tidak boleh menambah latency signifikan pada payment processing (<5ms overhead acceptable).
- **Incremental adoption**: Bisa diadopsi bertahap tanpa big-bang migration.

## Considered Options

### Option A: Kogito + Drools (Cloud-Native RHPAM)

Kogito adalah cloud-native successor dari RHPAM, didesain untuk Quarkus/Spring Boot.

**Pros:**

- Cloud-native, designed for containers & Kubernetes/OpenShift
- Supports BPMN 2.0, DMN 1.4, DRL, and Predictive Model Markup Language (PMML)
- Spring Boot integration via `kogito-spring-boot-starter`
- Quarkus integration via `kogito-quarkus` (native compile support)
- Event-driven via CloudEvents (compatible with existing Kafka setup)
- Lower resource footprint than traditional RHPAM/jBPM
- Open-source (Apache 2.0) — community version of RHPAM

**Cons:**

- Kogito still maturing (some enterprise features in Red Hat build only)
- Less visual tooling compared to traditional RHPAM Business Central
- Requires understanding of DRL/DMN authoring

### Option B: Traditional RHPAM (jBPM + Drools + Business Central)

Full enterprise BPM/BRMS suite with visual editors.

**Pros:**

- Mature, production-proven in banking (10+ years)
- Business Central UI for non-technical rule authoring
- Built-in KIE Server for rule execution
- Comprehensive audit logging and versioning
- Red Hat support with SLA

**Cons:**

- Heavy resource footprint (~3-4 pods minimum: Business Central, KIE Server, databases)
- Monolithic architecture, doesn't fit well in microservices
- Slower startup (not suitable for scale-to-zero)
- Overkill for PayU's current scale

### Option C: Embedded Drools Only (Rules Engine tanpa BPM)

Use Drools rules engine embedded directly in services that need it, without full BPM capability.

**Pros:**

- Lightweight — just a library dependency, no additional pods
- Zero infrastructure overhead
- Direct API, no network hop for rule evaluation
- Can start immediately with existing services
- DRL files can be hot-reloaded from filesystem/Git/database
- Familiar Java API

**Cons:**

- No visual rule editor (DRL files must be authored by developers or via tooling)
- No built-in BPMN workflow orchestration
- Rule management distributed across services (no central governance)
- BPM workflows need separate solution later

## Decision

**Phased adoption of Drools/Kogito** with the following approach:

| Phase                 | Scope                                  | Technology                                         | Timeline        |
| :-------------------- | :------------------------------------- | :------------------------------------------------- | :-------------- |
| **Phase 1** (Now)     | Credit Scoring + Fraud Detection Rules | **Embedded Drools 9.x** via shared `rules-starter` | Sprint 1-2      |
| **Phase 2** (Q2 2026) | Payment Routing DMN + Promotion Rules  | **DMN Decision Tables** (Drools DMN)               | Sprint 3-4      |
| **Phase 3** (Q3 2026) | Lending Workflow + KYC/AML Workflow    | **Kogito BPMN** (Spring Boot/Quarkus)              | Sprint 5-8      |
| **Phase 4** (Q4 2026) | Business Central (Management Console)  | **Kogito Management Console**                      | Post-evaluation |

**Phase 1 starts with Option C (Embedded Drools)** as a shared library `rules-starter`, then evolves to Kogito as workflows are needed.

## Rationale

1. **Incremental risk**: Starting with embedded Drools has zero infrastructure overhead. If it doesn't work, we just remove the dependency.
2. **Immediate value**: Credit scoring rules and fraud detection rules are the most frequently changed and benefit most from externalization.
3. **Red Hat alignment**: Drools is the core engine behind RHPAM/Kogito. Starting with Drools ensures smooth upgrade path.
4. **Performance**: Embedded Drools adds <1ms overhead per rule evaluation — well within our <5ms budget.
5. **Developer familiarity**: DRL syntax is intuitive for Java developers. DMN decision tables are accessible to business analysts.

## Consequences

**Positive:**

- Credit scoring rules can be updated via DRL file changes without code recompilation
- Fraud detection rules can be hot-reloaded in production (with proper audit logging)
- Clear separation between business rules and application logic
- Foundation for future Kogito/BPM adoption
- OJK compliance rules can be updated in minutes instead of hours
- Shared `rules-starter` ensures consistent rule engine usage across services

**Negative:**

- Additional dependency in the build (Drools ~15MB)
- Team needs to learn DRL/DMN authoring
- Without Business Central, rule management is developer-driven (acceptable for Phase 1)
- Rule versioning needs to be managed via Git or database (not visual)

## Implementation Notes

### Phase 1: Shared `rules-starter` Library

1. Create `backend/shared/rules-starter/` as a new shared module
2. Dependencies: `drools-engine`, `drools-model-compiler`, `drools-decisiontables`
3. Provide `RulesEngineService` as auto-configured Spring Bean
4. Support rule loading from:
   - Classpath DRL files (default)
   - External filesystem path (configurable)
   - Database (future: for hot-reload)
5. Integrate with `security-starter` for audit logging of rule evaluations

### Target Services for Phase 1

| Service             | Use Case             | Rule Type             |
| :------------------ | :------------------- | :-------------------- |
| `lending-service`   | Credit Scoring       | DRL + Decision Tables |
| `analytics-service` | Fraud Detection      | DRL + CEP             |
| `gateway-service`   | Payment Routing      | DMN (Phase 2)         |
| `promotion-service` | Campaign Eligibility | DRL (Phase 2)         |

### Example: Credit Scoring DRL (replaces EnhancedCreditScoringService hardcoded logic)

```drools
package id.payu.lending.rules

import id.payu.lending.domain.model.CreditScoreFact

rule "KYC Approved Score"
    when
        $fact : CreditScoreFact(kycStatus == "APPROVED")
    then
        $fact.addScore(50);
end

rule "Tenure 3+ Years Bonus"
    when
        $fact : CreditScoreFact(accountTenureMonths >= 36)
    then
        $fact.addScore(40);
end

rule "High Success Rate Bonus"
    when
        $fact : CreditScoreFact(transactionSuccessRate >= 0.98)
    then
        $fact.addScore(30);
end
```

### Architecture Diagram

```text
┌─────────────────────────────────────────────────────────────┐
│                    PayU Platform                             │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              shared/rules-starter                     │   │
│  │  ┌────────────────┐  ┌─────────────────────────────┐ │   │
│  │  │ RulesEngine    │  │ Rule Sources                │ │   │
│  │  │ Service        │  │  • Classpath DRL            │ │   │
│  │  │                │  │  • External Files           │ │   │
│  │  │ • evaluate()   │  │  • Database (hot-reload)    │ │   │
│  │  │ • reload()     │  │  • DMN Decision Tables      │ │   │
│  │  │ • audit()      │  └─────────────────────────────┘ │   │
│  │  └────────────────┘                                   │   │
│  └──────────────────────┬────────────────────────────────┘   │
│                         │ Used by                             │
│  ┌──────────────┐  ┌────┴───────────┐  ┌──────────────────┐ │
│  │ lending-     │  │ analytics-     │  │ promotion-       │ │
│  │ service      │  │ service        │  │ service          │ │
│  │              │  │                │  │                  │ │
│  │ Credit Score │  │ Fraud Rules    │  │ Campaign Rules   │ │
│  │ DRL + DMN    │  │ DRL + CEP      │  │ DRL              │ │
│  └──────────────┘  └────────────────┘  └──────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

> _Created: 2026-03-11 | Relates to: ADR-0002 (Spring Boot), ADR-0003 (Quarkus), ADR-0014 (API Management)_
