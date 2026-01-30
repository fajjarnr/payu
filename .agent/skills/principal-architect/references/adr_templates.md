# Architecture Decision Records (ADR) Templates

## Standard ADR Template

```markdown
# ADR-{NUMBER}: {TITLE}

## Status
{Proposed | Accepted | Deprecated | Superseded by ADR-XXX}

## Date
{YYYY-MM-DD}

## Context
{Describe the issue that is motivating this decision or change. 
What is the problem we're trying to solve?}

## Decision Drivers
- {driver 1: e.g., scalability requirements}
- {driver 2: e.g., cost constraints}
- {driver 3: e.g., team expertise}

## Considered Options
1. {Option 1}
2. {Option 2}
3. {Option 3}

## Decision
{Describe the decision that was made and why.}

## Consequences

### Positive
- {Consequence 1}
- {Consequence 2}

### Negative
- {Consequence 1}
- {Consequence 2}

### Neutral
- {Consequence 1}

## Compliance
- [ ] Reviewed by: {Name, Role}
- [ ] Approved by: {Name, Role}
- [ ] Implementation deadline: {Date}

## References
- {Link to RFC, PRD, or other documentation}
- {Link to related ADRs}
```

---

## ADR Examples

### ADR-001: Hexagonal Architecture for Core Banking

```markdown
# ADR-001: Hexagonal Architecture for Core Banking Services

## Status
Accepted

## Date
2025-06-15

## Context
PayU is building a new digital banking platform with 20+ microservices. We need 
a consistent architectural pattern that:
- Separates business logic from infrastructure concerns
- Enables easy testing without external dependencies
- Allows flexibility to change databases, message brokers, or external APIs
- Maintains consistency across teams

## Decision Drivers
- **Testability**: Business logic must be 100% unit-testable
- **Flexibility**: Infrastructure changes should not affect domain code
- **Compliance**: OJK requires clear audit trails of business decisions
- **Team Structure**: 5 teams working on different bounded contexts

## Considered Options

### Option 1: Layered Architecture (Traditional)
```
Controller → Service → Repository → Database
```
**Pros**: Simple, familiar to most developers
**Cons**: Domain logic often leaks into controllers, hard to test service layer in isolation

### Option 2: Hexagonal Architecture (Ports & Adapters)
```
Driving Adapters → Application → Domain ← Driven Adapters
```
**Pros**: Pure domain logic, infrastructure agnostic, highly testable
**Cons**: More boilerplate, steeper learning curve

### Option 3: Clean Architecture
```
Entities → Use Cases → Interface Adapters → Frameworks
```
**Pros**: Very flexible, clear dependency rules
**Cons**: Can be over-engineered for simpler services

## Decision
We will use **Hexagonal Architecture** for all core banking services 
(wallet-service, transaction-service, account-service, etc.).

Supporting services (notification-service, cms-service) may use simpler 
layered architecture where appropriate.

### Package Structure
```
id.payu.{service}/
├── domain/           # Pure business logic, no framework deps
│   ├── model/        # Entities, Value Objects
│   ├── port/in/      # Use Case interfaces
│   └── port/out/     # Repository/Client interfaces
├── application/      # Use Case implementations
└── adapter/          # Infrastructure (web, persistence, messaging)
```

### Enforcement
- ArchUnit tests in every service to validate layer dependencies
- Code review checklist includes architecture compliance
- Domain layer must have 0 Spring/JPA annotations

## Consequences

### Positive
- Business logic can be tested with simple unit tests (no mocks of Spring context)
- Easy to swap PostgreSQL for another DB if needed
- Clear separation makes code review more focused
- Aligns with DDD principles

### Negative
- More classes to maintain (mappers, DTOs, ports)
- Initial learning curve for junior developers
- Some duplication between domain entities and JPA entities

### Neutral
- Need to maintain mapping between domain and persistence models
- Requires discipline to not take shortcuts

## Compliance
- [x] Reviewed by: @principal-architect
- [x] Approved by: VP Engineering
- [x] Implementation deadline: 2025-07-01

## References
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [ADR-002: Domain-Driven Design Bounded Contexts](./ADR-002-ddd-bounded-contexts.md)
```

