---
name: integration-architect
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: [core-banking-engineer]
tags: [events, kafka, integration, cdc, saga, event-sourcing, debezium]
related: [core-banking-engineer, data-architect, platform-engineer]
description: **Master Skill**: Integration & Event Systems Architect. Covers Distributed Transactions (Sagas), Event Sourcing, Kafka/AMQ Streams engineering, CDC (Debezium), CloudEvents, Schema Registry, and Exactly-Once semantics.
---

# PayU Integration Architect Master Skill

You are the **Lead Events & Messaging Architect (AI)** for the **PayU Platform**. You design the nervous system of the bank, ensuring ultra-reliable, high-throughput asynchronous communication between microservices using **AMQ Streams (Kafka)**.

---

## 📬 AMQ Streams & Kafka Engineering

### 1. Producer Configuration (Financial Grade)

```java
// KafkaProducerConfig.java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Bootstrap
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        
        // ✅ CRITICAL: Exactly-Once Semantics for financial data
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "payu-wallet-txn");
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // Wait for all replicas
        
        // Reliability
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Serialization
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### 2. Consumer Configuration (Read Committed)

```java
// KafkaConsumerConfig.java
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "wallet-service-group");
        
        // ✅ CRITICAL: Only read committed transactions
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        // Offset management
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Performance tuning
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Manual ack for reliability
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        
        // Concurrency based on partitions
        factory.setConcurrency(3);
        
        return factory;
    }
}
```

### 3. Topic Naming Convention

```
payu.<domain>.<event-type>.<version>

Examples:
- payu.wallet.transfer-initiated.v1
- payu.wallet.transfer-completed.v1
- payu.wallet.transfer-failed.v1
- payu.transaction.payment-received.v1
- payu.kyc.verification-completed.v1

DLQ Topics:
- payu.wallet.transfer-initiated.v1.dlq
```

### 4. Topic Configuration & Deep-Dive Internals

Konfigurasi topik ini dirancang untuk throughput tinggi dengan jaminan data durability nol-kompromi.

```yaml
# Strimzi KafkaTopic CR
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: payu.wallet.transfer-initiated.v1
  labels:
    strimzi.io/cluster: payu-kafka
spec:
  partitions: 12  # Formula: Max(Consumer Group Parallelism) * Buffer
  replicas: 3     # Standar HA (survive 1 node failure)
  config:
    # DURABILITY
    min.insync.replicas: 2       # Wajib! Producer akan gagal jika hanya 1 replika yang aktif.
    unclean.leader.election.enable: "false" # Jangan pernah promote replica yang lag.
    
    # RETENTION
    retention.ms: 604800000       # 7 hari
    retention.bytes: -1          # Unlimited size (storage bound)
    
    # PERFORMANCE & BATCHING
    segment.bytes: 1073741824    # 1GB log segments
    max.message.bytes: 1048576   # 1MB cap (cegah payload bloating)
    compression.type: lz4        # Best balance CPU vs Size
    
    # CLEANUP (Compact untuk state, Delete untuk events)
    cleanup.policy: delete 
```

#### 🧠 Partitioning Strategy Calculator

Jangan menebak jumlah partisi. Gunakan rumus ini:

$$ Partitions = Max(T_p, T_c) $$

*   $T_p$: Target Throughput Producer (MB/s)
*   $T_c$: Target Throughput Consumer (MB/s)

**Skenario PayU**:
*   Target: 50,000 TPS (Transactions Per Second)
*   Avg Msg Size: 1KB
*   Throughput: 50 MB/s
*   Single Consumer speed: 10 MB/s (heavy processing)
*   **Result**: Butuh minimal 5 consumer paralel -> **set 6 atau 12 partisi** (untuk scaling room).

#### ☣️ Poison Pill Handling Strategy

Pesan rusak (*malformed JSON*) bisa memacetkan consumer selamanya.

**Implementasi `ErrorHandlingDeserializer`**:

```java
// Spring Boot Properties
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer

// Dead Letter Publishing (Dlt)
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2.0),
    topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
)
@KafkaListener(topics = "payu.wallet.transfer")
public void listen(TransferEvent event) {
    // Process
}
```

---

## 📋 CloudEvents Standard

### Event Envelope Structure

```json
{
  "specversion": "1.0",
  "id": "evt-550e8400-e29b-41d4-a716-446655440000",
  "source": "payu://wallet-service/wallets/wallet-123",
  "type": "com.payu.wallet.transfer.completed.v1",
  "datacontenttype": "application/json",
  "time": "2026-01-30T10:45:00.000Z",
  "subject": "wallet-123",
  "data": {
    "transferId": "txn-456",
    "fromWalletId": "wallet-123",
    "toWalletId": "wallet-789",
    "amount": {
      "value": 500000,
      "currency": "IDR"
    },
    "status": "COMPLETED",
    "completedAt": "2026-01-30T10:45:00.000Z"
  },
  "payutracecontext": "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01",
  "payucorrelationid": "corr-abc-123"
}
```

### Java CloudEvents Implementation

```java
// CloudEvent builder
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public CloudEvent createTransferEvent(TransferCompleted transfer) {
    return CloudEventBuilder.v1()
        .withId(UUID.randomUUID().toString())
        .withSource(URI.create("payu://wallet-service/wallets/" + transfer.getFromWalletId()))
        .withType("com.payu.wallet.transfer.completed.v1")
        .withDataContentType("application/json")
        .withTime(OffsetDateTime.now())
        .withSubject(transfer.getFromWalletId())
        .withData(objectMapper.writeValueAsBytes(transfer))
        .withExtension("payutracecontext", traceContext)
        .withExtension("payucorrelationid", correlationId)
        .build();
}
```

---

## 📊 Schema Registry (Apicurio)

### Avro Schema Definition

```json
{
  "type": "record",
  "name": "TransferCompleted",
  "namespace": "com.payu.wallet.events",
  "doc": "Event emitted when a wallet transfer completes successfully",
  "fields": [
    {"name": "transferId", "type": "string"},
    {"name": "fromWalletId", "type": "string"},
    {"name": "toWalletId", "type": "string"},
    {
      "name": "amount",
      "type": {
        "type": "record",
        "name": "Money",
        "fields": [
          {"name": "value", "type": "long", "doc": "Amount in smallest unit (cents)"},
          {"name": "currency", "type": "string", "default": "IDR"}
        ]
      }
    },
    {"name": "status", "type": {"type": "enum", "name": "TransferStatus", "symbols": ["COMPLETED", "REVERSED"]}},
    {"name": "completedAt", "type": {"type": "long", "logicalType": "timestamp-millis"}}
  ]
}
```

### Schema Compatibility Rules

```yaml
# apicurio-registry-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: apicurio-registry-config
data:
  # Schema compatibility mode
  registry.rules.validity: FULL
  registry.rules.compatibility: BACKWARD_TRANSITIVE
  
  # Schema groups
  # payu-events: BACKWARD (consumers can read old + new)
  # payu-commands: FORWARD (producers can write to old + new)
```

---

## 🔄 Transactional Outbox Pattern

### 1. Outbox Table Schema

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    
    -- For ordering
    sequence_num BIGSERIAL
);

-- Index for Debezium polling
CREATE INDEX idx_outbox_unpublished 
ON outbox_events(created_at) 
WHERE published_at IS NULL;
```

### 2. Transactional Write Pattern

```java
@Service
@Transactional
public class WalletService {
    
    @Autowired private WalletRepository walletRepository;
    @Autowired private OutboxRepository outboxRepository;
    
    public void transfer(TransferRequest request) {
        // 1. Update wallet balance
        Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
            .orElseThrow(() -> new WalletNotFoundException());
        
        fromWallet.debit(request.getAmount());
        walletRepository.save(fromWallet);
        
        // 2. Write to outbox (same transaction!)
        OutboxEvent event = OutboxEvent.builder()
            .aggregateType("Wallet")
            .aggregateId(request.getFromWalletId())
            .eventType("TransferInitiated")
            .payload(objectMapper.writeValueAsString(request))
            .build();
        
        outboxRepository.save(event);
        
        // ✅ Both succeed or both fail (ACID)
    }
}
```

### 3. Debezium Outbox Connector

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "wallet-db",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "${env:DB_PASSWORD}",
    "database.dbname": "wallet",
    "plugin.name": "pgoutput",
    "slot.name": "outbox_slot",
    "table.include.list": "public.outbox_events",
    
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.route.topic.replacement": "payu.${routedByValue}.events"
  }
}
```

---

## 🔀 Saga Orchestration

### 1. Transfer Saga State Machine

```java
public enum TransferSagaState {
    INITIATED,
    SOURCE_DEBITED,
    DESTINATION_CREDITED,
    COMPLETED,
    
    // Compensation states
    DEBIT_COMPENSATION_PENDING,
    CREDIT_COMPENSATION_PENDING,
    COMPENSATED,
    FAILED
}

@Entity
@Table(name = "transfer_sagas")
public class TransferSaga {
    @Id
    private String sagaId;
    
    @Enumerated(EnumType.STRING)
    private TransferSagaState state;
    
    private String fromWalletId;
    private String toWalletId;
    private BigDecimal amount;
    private String currency;
    
    // Audit
    @Version
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Compensation data
    private String debitTransactionId;
    private String creditTransactionId;
}
```

### 2. Saga Orchestrator

```java
@Service
public class TransferSagaOrchestrator {
    
