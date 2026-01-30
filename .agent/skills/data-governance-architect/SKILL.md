---
name: data-governance-architect
version: 2.0.0
requires: [data-architect]
description: **Master Skill**: Data Governance & Lineage Architect. Covers Data Cataloging, Lineage Tracking, PII Classification, Retention Policies, and Regulatory Compliance (POJK, UU PDP).
---

# PayU Data Governance Architect Master Skill

You are the **Lead Data Governance Architect (AI)** for the **PayU Platform**. You ensure that all data assets are properly cataloged, classified, tracked, and compliant with Indonesian data protection regulations (UU PDP, POJK).

## 🎯 Core Objectives

- **Data Discoverability**: All data assets are cataloged and searchable
- **Lineage Transparency**: Full visibility into data flow from source to consumption
- **Regulatory Compliance**: Meet UU PDP (Undang-Undang Perlindungan Data Pribadi) requirements
- **Data Quality**: Ensure accuracy, completeness, and consistency of financial data
- **Access Governance**: Right data to right people with full auditability

---

## 📚 Data Classification Framework

### Sensitivity Levels

| Level | Classification | Examples | Handling Requirements |
|:------|:---------------|:---------|:---------------------|
| **L1 - Restricted** | Highly Sensitive PII | NIK, Biometric, PIN, Card PAN | Encrypted at rest + transit, masked in logs, access logging, 2FA for access |
| **L2 - Confidential** | Sensitive PII | Full Name, DOB, Address, Phone | Encrypted at rest, masked in non-prod, role-based access |
| **L3 - Internal** | Business Sensitive | Transaction amounts, Account balances | Encrypted in transit, internal access only |
| **L4 - Public** | Non-sensitive | Product names, Public rates | Standard security controls |

### Data Domain Ownership

| Domain | Owner | Key Data Assets | Steward |
|:-------|:------|:----------------|:--------|
| **Customer** | Account Service | User profiles, KYC data | Product Team |
| **Transaction** | Transaction Service | Transfers, Payments, Ledger | Finance Team |
| **Risk** | Compliance Service | AML scores, Fraud signals | Risk Team |
| **Product** | CMS Service | Rates, Fees, Product configs | Business Team |
| **Analytics** | Analytics Service | Aggregated metrics, Reports | Data Team |

---

## 🔄 Data Lineage Architecture

### Lineage Tracking Components

```
┌─────────────────────────────────────────────────────────────┐
│                    DATA LINEAGE FLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌────────┐│
│  │  Source  │───►│  Kafka   │───►│  Target  │───►│ Report ││
│  │  (OLTP)  │    │  Topic   │    │  (OLAP)  │    │  Layer ││
│  └──────────┘    └──────────┘    └──────────┘    └────────┘│
│       │               │               │               │     │
│       ▼               ▼               ▼               ▼     │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              OpenLineage / DataHub                       ││
│  │  • Source metadata    • Transformation logic             ││
│  │  • Schema evolution   • Quality metrics                  ││
│  │  • Access patterns    • Compliance tags                  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### OpenLineage Integration

```java
// Emit lineage events from services
@Component
public class LineageEmitter {
    
    private final OpenLineageClient client;
    
    public void emitTransformationLineage(
            String jobName,
            List<Dataset> inputs,
            List<Dataset> outputs) {
        
        RunEvent event = RunEvent.builder()
            .eventType(RunEvent.EventType.COMPLETE)
            .eventTime(ZonedDateTime.now())
            .run(Run.builder()
                .runId(UUID.randomUUID())
                .build())
            .job(Job.builder()
                .namespace("payu")
                .name(jobName)
                .build())
            .inputs(inputs)
            .outputs(outputs)
            .build();
            
        client.emit(event);
    }
}

// Example: Transaction aggregation job
lineageEmitter.emitTransformationLineage(
    "daily-transaction-summary",
    List.of(
        Dataset.builder()
            .namespace("payu")
            .name("transactions.ledger")
            .facets(DatasetFacets.builder()
                .schema(schemaFacet)
                .dataQuality(qualityFacet)
                .build())
            .build()
    ),
    List.of(
        Dataset.builder()
            .namespace("payu")
            .name("analytics.daily_summary")
            .build()
    )
);
```

### Column-Level Lineage

```yaml
# DataHub lineage configuration
lineage:
  source:
    table: wallet.ledger
    columns:
      - amount
      - account_id
      - created_at
  transformations:
    - type: aggregation
      operation: SUM(amount) GROUP BY account_id, DATE(created_at)
  target:
    table: analytics.daily_balances
    columns:
      - total_amount
      - account_id
      - balance_date
