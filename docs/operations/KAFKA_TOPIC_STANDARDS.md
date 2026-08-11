# Kafka Topic Naming Convention & Schema Registry Standards

> **Version**: 1.0.0
> **Last Updated**: January 2026
> **Owner**: Platform Engineering Team
> **Status**: Mandatory

---

## Table of Contents

1. [Topic Naming Convention](#1-topic-naming-convention)
2. [Topic Configuration Standards](#2-topic-configuration-standards)
3. [Schema Registry Guidelines](#3-schema-registry-guidelines)
4. [Consumer Group Naming](#4-consumer-group-naming)
5. [Event Type Catalog](#5-event-type-catalog)
6. [Implementation Examples](#6-implementation-examples)
7. [Operational Guidelines](#7-operational-guidelines)

---

## 1. Topic Naming Convention

### 1.1 Standard Format

```
payu.<domain>.<event-type>.<version>
```

| Component | Description | Rules |
|-----------|-------------|-------|
| `payu` | Organization prefix | Always lowercase, static |
| `<domain>` | Business domain / service | lowercase, hyphen-separated |
| `<event-type>` | Event classification | lowercase, dot-separated action |
| `<version>` | Schema version (v1, v2, etc) | v{N} format, starts at v1 |

### 1.2 Domain Mapping

| Service | Domain Name | Description |
|---------|-------------|-------------|
| Account Service | `account` | User registration, profile, KYC |
| Wallet Service | `wallet` | Balance, reservations, ledger |
| Transaction Service | `transaction` | Transfers, BI-FAST, QRIS |
| Lending Service | `lending` | Loans, PayLater, credit scoring |
| Investment Service | `investment` | Mutual funds, gold, robo-advisory |
| Billing Service | `billing` | Bill payments (PLN, PDAM) |
| Notification Service | `notification` | Push, SMS, email, WhatsApp |
| Promotion Service | `promotion` | Vouchers, campaigns, rewards |
| Compliance Service | `compliance` | AML, fraud, regulatory |
| Partner Service | `partner` | External integrations |
| CMS Service | `cms` | Content management |
| Analytics Service | `analytics` | Events for data warehouse |

### 1.3 Topic Examples by Service

#### Wallet Service

| Topic Name | Description | Event Type |
|------------|-------------|------------|
| `payu.wallet.balance.changed.v1` | Balance update events | Domain Event |
| `payu.wallet.balance.reserved.v1` | Balance reservation | Domain Event |
| `payu.wallet.reservation.committed.v1` | Reservation commit | Domain Event |
| `payu.wallet.reservation.released.v1` | Reservation release | Domain Event |
| `payu.wallet.created.v1` | New wallet creation | Domain Event |
| `payu.wallet.ledger.entry.v1` | Ledger entry audit | Audit Event |

#### Transaction Service

| Topic Name | Description | Event Type |
|------------|-------------|------------|
| `payu.transaction.initiated.v1` | Transaction started | Domain Event |
| `payu.transaction.validated.v1` | Transaction validated | Domain Event |
| `payu.transaction.completed.v1` | Transaction successful | Domain Event |
| `payu.transaction.failed.v1` | Transaction failure | Domain Event |
| `payu.transaction.reversed.v1` | Transaction reversal | Domain Event |
| `payu.transaction.bi-fast.sent.v1` | BI-FAST outgoing | Integration Event |
| `payu.transaction.qris.generated.v1` | QRIS generation | Integration Event |

#### Account Service

| Topic Name | Description | Event Type |
|------------|-------------|------------|
| `payu.account.created.v1` | New user registration | Domain Event |
| `payu.account.updated.v1` | Profile changes | Domain Event |
| `payu.account.activated.v1` | Account activation | Domain Event |
| `payu.account.suspended.v1` | Account suspension | Domain Event |
| `payu.account.kyc.submitted.v1` | KYC submission | Domain Event |
| `payu.account.kyc.verified.v1` | KYC approval | Domain Event |
| `payu.account.kyc.rejected.v1` | KYC rejection | Domain Event |

#### Lending Service

| Topic Name | Description | Event Type |
|------------|-------------|------------|
| `payu.lending.loan.applied.v1` | Loan application | Domain Event |
| `payu.lending.loan.approved.v1` | Loan approval | Domain Event |
| `payu.lending.loan.rejected.v1` | Loan rejection | Domain Event |
| `payu.lending.loan.disbursed.v1` | Fund disbursement | Domain Event |
| `payu.lending.loan.repaid.v1` | Loan repayment | Domain Event |
| `payu.lending.paylater.activated.v1` | PayLater activation | Domain Event |

#### Investment Service

| Topic Name | Description | Event Type |
|------------|-------------|------------|
| `payu.investment.created.v1` | Investment order | Domain Event |
| `payu.investment.completed.v1` | Investment execution | Domain Event |
| `payu.investment.redeemed.v1` | Redemption request | Domain Event |
| `payu.investment.failed.v1` | Investment failure | Domain Event |
| `payu.investment.price.updated.v1` | NAV price update | Domain Event |

### 1.4 Dead Letter Queue (DLQ) Topics

Format:
```
payu.<domain>.<event-type>.<version>.dlq
```

Examples:
- `payu.transaction.completed.v1.dlq`
- `payu.wallet.balance.changed.v1.dlq`
- `payu.account.kyc.verified.v1.dlq`

**DLQ Naming Rules:**
- Always append `.dlq` suffix to original topic
- Same partition count as source topic
- Retention: 14 days (double the standard retention)
- Used for poison pill messages and processing failures

### 1.5 Special Topic Categories

#### Audit Topics
```
payu.audit.<domain>.<action>.v1
```
Examples:
- `payu.audit.transaction.all.v1` - All transaction events for audit
- `payu.audit.account.access.v1` - Account access logs
- `payu.audit.compliance.suspicious.v1` - AML flagged events

#### Command Topics (Saga Pattern)
```
payu.saga.<saga-name>.<command>.v1
```
Examples:
- `payu.saga.transfer.execute.v1`
- `payu.saga.transfer.compensate.v1`
- `payu.saga.loan.disburse.execute.v1`

#### State Store Topics (Compacted)
```
payu.state.<domain>.<entity>.v1
```
Examples:
- `payu.state.wallet.balance.v1`
- `payu.state.account.profile.v1`
- `payu.state.lending.credit-limit.v1`

---

## 2. Topic Configuration Standards

### 2.1 Partition Formula

```
partitions = max(3, ceil(expected_throughput_tps / 1000))
```

| Throughput (TPS) | Partitions | Use Case |
|------------------|------------|----------|
| < 1,000 | 3 | Low volume (CMS, notifications) |
| 1,000 - 5,000 | 6 | Medium volume (lending, investment) |
| 5,000 - 10,000 | 12 | High volume (wallet, account) |
| 10,000 - 50,000 | 24 | Very high volume (transactions) |
| > 50,000 | 48+ | Extreme volume (BI-FAST, QRIS) |

**Partitioning Key Strategy:**
- Use `aggregateId` (accountId, transactionId, walletId) for ordering guarantee
- Ensure even distribution to avoid hot partitions
- Key should be string format for consistency

### 2.2 Replication Configuration

| Environment | Replication Factor | Min ISR | Notes |
|-------------|-------------------|---------|-------|
| Development | 1 | 1 | Single broker acceptable |
| Staging | 2 | 1 | Cost optimization |
| Production | 3 | 2 | HA requirement |
| Critical | 3 | 3 | No data loss (sync replication) |

### 2.3 Standard Topic Configuration

```yaml
# Standard Event Topic
standard.event.config:
  partitions: 6
  replication.factor: 3
  min.insync.replicas: 2
  retention.ms: 604800000        # 7 days
  retention.bytes: -1            # No size limit
  compression.type: lz4
  cleanup.policy: delete
  segment.ms: 604800000          # 7 day segments
  segment.bytes: 1073741824      # 1GB segments
  message.max.bytes: 1048576     # 1MB max message

# Compacted State Topic
compacted.state.config:
  partitions: 3
  replication.factor: 3
  min.insync.replicas: 2
  retention.ms: -1               # Infinite (compact only)
  cleanup.policy: compact
  min.cleanable.dirty.ratio: 0.5
  delete.retention.ms: 86400000  # 1 day tombstone retention
  segment.ms: 86400000           # 1 day segments
  compression.type: lz4

# Audit Topic (Long Retention)
audit.topic.config:
  partitions: 12
  replication.factor: 3
  min.insync.replicas: 2
  retention.ms: 2592000000       # 30 days
  retention.bytes: 107374182400  # 100GB
  compression.type: lz4
  cleanup.policy: delete

# DLQ Topic
 dlq.topic.config:
  partitions: 3                  # Match source topic
  replication.factor: 3
  min.insync.replicas: 2
  retention.ms: 1209600000       # 14 days
  compression.type: lz4
  cleanup.policy: delete
```

### 2.4 Configuration by Event Type

| Event Type | Partitions | Retention | Cleanup Policy | Compression |
|------------|------------|-----------|----------------|-------------|
| Domain Events | 6-24 | 7 days | delete | lz4 |
| Audit Events | 12 | 30 days | delete | lz4 |
| State Events | 3 | Infinite | compact | lz4 |
| Command Events | 6 | 1 day | delete | lz4 |
| DLQ | 3 | 14 days | delete | lz4 |
| Analytics | 12 | 7 days | delete | snappy |

---

## 3. Schema Registry Guidelines

### 3.1 Schema Format

PayU uses **Avro** as the primary schema format with Confluent Schema Registry.

### 3.2 Avro Schema Structure

```json
{
  "type": "record",
  "name": "TransactionCompletedEvent",
  "namespace": "com.payu.transaction.events",
  "doc": "Event published when a transaction is successfully completed",
  "version": "1",
  "fields": [
    {
      "name": "metadata",
      "type": {
        "type": "record",
        "name": "EventMetadata",
        "fields": [
          {"name": "eventId", "type": "string", "doc": "Unique event UUID"},
          {"name": "eventType", "type": "string", "doc": "Fully qualified event type"},
          {"name": "eventVersion", "type": "string", "default": "1.0"},
          {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
          {"name": "source", "type": "string", "doc": "Service that emitted the event"},
          {"name": "correlationId", "type": "string", "doc": "Request correlation ID"},
          {"name": "traceId", "type": ["null", "string"], "default": null, "doc": "Distributed trace ID"}
        ]
      }
    },
    {
      "name": "payload",
      "type": {
        "type": "record",
        "name": "TransactionPayload",
        "fields": [
          {"name": "transactionId", "type": "string"},
          {"name": "referenceNumber", "type": "string"},
          {"name": "senderAccountId", "type": "string"},
          {"name": "recipientAccountId", "type": ["null", "string"], "default": null},
          {"name": "amount", "type": {
            "type": "record",
            "name": "Money",
            "fields": [
              {"name": "amount", "type": "string", "doc": "Decimal as string to preserve precision"},
              {"name": "currency", "type": "string", "default": "IDR"}
            ]
          }},
          {"name": "type", "type": {"type": "enum", "name": "TransactionType", "symbols": ["TRANSFER", "PAYMENT", "WITHDRAWAL", "DEPOSIT"]}},
          {"name": "status", "type": {"type": "enum", "name": "TransactionStatus", "symbols": ["PENDING", "COMPLETED", "FAILED", "REVERSED"]}},
          {"name": "completedAt", "type": "long", "logicalType": "timestamp-millis"},
          {"name": "metadata", "type": ["null", {"type": "map", "values": "string"}], "default": null}
        ]
      }
    }
  ]
}
```

### 3.3 Schema Naming Convention

```
<namespace>.<EventName>
```

| Service | Namespace | Example |
|---------|-----------|---------|
| Wallet | `com.payu.wallet.events` | `com.payu.wallet.events.BalanceChangedEvent` |
| Transaction | `com.payu.transaction.events` | `com.payu.transaction.events.TransactionCompletedEvent` |
| Account | `com.payu.account.events` | `com.payu.account.events.AccountCreatedEvent` |
| Lending | `com.payu.lending.events` | `com.payu.lending.events.LoanApprovedEvent` |
| Investment | `com.payu.investment.events` | `com.payu.investment.events.InvestmentCreatedEvent` |

### 3.4 Compatibility Mode

**Default: `BACKWARD_TRANSITIVE`**

```bash
# Set compatibility for a subject
curl -X PUT http://schema-registry:8081/config/payu.transaction.completed.v1-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD_TRANSITIVE"}'
```

| Compatibility | When to Use | Rules |
|---------------|-------------|-------|
| `BACKWARD` | Default | New schema can read old data. Can add optional fields, delete fields. |
| `BACKWARD_TRANSITIVE` | Financial events | Backward compatibility with ALL previous versions. **Recommended for PayU.** |
| `FORWARD` | Consumer flexibility | Old schema can read new data. Can add fields, delete optional fields. |
| `FORWARD_TRANSITIVE` | Migration periods | Forward compatible with ALL future versions. |
| `FULL` | Simple events | Both backward and forward. Can only add optional fields. |
| `FULL_TRANSITIVE` | Strict governance | Full compatibility with ALL versions. |
| `NONE` | Development only | No compatibility checks. Not for production. |

### 3.5 Schema Evolution Rules

#### Allowed Changes (Backward Compatible)

1. **Add optional fields** with default values
   ```json
   {"name": "newField", "type": ["null", "string"], "default": null}
   ```

2. **Change field from required to optional** by adding null union
   ```json
   // Before: {"name": "field", "type": "string"}
   // After:  {"name": "field", "type": ["null", "string"], "default": null}
   ```

3. **Promote types** (int -> long, float -> double)

4. **Add new enum symbols** (if consumers handle unknown values)

#### Prohibited Changes (Breaking)

1. **Remove required fields**
2. **Change field type** (except promotions)
3. **Rename fields** (treat as remove + add)
4. **Change field order** without explicit field aliases
5. **Remove enum symbols**

### 3.6 Required Fields for Financial Events

Every financial event schema MUST include:

```json
{
  "fields": [
    // Event Identity
    {"name": "eventId", "type": "string", "doc": "UUID v4"},
    {"name": "eventType", "type": "string", "doc": "FQDN event type"},
    {"name": "eventVersion", "type": "string", "default": "1.0"},

    // Timing
    {"name": "timestamp", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "timezone", "type": "string", "default": "Asia/Jakarta"},

    // Traceability
    {"name": "correlationId", "type": "string", "doc": "Request correlation ID"},
    {"name": "traceId", "type": ["null", "string"], "default": null},
    {"name": "source", "type": "string", "doc": "Source service name"},

    // Financial Context
    {"name": "accountId", "type": "string", "doc": "Primary account identifier"},
    {"name": "amount", "type": "string", "doc": "Decimal amount as string"},
    {"name": "currency", "type": "string", "default": "IDR"},
    {"name": "referenceNumber", "type": "string", "doc": "Business reference"},

    // Idempotency
    {"name": "idempotencyKey", "type": ["null", "string"], "default": null}
  ]
}
```

### 3.7 Schema Registration Workflow

```
1. Define schema in service: src/main/avro/<EventName>.avsc
2. Run Maven plugin to generate Java classes
3. Register schema to dev registry: mvn schema-registry:register
4. Test backward compatibility
5. Promote to staging, then production
6. Update topic documentation
```

---

## 4. Consumer Group Naming

### 4.1 Standard Format

```
<service-name>-<purpose>-group
```

| Component | Description | Example |
|-----------|-------------|---------|
| `<service-name>` | Service identifier | `wallet-service`, `analytics-service` |
| `<purpose>` | Consumer purpose | `processor`, `notifier`, `indexer` |
| `group` | Suffix | Always `group` |

### 4.2 Consumer Group Examples

| Service | Consumer Group | Subscribed Topics |
|---------|----------------|-------------------|
| Wallet Service | `wallet-service-processor-group` | `payu.transaction.completed.v1` |
| Analytics Service | `analytics-service-indexer-group` | `payu.>.v1` (all topics) |
| Notification Service | `notification-service-notifier-group` | `payu.transaction.completed.v1`, `payu.wallet.balance.changed.v1` |
| Compliance Service | `compliance-service-aml-group` | `payu.transaction.>.v1` |
| Lending Service | `lending-service-scoring-group` | `payu.account.kyc.verified.v1` |
| Investment Service | `investment-service-settlement-group` | `payu.wallet.balance.changed.v1` |

### 4.3 Consumer Group Rules

1. **One group per service-purpose combination**
2. **Never share consumer groups across services**
3. **Use separate groups for different processing logic in same service**
4. **Group ID must be unique across the cluster**
5. **Include environment suffix for shared clusters**:
   - `wallet-service-processor-group-dev`
   - `wallet-service-processor-group-prod`

---

## 5. Event Type Catalog

### 5.1 Event Type Format

```
com.payu.<domain>.<action>.<version>
```

### 5.2 Complete Event Catalog

#### Account Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.account.created.v1` | `payu.account.created.v1` | User registration complete |
| `com.payu.account.updated.v1` | `payu.account.updated.v1` | Profile information changed |
| `com.payu.account.activated.v1` | `payu.account.activated.v1` | Account activated |
| `com.payu.account.suspended.v1` | `payu.account.suspended.v1` | Account suspended |
| `com.payu.account.closed.v1` | `payu.account.closed.v1` | Account closed |
| `com.payu.account.kyc.submitted.v1` | `payu.account.kyc.submitted.v1` | KYC documents submitted |
| `com.payu.account.kyc.verified.v1` | `payu.account.kyc.verified.v1` | KYC approved |
| `com.payu.account.kyc.rejected.v1` | `payu.account.kyc.rejected.v1` | KYC rejected |
| `com.payu.account.pin.changed.v1` | `payu.account.pin.changed.v1` | PIN changed |
| `com.payu.account.device.linked.v1` | `payu.account.device.linked.v1` | New device authorized |

#### Wallet Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.wallet.created.v1` | `payu.wallet.created.v1` | Wallet provisioned |
| `com.payu.wallet.balance.credited.v1` | `payu.wallet.balance.credited.v1` | Balance increased |
| `com.payu.wallet.balance.debited.v1` | `payu.wallet.balance.debited.v1` | Balance decreased |
| `com.payu.wallet.balance.reserved.v1` | `payu.wallet.balance.reserved.v1` | Balance reserved |
| `com.payu.wallet.reservation.committed.v1` | `payu.wallet.reservation.committed.v1` | Reservation committed |
| `com.payu.wallet.reservation.released.v1` | `payu.wallet.reservation.released.v1` | Reservation released |
| `com.payu.wallet.ledger.entry.created.v1` | `payu.wallet.ledger.entry.created.v1` | Ledger entry recorded |
| `com.payu.wallet.limit.updated.v1` | `payu.wallet.limit.updated.v1` | Transaction limit changed |

#### Transaction Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.transaction.initiated.v1` | `payu.transaction.initiated.v1` | Transaction started |
| `com.payu.transaction.validated.v1` | `payu.transaction.validated.v1` | Validation passed |
| `com.payu.transaction.completed.v1` | `payu.transaction.completed.v1` | Transaction successful |
| `com.payu.transaction.failed.v1` | `payu.transaction.failed.v1` | Transaction failed |
| `com.payu.transaction.reversed.v1` | `payu.transaction.reversed.v1` | Reversal processed |
| `com.payu.transaction.bi-fast.sent.v1` | `payu.transaction.bi-fast.sent.v1` | BI-FAST message sent |
| `com.payu.transaction.bi-fast.received.v1` | `payu.transaction.bi-fast.received.v1` | BI-FAST message received |
| `com.payu.transaction.qris.generated.v1` | `payu.transaction.qris.generated.v1` | QRIS code generated |
| `com.payu.transaction.qris.paid.v1` | `payu.transaction.qris.paid.v1` | QRIS payment received |

#### Lending Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.lending.loan.applied.v1` | `payu.lending.loan.applied.v1` | Loan application submitted |
| `com.payu.lending.loan.approved.v1` | `payu.lending.loan.approved.v1` | Loan approved |
| `com.payu.lending.loan.rejected.v1` | `payu.lending.loan.rejected.v1` | Loan rejected |
| `com.payu.lending.loan.disbursed.v1` | `payu.lending.loan.disbursed.v1` | Funds disbursed |
| `com.payu.lending.loan.repaid.v1` | `payu.lending.loan.repaid.v1` | Repayment received |
| `com.payu.lending.loan.defaulted.v1` | `payu.lending.loan.defaulted.v1` | Loan defaulted |
| `com.payu.lending.paylater.activated.v1` | `payu.lending.paylater.activated.v1` | PayLater activated |
| `com.payu.lending.paylater.used.v1` | `payu.lending.paylater.used.v1` | PayLater used |
| `com.payu.lending.credit.score.updated.v1` | `payu.lending.credit.score.updated.v1` | Credit score changed |

#### Investment Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.investment.order.created.v1` | `payu.investment.order.created.v1` | Buy order placed |
| `com.payu.investment.order.executed.v1` | `payu.investment.order.executed.v1` | Order executed |
| `com.payu.investment.order.failed.v1` | `payu.investment.order.failed.v1` | Order failed |
| `com.payu.investment.redeem.created.v1` | `payu.investment.redeem.created.v1` | Redemption requested |
| `com.payu.investment.redeem.executed.v1` | `payu.investment.redeem.executed.v1` | Redemption completed |
| `com.payu.investment.nav.updated.v1` | `payu.investment.nav.updated.v1` | NAV price updated |
| `com.payu.investment.portfolio.rebalanced.v1` | `payu.investment.portfolio.rebalanced.v1` | Robo-advisory rebalanced |

#### Billing Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.billing.inquiry.completed.v1` | `payu.billing.inquiry.completed.v1` | Bill inquiry done |
| `com.payu.billing.payment.completed.v1` | `payu.billing.payment.completed.v1` | Bill payment successful |
| `com.payu.billing.payment.failed.v1` | `payu.billing.payment.failed.v1` | Bill payment failed |

#### Compliance Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.compliance.suspicious.activity.v1` | `payu.compliance.suspicious.activity.v1` | AML alert triggered |
| `com.payu.compliance.transaction.blocked.v1` | `payu.compliance.transaction.blocked.v1` | Transaction blocked |
| `com.payu.compliance.fraud.detected.v1` | `payu.compliance.fraud.detected.v1` | Fraud pattern detected |

#### Notification Domain

| Event Type | Topic | Description |
|------------|-------|-------------|
| `com.payu.notification.sent.v1` | `payu.notification.sent.v1` | Notification delivered |
| `com.payu.notification.failed.v1` | `payu.notification.failed.v1` | Notification failed |

---

## 6. Implementation Examples

### 6.1 Spring Boot Producer

```java
@Component
@RequiredArgsConstructor
public class TransactionEventPublisherAdapter implements TransactionEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "payu.transaction.completed.v1";

    @Override
    public void publishTransactionCompleted(Transaction transaction) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
            .metadata(EventMetadata.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("com.payu.transaction.completed.v1")
                .eventVersion("1.0")
                .timestamp(Instant.now().toEpochMilli())
                .source("transaction-service")
                .correlationId(MDC.get("correlationId"))
                .traceId(MDC.get("traceId"))
                .build())
            .payload(TransactionPayload.builder()
                .transactionId(transaction.getId().toString())
                .referenceNumber(transaction.getReferenceNumber())
                .senderAccountId(transaction.getSenderAccountId().toString())
                .amount(Money.of(transaction.getAmount()))
                .status(TransactionStatus.COMPLETED)
                .completedAt(Instant.now().toEpochMilli())
                .build())
            .build();

        kafkaTemplate.send(TOPIC, transaction.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to {}", TOPIC, ex);
                } else {
                    log.info("Published event to {} partition {}",
                        TOPIC, result.getRecordMetadata().partition());
                }
            });
    }
}
```

### 6.2 Spring Boot Consumer

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final AnalyticsService analyticsService;

    @KafkaListener(
        topics = "payu.transaction.completed.v1",
        groupId = "analytics-service-indexer-group",
        containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void handleTransactionCompleted(
            @Payload TransactionCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received transaction completed event: {} from partition {} offset {}",
            event.getMetadata().getEventId(), partition, offset);

        try {
            analyticsService.indexTransaction(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getMetadata().getEventId(), e);
            throw new EventProcessingException("Processing failed", e);
        }
    }
}
```

### 6.3 Python (FastAPI) Producer

```python
from aiokafka import AIOKafkaProducer
import json
from datetime import datetime, timezone

class TransactionEventPublisher:
    def __init__(self, bootstrap_servers: str):
        self.producer = AIOKafkaProducer(
            bootstrap_servers=bootstrap_servers,
            value_serializer=lambda v: json.dumps(v, default=str).encode('utf-8'),
            key_serializer=lambda k: k.encode('utf-8') if k else None
        )

    async def publish_transaction_completed(self, transaction: Transaction):
        event = {
            "metadata": {
                "eventId": str(uuid.uuid4()),
                "eventType": "com.payu.transaction.completed.v1",
                "eventVersion": "1.0",
                "timestamp": int(datetime.now(timezone.utc).timestamp() * 1000),
                "source": "kyc-service",
                "correlationId": get_correlation_id(),
                "traceId": get_trace_id()
            },
            "payload": {
                "transactionId": str(transaction.id),
                "referenceNumber": transaction.reference_number,
                "senderAccountId": str(transaction.sender_account_id),
                "amount": {
                    "amount": str(transaction.amount),
                    "currency": transaction.currency
                },
                "status": "COMPLETED",
                "completedAt": int(datetime.now(timezone.utc).timestamp() * 1000)
            }
        }

        await self.producer.send(
            topic="payu.transaction.completed.v1",
            key=str(transaction.id),
            value=event
        )
```

### 6.4 Topic Creation Script

```bash
#!/bin/bash
# create-topics.sh - Idempotent topic creation script

KAFKA_BROKER=${KAFKA_BROKER:-localhost:9092}

# Standard event topic
create_event_topic() {
    local topic=$1
    local partitions=${2:-6}

    kafka-topics.sh --bootstrap-server $KAFKA_BROKER \
        --create --if-not-exists \
        --topic $topic \
        --partitions $partitions \
        --replication-factor 3 \
        --config min.insync.replicas=2 \
        --config retention.ms=604800000 \
        --config compression.type=lz4 \
        --config cleanup.policy=delete
}

# Compacted state topic
create_state_topic() {
    local topic=$1

    kafka-topics.sh --bootstrap-server $KAFKA_BROKER \
        --create --if-not-exists \
        --topic $topic \
        --partitions 3 \
        --replication-factor 3 \
        --config min.insync.replicas=2 \
        --config cleanup.policy=compact \
        --config compression.type=lz4 \
        --config min.cleanable.dirty.ratio=0.5
}

# DLQ topic
create_dlq_topic() {
    local source_topic=$1
    local partitions=${2:-3}

    kafka-topics.sh --bootstrap-server $KAFKA_BROKER \
        --create --if-not-exists \
        --topic "${source_topic}.dlq" \
        --partitions $partitions \
        --replication-factor 3 \
        --config min.insync.replicas=2 \
        --config retention.ms=1209600000 \
        --config compression.type=lz4
}

# Create topics for Transaction Service
create_event_topic "payu.transaction.initiated.v1" 24
create_event_topic "payu.transaction.completed.v1" 24
create_event_topic "payu.transaction.failed.v1" 12
create_dlq_topic "payu.transaction.completed.v1" 24

# Create topics for Wallet Service
create_event_topic "payu.wallet.balance.changed.v1" 12
create_state_topic "payu.state.wallet.balance.v1"
create_dlq_topic "payu.wallet.balance.changed.v1" 12

echo "Topic creation completed"
```

---

## 7. Operational Guidelines

### 7.1 Monitoring Checklist

| Metric | Alert Threshold | Action |
|--------|-----------------|--------|
| Consumer Lag | > 10,000 messages | Scale consumers |
| Partition Skew | > 20% difference | Review partitioning key |
| Message Rate Drop | > 50% from baseline | Check producer health |
| DLQ Messages | > 0 in 5 minutes | Investigate failures |
| Schema Registry Errors | > 10/minute | Check compatibility |

### 7.2 Disaster Recovery

| Scenario | RTO | RPO | Procedure |
|----------|-----|-----|-----------|
| Single broker failure | 0s | 0 | Automatic (replication factor 3) |
| Full cluster failure | 30 min | 0 | Restore from backup |
| Topic corruption | 15 min | < 1 min | Recreate from schema registry |
| Schema incompatibility | 5 min | 0 | Rollback to previous version |

### 7.3 Topic Lifecycle Management

```
1. Create topic in DEV environment
2. Register schema to DEV registry
3. Test with integration tests
4. Promote to STAGING
5. Performance testing
6. Promote to PRODUCTION
7. Monitor for 48 hours
8. Document in service README
```

### 7.4 Deprecation Process

1. **Announce deprecation** - 30 days notice
2. **Create new version** - `payu.domain.event.v2`
3. **Dual publish** - Publish to both v1 and v2 for 14 days
4. **Migrate consumers** - Update all consumer groups
5. **Stop publishing** - Remove v1 producer code
6. **Delete topic** - After 30 days retention

---

## Appendix A: Quick Reference Card

### Topic Naming Decision Tree

```
Is it a state store?
├── YES → payu.state.<domain>.<entity>.v1 (compact)
└── NO → Is it an audit log?
    ├── YES → payu.audit.<domain>.<action>.v1 (30d retention)
    └── NO → Is it a DLQ?
        ├── YES → payu.<domain>.<event>.v1.dlq (14d retention)
        └── NO → payu.<domain>.<event-type>.v1 (7d retention)
```

### Configuration Quick Pick

| Use Case | Command |
|----------|---------|
| Standard Event | `partitions=6, retention.ms=604800000, cleanup.policy=delete` |
| High Throughput | `partitions=24, retention.ms=604800000` |
| State Store | `partitions=3, cleanup.policy=compact, retention.ms=-1` |
| Audit Log | `partitions=12, retention.ms=2592000000` |
| DLQ | `partitions=3, retention.ms=1209600000` |

---

## References

- [ADR-0005: Kafka as Event Streaming Platform](../adr/0005-kafka-event-streaming.md)
- [Confluent Schema Registry Documentation](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [PayU Disaster Recovery Plan](../operations/DISASTER_RECOVERY.md)

---

*Document maintained by Platform Engineering Team. For questions, contact #platform-engineering on Slack.*
