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
- **Ecosystem**: Excellent tooling (Flyway, pgAdmin, Hibernate 6.x)
- **Deployment Portability**: CloudNativePG (CNPG) for in-cluster OpenShift (Dev/SIT/UAT) & AWS RDS Multi-AZ for managed Production

## Considered Options

### Option 1: PostgreSQL 16 (CNPG in-cluster for Staging + AWS RDS Multi-AZ for Production Target)
- **Pros**:
  - Full ACID compliance and PostgreSQL 16 feature parity across all environments.
  - JSONB for flexible KYC documents and audit logs.
  - In-Cluster (Dev/SIT/UAT): CNPG provides self-contained 3-instance HA + Barman Cloud S3 backups without extra cloud infrastructure cost.
  - Production: AWS RDS Multi-AZ / Aurora provides managed 99.99% SLA, hardware replication, KMS encryption at rest, and automated maintenance windows.
  - Zero application code/Flyway migration changes between staging and production.
- **Cons**:
  - Manual sharding required for extreme scale.
- **Complexity**: Medium
- **Rationale**: Best all-around choice for cloud-native banking with clear path to managed cloud production.

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

**Choose PostgreSQL 16 as the Standard Database Engine** for all services requiring persistence:

- Account data
- Transaction records
- Wallet ledger
- KYC documents (JSONB)
- Analytics data

**Deployment Topology**:
1. **Dev / SIT / UAT / Pre-Prod**: Orchestrated in-cluster via **CloudNativePG (CNPG 1.30+)** with 3 instances (1 Primary + 2 Standbys) and Barman Cloud continuous S3 WAL archiving.
2. **Production Target**: Deployed on **AWS RDS PostgreSQL (Multi-AZ)** or **Amazon Aurora PostgreSQL** with automated cross-AZ synchronous replication, KMS volume encryption, and continuous PITR snapshots.

**Use TimescaleDB** (PostgreSQL extension) for:

- Time-series analytics data

## Rationale

1. **ACID Compliance**: Essential for financial transactions and double-entry ledger.
2. **JSONB**: Flexibility for KYC documents and audit trails.
3. **Tooling**: 100% consistent Flyway migrations and JPA configurations across local, OpenShift, and AWS RDS.
4. **Cloud-Native & Managed Ready**: In-cluster CNPG provides robust local/staging HA, while AWS RDS eliminates operational overhead in production.
5. **No Vendor Lock-in**: Standard PostgreSQL wire protocol allows seamless migration between OpenShift in-cluster and managed AWS RDS.

## Consequences

**Positive**:

- ACID transactions guaranteed
- JSONB for flexible schemas
- Identical SQL dialect, Flyway migrations, and JPA mappings between OpenShift CNPG and AWS RDS
- OpenShift staging costs minimized via in-cluster CNPG
- Production operational burden minimized via AWS RDS Multi-AZ managed services

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

*Created via @principal-architect*
