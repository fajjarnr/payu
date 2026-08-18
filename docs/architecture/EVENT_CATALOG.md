# Kafka Event Catalog

> **Authoritative inventory of all Kafka topics declared in [`01-kafka-topics-code.yaml`](../../infrastructure/platform/messaging/base/01-kafka-topics-code.yaml).**
>
> Architecture decision: [ADR-0026](../adr/0026-kafka-topic-governance-and-dlq-strategy.md). Generated from manifest (ARCH-TOPIC-002). Last updated: 2026-08-18.

## 📊 Summary

| Metric | Count |
|:---|:---:|
| **Non-DLQ topics** | 65 |
| **DLQ topics** | 42 |
| **Total KafkaTopic resources** | 107 |
| **Domains** | 17 |

---

## 📋 Topic Naming Convention

```
payu.{domain}.{event-type}.v{version}[.dlq]
```

- **Domain**: singular kebab-case (`account`, `transaction`, `wallet`)
- **Event type**: kebab-case (`balance-changed`, `loan-approved`)
- **Version**: `v{N}` suffix — enables backward-compatible evolution
- **DLQ**: `.dlq` suffix for dead-letter topics

---

## 🏦 Account Domain

> **Owner**: account-service | **Topics**: 4 + 4 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.account.kyc-completed.v1` | 3 | 3 | 7d | **Producer**: account-service · **Consumer**: notification-service |
| `payu.account.opened.v10` | 3 | 3 | 7d | **Producer**: shared/outbox · **Consumer**: wallet-service |
| `payu.account.user-created.v1` | 3 | 3 | 7d | **Producer**: account-service · **Consumer**: wallet-service |
| `payu.account.user-updated.v1` | 3 | 3 | 7d | **Producer**: account-service |
| `payu.account.kyc-completed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.account.opened.v10.dlq` | 3 | 3 | 30d | DLQ |
| `payu.account.user-created.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.account.user-updated.v1.dlq` | 3 | 3 | 30d | DLQ |

---

## 💳 Billing Domain

> **Owner**: billing-service | **Topics**: 3 (no DLQ — Tier 3)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.billing.plan-created.v1` | 3 | 3 | 7d | **Producer**: billing-service |
| `payu.billing.subscription-due.v1` | 3 | 3 | 7d | **Producer**: billing-service |
| `payu.billing.subscription-event.v1` | 3 | 3 | 7d | **Producer**: billing-service · **Consumer**: partner-service |

---

## 🔄 Cache Domain

> **Owner**: shared/cache-starter | **Topics**: 1 (no DLQ — Tier 3, ephemeral)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.cache.invalidation.v1` | 3 | 3 | 7d | **Producer**: account-service, product-catalog-service, transaction-service · **Consumer**: all cache-enabled services |

---

## 📝 CMS Domain

> **Owner**: cms-service | **Topics**: 3 (no DLQ — Tier 3)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.cms.content-archived.v1` | 3 | 3 | 7d | **Producer**: cms-service |
| `payu.cms.content-published.v1` | 3 | 3 | 7d | **Producer**: cms-service |
| `payu.cms.content-updated.v1` | 3 | 3 | 7d | **Producer**: cms-service |

---

## ⚖️ Dispute Domain

> **Owner**: dispute-service | **Topics**: 2 (no DLQ — DLQ via escalated shared starter)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.dispute.escalated.v1` | 3 | 3 | 7d | **Producer**: shared/outbox |
| `payu.dispute.refund-requested.v1` | 3 | 3 | 7d | **Producer**: dispute-service · **Consumer**: wallet-service |

---

## 💱 FX Domain

> **Owner**: fx-service | **Topics**: 1 (no DLQ — Tier 3, next tick overwrites)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.fx.rates-updated.v1` | 3 | 3 | 7d | **Producer**: fx-service · **Consumer**: wallet-service |

---

## 🔗 Integration Domain

> **Owner**: integration-service | **Topics**: 3 (no DLQ — Tier 3, error reporting)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.integration.ojk-errors.v1` | 3 | 3 | 7d | **Producer**: integration-service |
| `payu.integration.swift-errors.v1` | 3 | 3 | 7d | **Producer**: integration-service |
| `payu.integration.swift-processed.v1` | 3 | 3 | 7d | **Producer**: integration-service |

---

## 📈 Investment Domain

> **Owner**: investment-service | **Topics**: 1

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.investment.event.v1` | 3 | 3 | 7d | **Producer**: investment-service · **Consumer**: partner-service |

> ⚠️ **Gap**: Code references `payu.investment.completed.v1`, `payu.investment.created.v1`, `payu.investment.failed.v1` in investment-service and partner-service. These are NOT in the manifest. Investigate whether `payu.investment.event.v1` is the canonical topic or if 3 additional topics need declaring.

---

## 🔑 KYC Domain