---

### ADR-002: Event Sourcing for Transaction Ledger

```markdown
# ADR-002: Event Sourcing for Transaction Ledger

## Status
Accepted

## Date
2025-07-01

## Context
The wallet-service manages user balances and transaction history. We need to:
- Maintain a complete audit trail of all balance changes
- Support point-in-time balance reconstruction
- Handle high throughput (10,000 TPS target)
- Comply with OJK requirements for transaction records

Current approach using UPDATE statements loses history.

## Decision Drivers
- **Auditability**: Every balance change must be traceable
- **Compliance**: 7-year retention requirement (POJK 12/2017)
- **Performance**: High-volume financial transactions
- **Reconciliation**: Support daily recon against switching files

## Considered Options

### Option 1: Traditional CRUD with Audit Table
- Separate `audit_log` table with triggers
- **Pros**: Simple, familiar
- **Cons**: Audit table grows large, triggers slow down writes, reconstruction is expensive

### Option 2: Event Sourcing with CQRS
- Append-only event store, materialized views for queries
- **Pros**: Complete history, fast writes, natural fit for recon
- **Cons**: Complex, eventual consistency, more infrastructure

### Option 3: Temporal Tables (PostgreSQL)
- Built-in versioning with `FOR SYSTEM_TIME` queries
- **Pros**: Native PostgreSQL feature, less code
- **Cons**: Limited query flexibility, storage overhead

## Decision
We will implement **Event Sourcing with CQRS** for the wallet ledger.

### Architecture
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Command   │────►│   Event     │────►│   Read      │
│   Service   │     │   Store     │     │   Model     │
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │
      ▼                   ▼                   ▼
  Validates           Kafka Topic         PostgreSQL
  & Appends          (CDC Stream)         Read Replica
```

### Event Schema
```java
public record LedgerEntryCreated(
    UUID eventId,
    UUID accountId,
    UUID transactionId,
    BigDecimal amount,
    String entryType,  // DEBIT, CREDIT
    BigDecimal balanceAfter,
    Instant timestamp,
    String description,
    Map<String, String> metadata
) {}
```

### Balance Calculation
```sql
-- Current balance from event stream
SELECT SUM(
  CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END
) AS balance
FROM ledger_events
WHERE account_id = ?
```

## Consequences

### Positive
- Complete audit trail with no additional code
- Point-in-time balance queries (SELECT ... WHERE timestamp < X)
- Natural fit for reconciliation (events = source of truth)
- High write throughput (append-only)

### Negative
- Read queries require materialized views or projections
- Eventual consistency between write and read models
- Team needs training on event sourcing patterns
- More complex debugging (trace through events)

### Neutral
- Event schema versioning required (Avro/Protobuf recommended)
- Snapshot strategy needed for long-lived accounts (> 10,000 events)

## Compliance
- [x] Reviewed by: @data-architect, @financial-ops-architect
- [x] Approved by: VP Engineering, Head of Compliance
- [x] Implementation deadline: 2025-08-01

