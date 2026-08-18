# ADR-0007: Database per Service Pattern

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Architecture Team, Engineering Leads

## Context

PayU microservices architecture requires a data persistence strategy. We need to decide between:

- Shared database (all services use one DB)
- Database per service (each service has own DB)

## Decision Drivers

- **Independent Deployment**: Services should deploy independently
- **Technology Freedom**: Different services can use different DB schemas
- **Fault Isolation**: One service's DB issue shouldn't affect others
- **Data Ownership**: Clear ownership of data models

## Considered Options

### Option 1: Database per Service

- **Pros**:
  - Independent deployment
  - Fault isolation
  - Clear data ownership
  - Technology flexibility
  - Teams can work independently
- **Cons**:
  - No cross-service JOINs
  - Distributed transactions required
  - More operational overhead
- **Complexity**: Medium
- **Rationale**: Best for microservices architecture

### Option 2: Shared Database

- **Pros**:
  - Simpler transaction management
  - Cross-table queries possible
  - Less operational overhead
- **Cons**:
  - Tight coupling between services
  - Shared schema changes
  - No independent scaling
  - Single point of failure
- **Complexity**: Low
- **Rationale**: Anti-pattern for microservices

### Option 3: Hybrid Approach

- **Pros**:
  - Flexibility to choose per domain
- **Cons**:
  - Inconsistent patterns
  - Confusing for developers
  - Harder to enforce standards
- **Complexity**: High
- **Rationale**: Inconsistency creates confusion

## Decision

**Choose Database per Service** for all services.

Each microservice has its own database:

- account-service → `payu_account`
- transaction-service → `payu_transaction`
- wallet-service → `payu_wallet`
- etc.

## Rationale

1. **Independent Deployment**: Services can deploy without schema conflicts
2. **Fault Isolation**: One DB issue doesn't affect all services
3. **Data Ownership**: Clear boundaries for team responsibilities
4. **Technology Freedom**: Services can optimize their own schemas
5. **Microservices Principle**: Aligns with bounded contexts

## Consequences

**Positive**:

- Clear service boundaries
- Independent deployments
- Fault isolation
- Teams work independently

**Negative**:

- No cross-service JOINs
- Distributed transactions required (Saga pattern)
- More operational overhead
- Reporting requires API calls or data warehouse

**Trade-offs Accepted**:

- Accept no JOINs for service independence
- Accept Saga pattern for distributed transactions
- Accept operational overhead for fault isolation

## Implementation Notes

### Database Naming Convention

```
payu_{service_name}
```

Examples:

- `payu_account` (for account-service)
- `payu_transaction` (for transaction-service)
- `payu_wallet` (for wallet-service)

### Cross-Service Queries

Use API calls or event streaming instead of JOINs:

```java
// ❌ BAD: Direct database access
SELECT * FROM payu_account.accounts a
JOIN payu_transaction.transactions t ON a.id = t.account_id;

// ✅ GOOD: API call or event stream
// 1. Get account via API
Account account = accountApiClient.getAccount(accountId);

// 2. Or consume account domain events
@KafkaListener(topics = "payu.accounts")
public void handleAccountEvent(AccountEvent event) {
    // Process account change
}
```

### Distributed Transactions

Use Saga pattern instead of 2PC:

```java
// Transfer Saga
@Saga
public class TransferSaga {
    // Steps:
    // 1. Reserve balance (wallet-service)
    // 2. Validate recipient (account-service)
    // 3. Commit transfer (wallet-service)
    // 4. Compensate if any step fails
}
```

### Reporting and Analytics

Use data warehouse for reporting:

- Debezium CDC to capture changes
- Kafka to stream events
- Analytics database to aggregate data

---

## Evolution & Subsequent Standards

Prinsip dasar **Database per Service** pada ADR ini tetap aktif dan menjadi fondasi utama persistensi data PayU. Untuk detail implementasi teknis mutakhir yang menyempurnakan dokumen ini, rujuk dokumen ADR berikut:

1. **Event Streaming & Kafka Governance ([ADR-0026](0026-kafka-topic-governance-and-dlq-strategy.md))**:
   - Format topic menggunakan standar CloudEvents 1.0.2: `payu.<domain>.<event-type>.v<n>` (bukan topic generik seperti contoh awal).
   - Seluruh publishing event wajib melalui Transactional Outbox Pattern via `outbox-starter`.
2. **Money & Idempotency Invariants ([ADR-0022](0022-money-idempotency-standard.md))**:
   - Double-entry ledger, `BigDecimal` HALF_EVEN, dan `X-Idempotency-Key` pada seluruh mutasi finansial antar-database.
3. **Database High Availability & PITR ([ADR-0031](0031-database-resilience-pitr-and-disaster-recovery.md))**:
   - CloudNativePG (CNPG) Barman Cloud continuous WAL streaming ke S3 dengan target RPO=0 dan RTO < 5 menit.
4. **Multi-Tenant Row-Level Security ([ADR-0033](0033-database-row-level-security-and-multi-tenant-isolation-standard.md))**:
   - Di dalam masing-masing database service, isolasi multi-tenant (TokoBapak, Nobar, Dolan, dll.) ditegakkan menggunakan **PostgreSQL RLS (`FORCE ROW LEVEL SECURITY`) + GUC `app.tenant_id`**.

---

*Created via @principal-architect (Updated 2026-08-18 with ADR-0026/0031/0033 cross-references)*