> **Owner**: kyc-service (Python) | **Topics**: 3 + 2 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.kyc.failed.v1` | 3 | 3 | 7d | **Producer**: kyc-service · **Consumer**: notification-service |
| `payu.kyc.ktp-uploaded.v1` | 3 | 3 | 7d | **Producer**: kyc-service |
| `payu.kyc.verified.v1` | 3 | 3 | 7d | **Producer**: kyc-service · **Consumer**: notification-service |
| `payu.kyc.failed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.kyc.verified.v1.dlq` | 3 | 3 | 30d | DLQ |

---

## 🏦 Lending Domain

> **Owner**: lending-service, loan-origination-process | **Topics**: 4 + 4 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.lending.loan-approved.v1` | 3 | 3 | 7d | **Producer**: lending-service |
| `payu.lending.loan-disbursed.v1` | 3 | 3 | 7d | **Producer**: loan-origination-process |
| `payu.lending.loan-rejected.v1` | 3 | 3 | 7d | **Producer**: lending-service |
| `payu.lending.loan-repayment-processed.v1` | 3 | 3 | 7d | **Producer**: lending-service |
| `payu.lending.loan-approved.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.lending.loan-disbursed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.lending.loan-rejected.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.lending.loan-repayment-processed.v1.dlq` | 3 | 3 | 30d | DLQ |

---

## 🤝 Partner Domain

> **Owner**: partner-service | **Topics**: 5 + 5 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.partner.merchant-settlement.v1` | 3 | 3 | 7d | **Producer**: partner-service |
| `payu.partner.payment-completed.v1` | 3 | 3 | 7d | **Producer**: partner-service |
| `payu.partner.payment-link-event.v1` | 3 | 3 | 7d | **Producer**: partner-service |
| `payu.partner.payment-refunded.v1` | 3 | 3 | 7d | **Producer**: partner-service |
| `payu.partner.refund-completed.v1` | 3 | 3 | 7d | **Producer**: partner-service |
| `payu.partner.merchant-settlement.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.partner.payment-completed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.partner.payment-link-event.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.partner.payment-refunded.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.partner.refund-completed.v1.dlq` | 3 | 3 | 30d | DLQ |

---

## 💳 Payment Domain

> **Owner**: shared/outbox | **Topics**: 0 + 1 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.payment.failed.v3.dlq` | 3 | 3 | 30d | DLQ |

> ℹ️ `payu.payment.failed.v3` is referenced in shared starters but declared as a DLQ topic only.

---

## 🎁 Promotion Domain

> **Owner**: promotion-service | **Topics**: 5 (no DLQ — Tier 3)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.promotion.cashback-event.v1` | 3 | 3 | 7d | **Producer**: promotion-service |
| `payu.promotion.loyalty-event.v1` | 3 | 3 | 7d | **Producer**: promotion-service |
| `payu.promotion.notification.v1` | 3 | 3 | 7d | **Producer**: promotion-service |
| `payu.promotion.promotion-event.v1` | 3 | 3 | 7d | **Producer**: promotion-service |
| `payu.promotion.referral-event.v1` | 3 | 3 | 7d | **Producer**: promotion-service |

---

## 🔄 Saga Domain

> **Owner**: shared/saga-starter | **Topics**: 1 (no DLQ — saga state machine handles recovery)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.saga.events.v1` | 3 | 3 | 7d | **Producer**: shared/outbox · **Consumer**: saga orchestrators |

---

## 🔒 Security Domain

> **Owner**: shared/security-starter | **Topics**: 1 (no DLQ — audit persisted in DB)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.security.audit-log.v1` | 3 | 3 | 7d | **Producer**: account-service, shared · **Consumer**: analytics-service |

---

## 📊 Statement Domain

> **Owner**: statement-service | **Topics**: 1

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.statement.generated.v1` | 3 | 3 | 7d | **Producer**: statement-service |

---

## 💸 Transaction Domain

> **Owner**: transaction-service | **Topics**: 11 + 12 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.transaction.completed.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.initiated.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: partner-service |
| `payu.transaction.participant-added.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service |
| `payu.transaction.payment-expired.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.payment-made.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.payment-reminder.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service |
| `payu.transaction.split-bill-activated.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.split-bill-cancelled.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.split-bill-completed.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.split-bill-created.v1` | 3 | 3 | 7d | **Producer**: transaction-service · **Consumer**: notification-service, partner-service |
| `payu.transaction.transfer-archived.v1` | 3 | 3 | 7d | **Producer**: transaction-service |
| `payu.transaction.va-paid.v1` | 3 | 3 | 7d | **Producer**: transaction-service |
| `payu.transaction.completed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.initiated.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.participant-added.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.payment-expired.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.payment-made.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.payment-reminder.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.split-bill-activated.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.split-bill-cancelled.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.split-bill-completed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.split-bill-created.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.split-bill-events.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.transfer-archived.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.transaction.va-paid.v1.dlq` | 3 | 3 | 30d | DLQ |

> ⚠️ **Gap**: Code references `payu.transaction.validated.v1`, `payu.transaction.failed.v1` (partner-service consumer), and `payu.transaction.disbursement-batch.v1` (transaction-service). These are NOT in the manifest.

---

## 🔀 Transfer Domain