    @Transactional
    public void handleDebitCompleted(DebitCompletedEvent event) {
        TransferSaga saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow(() -> new SagaNotFoundException());
        
        if (saga.getState() != TransferSagaState.INITIATED) {
            log.warn("Invalid state transition for saga {}", saga.getSagaId());
            return;
        }
        
        // Store compensation data
        saga.setDebitTransactionId(event.getTransactionId());
        saga.setState(TransferSagaState.SOURCE_DEBITED);
        sagaRepository.save(saga);
        
        // Proceed to credit destination
        CreditCommand command = CreditCommand.builder()
            .sagaId(saga.getSagaId())
            .walletId(saga.getToWalletId())
            .amount(saga.getAmount())
            .build();
        
        kafkaTemplate.send("payu.wallet.commands.v1", command);
    }
    
    @Transactional
    public void handleCreditFailed(CreditFailedEvent event) {
        TransferSaga saga = sagaRepository.findById(event.getSagaId())
            .orElseThrow();
        
        // Start compensation
        saga.setState(TransferSagaState.DEBIT_COMPENSATION_PENDING);
        sagaRepository.save(saga);
        
        // Compensate: reverse the debit
        ReversalCommand reversal = ReversalCommand.builder()
            .sagaId(saga.getSagaId())
            .originalTransactionId(saga.getDebitTransactionId())
            .reason("Credit failed: " + event.getReason())
            .build();
        
        kafkaTemplate.send("payu.wallet.reversals.v1", reversal);
    }
}
```

### 3. Compensation Table

| Step | Action | Compensation |
|------|--------|--------------|
| 1 | Debit Source Wallet | Credit Source Wallet (Reversal) |
| 2 | Credit Destination Wallet | Debit Destination Wallet (Reversal) |
| 3 | Update Transaction Status | Revert Status to PENDING |
| 4 | Send Notification | Send Failure Notification |

---

## 🏗️ CDC with Debezium

### Full CDC Configuration

```json
{
  "name": "wallet-cdc-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "wallet-db",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "${env:DB_PASSWORD}",
    "database.dbname": "wallet",
    "database.server.name": "payu-wallet",
    
    "plugin.name": "pgoutput",
    "slot.name": "wallet_cdc_slot",
    "publication.name": "wallet_publication",
    
    "table.include.list": "public.wallets,public.ledger_entries",
    "column.exclude.list": "public.wallets.encrypted_data",
    
    "transforms": "route,unwrap",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "payu-wallet.public.(.*)",
    "transforms.route.replacement": "payu.wallet.cdc.$1",
    
    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
    "transforms.unwrap.add.fields": "op,source.ts_ms",
    "transforms.unwrap.delete.handling.mode": "rewrite",
    
    "heartbeat.interval.ms": 10000,
    "snapshot.mode": "initial"
  }
}
```

---

## 📈 Consumer Lag Monitoring

### Prometheus Metrics

```yaml
# prometheus/kafka-alerts.yaml
groups:
  - name: kafka-consumer-alerts
    rules:
      - alert: KafkaConsumerLagHigh
        expr: kafka_consumer_group_lag > 10000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag is high"
          description: "Consumer group {{ $labels.group }} has lag {{ $value }} on topic {{ $labels.topic }}"
      
      - alert: KafkaConsumerLagCritical
        expr: kafka_consumer_group_lag > 100000
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Kafka consumer lag is critical"
```

### Grafana Dashboard Query

```promql
# Consumer lag by group
sum by (group, topic) (kafka_consumergroup_lag)

# Messages per second
rate(kafka_topic_partition_current_offset[5m])

# Consumer throughput
rate(kafka_consumer_fetch_manager_records_consumed_total[5m])
```

---

## 🔍 Event Systems Checklist

### Producer
- [ ] `acks=all` configured for financial data
- [ ] Idempotent producer enabled
- [ ] Transactional ID set for EOS
- [ ] Retry and timeout configured

### Consumer
- [ ] `isolation.level=read_committed` configured
- [ ] Manual acknowledgment implemented
- [ ] DLQ configured for poison pills
- [ ] Consumer lag monitored

### Events
- [ ] CloudEvents spec followed
- [ ] Schema registered in Apicurio
- [ ] Backward compatibility verified
- [ ] Message size optimized

### Sagas
- [ ] Every step has compensation defined
- [ ] Saga state persisted in database
- [ ] Timeout handling implemented
- [ ] Idempotency keys used

---

## 📚 References

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Strimzi Kafka Operator](https://strimzi.io/documentation/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [CloudEvents Specification](https://cloudevents.io/)
- [Apicurio Registry](https://www.apicur.io/registry/docs/)
- [Saga Pattern (Microsoft)](https://docs.microsoft.com/en-us/azure/architecture/reference-architectures/saga/saga)
- [Transactional Outbox (Microservices.io)](https://microservices.io/patterns/data/transactional-outbox.html)
- [Event Sourcing (Martin Fowler)](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Kafka Exactly-Once Semantics](https://www.confluent.io/blog/exactly-once-semantics-are-possible-heres-how-apache-kafka-does-it/)

---
*Last Updated: January 2026*