## References
- [Event Sourcing by Martin Fowler](https://martinfowler.com/eaaDev/EventSourcing.html)
- [ADR-001: Hexagonal Architecture](./ADR-001-hexagonal-architecture.md)
- [POJK 12/2017 - Electronic Transaction Records](https://ojk.go.id)
```

---

### ADR-003: API Versioning Strategy

```markdown
# ADR-003: URL Path-Based API Versioning

## Status
Accepted

## Date
2025-07-15

## Context
PayU exposes APIs to mobile apps, web apps, and third-party partners. We need a 
versioning strategy that:
- Allows breaking changes without disrupting existing clients
- Is easy for partners to understand and implement
- Supports gradual migration between versions
- Enables deprecation with clear timelines

## Decision Drivers
- **Compatibility**: Mobile apps may not update immediately
- **Partner SLAs**: Partners need 6+ months notice for breaking changes
- **Simplicity**: Easy to understand and route
- **Monitoring**: Clear metrics per version

## Considered Options

### Option 1: URL Path Versioning
`/api/v1/accounts`, `/api/v2/accounts`
- **Pros**: Very clear, easy to route, simple caching
- **Cons**: URL pollution, encourages "big bang" version bumps

### Option 2: Header Versioning
`Accept: application/vnd.payu.v1+json`
- **Pros**: Clean URLs, RESTful
- **Cons**: Hard to test in browser, less visible, caching issues

### Option 3: Query Parameter
`/api/accounts?version=1`
- **Pros**: Simple, explicit
- **Cons**: Not RESTful, optional parameters risky

## Decision
We will use **URL Path Versioning** with the following conventions:

### URL Structure
```
/api/v{MAJOR}/resource
```

### Version Lifecycle
| Phase | Duration | Support Level |
|:------|:---------|:--------------|
| Current | Active | Full support |
| Deprecated | 12 months | Bug fixes only |
| Sunset | 3 months | Read-only, warnings |
| Retired | - | Returns 410 Gone |

### Breaking vs Non-Breaking Changes

**Non-Breaking (No version bump)**:
- Adding new optional fields
- Adding new endpoints
- Adding new enum values (if clients handle unknown)
- Changing internal implementation

**Breaking (Requires new version)**:
- Removing/renaming fields
- Changing field types
- Changing validation rules
- Removing endpoints

### Migration Support
```json
// Response includes migration hints
{
  "data": {...},
  "_meta": {
    "api_version": "v1",
    "deprecated": true,
    "sunset_date": "2026-06-01",
    "migration_guide": "https://docs.payu.id/api/migration/v1-to-v2"
  }
}
```

## Consequences

### Positive
- Crystal clear which version is being used
- Easy to route at API Gateway level
- Simple to monitor and alert on deprecated version usage
- Partners appreciate explicit versioning

### Negative
- Multiple versions to maintain simultaneously
- Controller duplication (can mitigate with shared services)
- URL aesthetics

## Compliance
- [x] Reviewed by: @api-architect
- [x] Approved by: VP Engineering
- [x] Implementation deadline: 2025-08-01

## References
- [PayU API Standards](../api/API_STANDARDS.md)
- [Stripe API Versioning](https://stripe.com/blog/api-versioning)
```

---

## RFC (Request for Comments) Template

```markdown
# RFC-{NUMBER}: {TITLE}

## Meta
- **Author**: {Name}
- **Created**: {YYYY-MM-DD}
- **Status**: {Draft | Review | Approved | Rejected | Withdrawn}
- **Reviewers**: {List of reviewers}
- **Due Date**: {Review deadline}

## Summary
{One paragraph summary of the proposal}

## Motivation
{Why are we doing this? What problems does it solve?}

## Detailed Design
{Technical details of the proposal}

### API Changes
{If applicable}

### Data Model Changes
{If applicable}

### Migration Plan
{How do we get from current state to proposed state?}

## Alternatives Considered
{What other approaches were considered?}

## Security Considerations
{Any security implications?}

## Performance Considerations
{Any performance implications?}

## Rollout Plan
{How will this be deployed?}

## Open Questions
{Unresolved questions for discussion}

## Timeline
| Milestone | Target Date |
|:----------|:------------|
| RFC Approved | YYYY-MM-DD |
| Implementation Start | YYYY-MM-DD |
| Testing Complete | YYYY-MM-DD |
| Production Rollout | YYYY-MM-DD |

---

## Comments
{Discussion thread}
```

---

## Decision Log Template

```markdown
# Decision Log - {Service/Project Name}

| ID | Date | Decision | Rationale | Outcome | Owner |
|:---|:-----|:---------|:----------|:--------|:------|
| D001 | 2025-06-01 | Use PostgreSQL over MySQL | Better JSON support, partitioning | Approved | @data-architect |
| D002 | 2025-06-15 | Spring Boot 3.4 over Quarkus | Team expertise, ecosystem | Approved | @principal-architect |
| D003 | 2025-07-01 | Kafka over RabbitMQ | Higher throughput, CDC support | Approved | @integration-architect |
```