```

---

## 📋 Data Catalog Structure

### Catalog Entry Schema

```json
{
  "asset_id": "payu.wallet.ledger",
  "name": "Wallet Ledger",
  "description": "Double-entry accounting ledger for all wallet movements",
  "domain": "Transaction",
  "owner": "wallet-service-team",
  "steward": "finance-ops@payu.id",
  "classification": "L3-Internal",
  "pii_fields": ["account_id"],
  "schema": {
    "columns": [
      {"name": "id", "type": "UUID", "description": "Primary key"},
      {"name": "account_id", "type": "UUID", "pii": true, "description": "Reference to account"},
      {"name": "amount", "type": "DECIMAL(19,4)", "description": "Transaction amount in IDR"},
      {"name": "entry_type", "type": "ENUM", "values": ["DEBIT", "CREDIT"]},
      {"name": "created_at", "type": "TIMESTAMPTZ", "description": "Entry timestamp"}
    ]
  },
  "freshness": {
    "expected": "real-time",
    "sla_minutes": 1
  },
  "quality_rules": [
    {"rule": "amount != 0", "severity": "error"},
    {"rule": "SUM(amount) = 0 GROUP BY transaction_id", "severity": "critical"}
  ],
  "retention": {
    "policy": "7-years",
    "regulation": "POJK 12/2017"
  },
  "tags": ["financial", "audit-required", "gdpr-relevant"]
}
```

---

## 🛡️ Privacy & Compliance (UU PDP)

### Data Subject Rights Implementation

| Right | Implementation | Service |
|:------|:---------------|:--------|
| **Right to Access** | Export all user data in machine-readable format | Account Service `/api/v1/users/{id}/data-export` |
| **Right to Rectification** | Allow profile updates with audit trail | Account Service `/api/v1/users/{id}/profile` |
| **Right to Erasure** | Anonymize PII, retain financial records | Compliance Service `/api/v1/gdpr/erasure` |
| **Right to Portability** | Export in standard format (JSON/CSV) | Account Service `/api/v1/users/{id}/portability` |
| **Right to Object** | Opt-out of marketing, analytics | Consent Service `/api/v1/users/{id}/consent` |

### Anonymization Patterns

```java
@Service
public class AnonymizationService {
    
    /**
     * Anonymize user data for GDPR erasure request
     * Retains financial records for regulatory compliance
     */
    @Transactional
    public void anonymizeUser(UUID userId) {
        // 1. Anonymize PII in account table
        accountRepository.anonymize(userId, AnonymizationConfig.builder()
            .field("full_name", "ANONYMIZED_USER_" + hash(userId))
            .field("email", hash(userId) + "@anonymized.local")
            .field("phone", "0000000000")
            .field("nik", "0000000000000000")
            .field("address", "ANONYMIZED")
            .build());
        
        // 2. Retain transaction records (required by POJK)
        // Only remove linking identifiers from non-essential fields
        transactionRepository.updateDescription(userId, "ANONYMIZED");
        
        // 3. Revoke all access tokens
        authService.revokeAllTokens(userId);
        
        // 4. Emit compliance event
        eventPublisher.publish(new DataErasureCompletedEvent(userId, Instant.now()));
        
        // 5. Audit log
        auditLogger.log(AuditEvent.builder()
            .action("GDPR_ERASURE")
            .subject(userId)
            .timestamp(Instant.now())
            .build());
    }
}
```

### Consent Management

```yaml
# Consent categories
consent:
  categories:
    - id: essential
      name: Essential Services
      required: true
      description: Required for basic banking functionality
      
    - id: marketing
      name: Marketing Communications
      required: false
      default: false
      description: Promotional emails, push notifications
      
    - id: analytics
      name: Analytics & Personalization
      required: false
      default: true
      description: Usage analytics for service improvement
      
    - id: third_party
      name: Partner Data Sharing
      required: false
      default: false
      description: Sharing data with partners for offers
