# ADR-0006: PostgreSQL 16 as Primary Database

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: Architecture Team, Engineering Leads

## Context

PayU platform requires a relational database for:
- ACID transactions for financial operations
- Complex queries and joins
- Mature tooling and ecosystem
- JSONB support for flexible schemas

## Decision Drivers

- **ACID Compliance**: Required for financial transactions
- **Maturity**: Proven in production banking
- **JSONB Support**: Flexibility for semi-structured data
- **Ecosystem**: Excellent tooling (Flyway, pgAdmin, etc.)
- **Support**: Crunchy PostgreSQL for OpenShift

## Considered Options

### Option 1: PostgreSQL 16 with JSONB
- **Pros**:
  - Full ACID compliance
  - JSONB for flexible schemas
  - Excellent tooling
  - Crunchy Data for OpenShift
  - Proven in banking
- **Cons**:
  - Manual sharding for scale
- **Complexity**: Medium
- **Rationale**: Best all-around choice for banking

### Option 2: MySQL 8.0
- **Pros**:
  - ACID compliant
  - Popular and familiar
- **Cons**:
  - JSON support less mature than JSONB
  - Different tooling
  - Less advanced features
- **Complexity**: Medium
- **Rationale**: Good but PostgreSQL has better JSONB

### Option 3: MongoDB
- **Pros**:
  - Flexible schema
  - Good for unstructured data
- **Cons**:
  - No ACID transactions (4.0+ has limitations)
  - Not suitable for financial transactions
- **Complexity**: Low
- **Rationale**: Not suitable for core banking

### Option 4: Oracle Database
- **Pros**:
  - Enterprise features
  - Proven in banking
- **Cons**:
  - Expensive licensing
  - Complex to operate
  - Not cloud-native
- **Complexity**: High
- **Rationale**: Overkill and expensive for our use case

## Decision

**Choose PostgreSQL 16** for all services requiring persistence:
- Account data
- Transaction records
- Wallet ledger
- KYC documents (JSONB)
- Analytics data

**Use TimescaleDB** (PostgreSQL extension) for:
- Time-series analytics data

## Rationale

1. **ACID Compliance**: Essential for financial transactions
2. **JSONB**: Flexibility for KYC documents, analytics
3. **Tooling**: Flyway migrations, pgAdmin, etc.
4. **Crunchy Data**: Red Hat certified operator for OpenShift
5. **Open Source**: No licensing costs

## Consequences

**Positive**:
- ACID transactions guaranteed
- JSONB for flexible schemas
- Excellent tooling ecosystem
- Crunchy Data for OpenShift

**Negative**:
- Manual sharding for scale
- Not horizontally scalable like NoSQL

**Trade-offs Accepted**:
- Accept manual sharding for ACID compliance
- Accept vertical scaling for simplicity

## Implementation Notes

### Connection String

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/payu_{service}
spring.datasource.username=payu
spring.datasource.password=${DB_PASSWORD}
```

### Flyway Migrations

```bash
# Migration location
src/main/resources/db/migration/

# Naming convention
V1__create_accounts_table.sql
V2__create_transactions_table.sql
```

### JSONB Column Example

```sql
CREATE TABLE kyc_documents (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- JSONB query example
SELECT * FROM kyc_documents
WHERE document_data->>'nik' = '3201234567890001';
```

### Database per Service Pattern

Each service has its own database:
- `payu_account`
- `payu_transaction`
- `payu_wallet`
- `payu_kyc`
- etc.

---

*Created via @docs-engineer*