> **Owner**: shared/outbox | **Topics**: 1

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.transfer.completed.v1` | 3 | 3 | 7d | **Producer**: shared/outbox |

---

## 💰 Wallet Domain

> **Owner**: wallet-service | **Topics**: 15 + 13 DLQ

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.wallet.balance-changed.v1` | 3 | 3 | 7d | **Producer**: wallet-service · **Consumer**: notification-service, partner-service |
| `payu.wallet.balance-reserved.v1` | 3 | 3 | 7d | **Producer**: wallet-service |
| `payu.wallet.created.v1` | 3 | 3 | 7d | **Producer**: wallet-service |
| `payu.wallet.credited.v1` | 3 | 3 | 7d | **Producer**: shared/outbox |
| `payu.wallet.escrow-expired.v1` | 3 | 3 | 7d | **Producer**: wallet-service · **Consumer**: partner-service |
| `payu.wallet.escrow-held.v1` | 3 | 3 | 7d | **Producer**: wallet-service · **Consumer**: partner-service |
| `payu.wallet.escrow-refunded.v1` | 3 | 3 | 7d | **Producer**: wallet-service · **Consumer**: partner-service |
| `payu.wallet.escrow-released.v1` | 3 | 3 | 7d | **Producer**: wallet-service · **Consumer**: partner-service |
| `payu.wallet.escrow-settled.v1` | 3 | 3 | 7d | **Producer**: wallet-service · **Consumer**: partner-service |
| `payu.wallet.reservation-committed.v1` | 3 | 3 | 7d | **Producer**: wallet-service |
| `payu.wallet.reservation-released.v1` | 3 | 3 | 7d | **Producer**: wallet-service |
| `payu.wallet.topup-completed.v1` | 3 | 3 | 7d | **Producer**: wallet-service |
| `payu.wallet.transfer.v1` | 3 | 3 | 7d | **Producer**: shared/outbox |
| `payu.wallet.balance-changed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.balance-reserved.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.created.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.credited.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.escrow-expired.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.escrow-held.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.escrow-refunded.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.escrow-released.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.escrow-settled.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.reservation-committed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.reservation-released.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.topup-completed.v1.dlq` | 3 | 3 | 30d | DLQ |
| `payu.wallet.transfer.v1.dlq` | 3 | 3 | 30d | DLQ |

---

## 🔔 Webhook Domain

> **Owner**: shared/webhook | **Topics**: 1 (no DLQ — webhook has own retry via PARTNER-PROD-004)

| Topic | P | RF | Retention | Services |
|:---|:---:|:---:|:---:|:---|
| `payu.webhook.events.v1` | 3 | 3 | 7d | **Producer**: shared/outbox |

---

## 🚨 DLQ Strategy

### Tiered Approach

| Tier | Domains | DLQ? | Rationale |
|:---|:---|:---:|:---|
| **1 — Financial** | transaction, wallet, payment, partner, lending, account | ✅ | Financial event loss = reconciliation risk |
| **2 — Operational** | kyc | ✅ | KYC failure = regulatory risk |
| **3 — Non-critical** | billing, cache, cms, dispute, fx, integration, investment, promotion, saga, security, statement, transfer, webhook | ❌ | UX/ops impact only, no financial loss |

### DLQ Configuration

- **Retention**: 30 days (`retention.ms: 2592000000`) — regulatory buffer
- **Partitions**: 3 (matches parent topic)
- **RF**: 3 (matches parent topic)
- **Consumer**: Deferred — `OutboxCleanupScheduler` logs `OUTBOX-001 ALERT` for DB-side tracking
- **Replay**: Via `scripts/dlq-replay.sh` (planned)

---

## ⚠️ Manifest vs Code Gaps

Topics referenced in code but **NOT declared** in the manifest:

| Topic | Referenced By | Action Needed |
|:---|:---|:---|
| `payu.investment.completed.v1` | investment-service, partner-service | Investigate: is `payu.investment.event.v1` the canonical topic? |
| `payu.investment.created.v1` | investment-service, partner-service | Same as above |
| `payu.investment.failed.v1` | investment-service, partner-service | Same as above |
| `payu.transaction.validated.v1` | partner-service (consumer) | Add to manifest or verify auto-created |
| `payu.transaction.failed.v1` | partner-service (consumer) | Add to manifest or verify auto-created |
| `payu.transaction.disbursement-batch.v1` | transaction-service | Add to manifest |

---

## 🔧 Configuration Standards

### Producer (Spring Boot)

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

### Consumer (Spring Boot)

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

### Event Format (CloudEvents 1.0.2)

All events published via `outbox-starter` use CloudEvents envelope:

```json
{
  "specversion": "1.0",
  "id": "<event-uuid>",
  "type": "payu.<domain>.<event-type>",
  "source": "payu/<service-name>",
  "time": "2026-08-18T10:00:00Z",
  "datacontenttype": "application/json",
  "data": { }
}
```

---

_Source of truth: [`01-kafka-topics-code.yaml`](../../infrastructure/platform/messaging/base/01-kafka-topics-code.yaml)_
_Last regenerated: 2026-08-18 (ARCH-TOPIC-002)_