```

---

## 📊 Data Quality Framework

### Quality Dimensions

| Dimension | Definition | Metric | Target |
|:----------|:-----------|:-------|:-------|
| **Completeness** | All required fields populated | % non-null | > 99.9% |
| **Accuracy** | Data matches real-world values | Validation pass rate | > 99.5% |
| **Consistency** | Same data across systems | Cross-system match rate | 100% |
| **Timeliness** | Data available when needed | Freshness lag | < SLA |
| **Uniqueness** | No unwanted duplicates | Duplicate rate | < 0.01% |
| **Validity** | Data conforms to rules | Schema validation | 100% |

### Quality Rules Engine

```python
# Data quality checks using Great Expectations
from great_expectations.core import ExpectationSuite

def create_transaction_quality_suite() -> ExpectationSuite:
    suite = ExpectationSuite(expectation_suite_name="transactions")
    
    # Completeness checks
    suite.add_expectation(
        expectation_type="expect_column_values_to_not_be_null",
        kwargs={"column": "transaction_id"}
    )
    
    # Validity checks
    suite.add_expectation(
        expectation_type="expect_column_values_to_be_in_set",
        kwargs={"column": "status", "value_set": ["PENDING", "COMPLETED", "FAILED", "REVERSED"]}
    )
    
    # Accuracy checks
    suite.add_expectation(
        expectation_type="expect_column_values_to_be_between",
        kwargs={"column": "amount", "min_value": 1, "max_value": 1_000_000_000}
    )
    
    # Consistency checks (double-entry must balance)
    suite.add_expectation(
        expectation_type="expect_column_pair_values_to_be_equal",
        kwargs={
            "column_A": "debit_total",
            "column_B": "credit_total",
            "mostly": 1.0  # 100% must match
        }
    )
    
    return suite
```

---

## 📜 Retention Policies

### Retention Matrix

| Data Category | Retention Period | Regulation | Archive Location | Deletion Method |
|:--------------|:-----------------|:-----------|:-----------------|:----------------|
| **Transaction Records** | 7 years | POJK 12/2017 | Cold Storage (S3 Glacier) | Secure deletion after period |
| **Audit Logs** | 10 years | PCI-DSS | Immutable storage | Automated lifecycle |
| **Customer PII** | Active + 5 years | UU PDP | Encrypted archive | Anonymization then deletion |
| **Session Logs** | 90 days | Security policy | Hot storage | Auto-purge |
| **Analytics Data** | 3 years | Internal | Data warehouse | Aggregation then deletion |

### Automated Retention Enforcement

```sql
-- PostgreSQL partitioned table with auto-drop
CREATE TABLE transactions (
    id UUID,
    amount DECIMAL(19,4),
    created_at TIMESTAMPTZ
) PARTITION BY RANGE (created_at);

-- Create monthly partitions
CREATE TABLE transactions_2026_01 PARTITION OF transactions
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- Automated cleanup job (runs monthly)
-- DROP partitions older than 7 years
DO $$
DECLARE
    partition_name TEXT;
BEGIN
    FOR partition_name IN
        SELECT tablename FROM pg_tables
        WHERE tablename LIKE 'transactions_%'
        AND tablename < 'transactions_' || to_char(NOW() - INTERVAL '7 years', 'YYYY_MM')
    LOOP
        EXECUTE 'DROP TABLE ' || partition_name;
        RAISE NOTICE 'Dropped partition: %', partition_name;
    END LOOP;
END $$;
```

---

## 🔍 Data Governance Checklist

- [ ] **Catalog**: Are all critical data assets cataloged with ownership?
- [ ] **Classification**: Is every table/column classified by sensitivity level?
- [ ] **Lineage**: Can you trace data from source to all downstream consumers?
- [ ] **PII**: Are all PII fields identified and properly protected?
- [ ] **Consent**: Is user consent tracked and enforced for each data use?
- [ ] **Quality**: Are automated quality checks running for critical datasets?
- [ ] **Retention**: Are retention policies defined and auto-enforced?
- [ ] **Access**: Is there role-based access with full audit logging?
- [ ] **Compliance**: Can you generate compliance reports for OJK/BI audits?

---
*Last Updated: January 2026*

```
