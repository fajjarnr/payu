# Kafka Event Catalog

> **Complete inventory of all Kafka topics, event schemas, and producers/consumers**

## 📊 Topics Overview

| Domain | Topics | Count |
|--------|--------|-------|
| **Accounts** | 4 topics | 4 |
| **Transactions** | 4 topics | 4 |
| **Wallet** | 3 topics | 3 |
| **Notifications** | 2 topics | 2 |
| **DLQ** | 2 topics | 2 |

**Total: 15 topics**

---

## 📋 Topic Naming Convention

```
payu.{domain}.{event-type}
```

Examples:
- `payu.accounts.account-created`
- `payu.transactions.transaction-initiated`
- `payu.wallet.balance-changed`

---

## 🏦 Accounts Domain

### account-created

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.accounts.account-created` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | account-service |
| **Consumers** | wallet-service, transaction-service, analytics-service |

**Event Schema:**
```json
{
  "eventId": "evt_001",
  "eventType": "account-created",
  "timestamp": "2026-01-30T10:00:00Z",
  "aggregateId": "acc-123456",
  "aggregateType": "Account",
  "data": {
    "accountId": "acc-123456",
    "accountNumber": "8890123456789000",
    "accountType": "SAVINGS",
    "customerId": "cust-001",
    "currency": "IDR",
    "initialBalance": 0,
    "status": "ACTIVE"
  }
}
```

---

### account-updated

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.accounts.account-updated` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | account-service |
| **Consumers** | analytics-service |

---

### pocket-created

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.accounts.pocket-created` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | account-service |
| **Consumers** | wallet-service |

---

### pocket-balance-changed

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.accounts.pocket-balance-changed` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | account-service |
| **Consumers** | wallet-service, analytics-service |

---

## 💸 Transactions Domain

### transaction-initiated

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.transactions.transaction-initiated` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | transaction-service |
| **Consumers** | wallet-service (Saga participant), analytics-service |

**Event Schema:**
```json
{
  "eventId": "evt_002",
  "eventType": "transaction-initiated",
  "timestamp": "2026-01-30T10:00:00Z",
  "aggregateId": "txn-789012",
  "aggregateType": "Transaction",
  "data": {
    "transactionId": "txn-789012",
    "transactionType": "TRANSFER",
    "fromAccountId": "acc-001",
    "toAccountId": "acc-002",
    "amount": 100000,
    "currency": "IDR",
    "reference": "REF-001"
  }
}
```

---

### transaction-validated

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.transactions.transaction-validated` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | transaction-service |
| **Consumers** | wallet-service (Saga participant) |

---

### transaction-completed

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.transactions.transaction-completed` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 30 days (audit) |
| **Producer** | transaction-service |
| **Consumers** | notification-service, analytics-service, statement-service |

---

### transaction-failed

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.transactions.transaction-failed` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | transaction-service |
| **Consumers** | notification-service, analytics-service |

---

## 💰 Wallet Domain

### balance-reserved

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.wallet.balance-reserved` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | wallet-service |
| **Consumers** | transaction-service (Saga orchestrator) |

**Event Schema:**
```json
{
  "eventId": "evt_003",
  "eventType": "balance-reserved",
  "timestamp": "2026-01-30T10:00:00Z",
  "aggregateId": "txn-789012",
  "aggregateType": "Transaction",
  "data": {
    "transactionId": "txn-789012",
    "accountId": "acc-001",
    "amount": 100000,
    "currency": "IDR",
    "reservedUntil": "2026-01-30T10:05:00Z"
  }
}
```

---

### balance-committed

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.wallet.balance-committed` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | wallet-service |
| **Consumers** | transaction-service (Saga orchestrator), analytics-service |

---

### balance-released

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.wallet.balance-released` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | wallet-service |
| **Consumers** | transaction-service (Saga orchestrator) |

---

## 🔔 Notifications Domain

### notification-requested

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.notifications.notification-requested` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | Various services |
| **Consumers** | notification-service |

---

### notification-delivered

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.notifications.notification-delivered` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 7 days |
| **Producer** | notification-service |
| **Consumers** | analytics-service |

---

## 🚨 Dead Letter Queues

### transactions-dlq

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.dlq.transactions` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 30 days |
| **Purpose** | Failed transaction events |

---

### notifications-dlq

| Attribute | Value |
|-----------|-------|
| **Topic** | `payu.dlq.notifications` |
| **Partitions** | 3 |
| **Replication Factor** | 3 |
| **Retention** | 30 days |
| **Purpose** | Failed notification events |

---

## 🔧 Configuration

### Producer Configuration

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
```

### Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      group-id: payu-${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: "*"
```

---

## 📝 Event Standards

### Common Event Fields

All events must include:

```json
{
  "eventId": "unique-event-id",
  "eventType": "event-type-name",
  "timestamp": "ISO-8601-timestamp",
  "aggregateId": "aggregate-id",
  "aggregateType": "Aggregate",
  "data": { /* event-specific data */ }
}
```

### Event Versioning

- Events are versioned by adding `_v{version}` suffix
- Example: `account-created_v2`
- Consumers must handle all versions

---

_Last Updated: January 30, 2026_
