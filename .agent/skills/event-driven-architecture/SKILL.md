---
name: event-driven-architecture
description: Expert in Event-Driven Architecture for PayU Digital Banking - Kafka messaging, Saga patterns, event sourcing, and distributed transaction management.
---

# PayU Event-Driven Architecture Skill

You are an expert in **Event-Driven Architecture (EDA)** for the PayU Digital Banking Platform. Your expertise covers Kafka messaging, Saga patterns, eventual consistency, and distributed transaction management.

## 🎯 Core Principles

| Principle | Description |
|-----------|-------------|
| **Loose Coupling** | Services communicate via events, not direct calls |
| **Eventual Consistency** | Accept temporary inconsistency for availability |
| **Idempotency** | Handle duplicate events gracefully |
| **Event Sourcing** | Store state changes as immutable events |

---

## 📬 Kafka Configuration (AMQ Streams)

### Topic Naming Convention

```
<domain>.<entity>.<event-type>

Examples:
- wallet.balance.changed
- transaction.transfer.initiated
- transaction.transfer.completed
- transaction.transfer.failed
- account.user.created
- notification.sms.requested
```

### Topic Configuration

| Topic Type | Partitions | Replication | Retention |
|------------|------------|-------------|-----------|
| **Commands** | 3 | 3 | 7 days |
| **Events** | 6 | 3 | 30 days |
| **Notifications** | 3 | 3 | 3 days |
| **Audit** | 12 | 3 | 365 days |

### Producer Configuration (Spring Boot)

```java
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Performance
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### Consumer Configuration (Spring Boot)

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "wallet-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Reliability
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(1000L, 3L)
        ));
        return factory;
    }
}
```

---

## 📨 Event Structure

### CloudEvents Standard

All events MUST follow [CloudEvents](https://cloudevents.io/) specification:

```java
public record PayuEvent<T>(
    String id,              // UUID
    String source,          // "payu://wallet-service"
    String type,            // "id.payu.wallet.BalanceChanged"
    String subject,         // "wallet:WAL-001"
    Instant time,           // Event timestamp
    String dataContentType, // "application/json"
    T data,                 // Event payload
    Map<String, String> extensions  // traceId, correlationId
) {}
```

### Event Envelope Example

```json
{
  "id": "evt-550e8400-e29b-41d4-a716-446655440000",
  "source": "payu://transaction-service",
  "type": "id.payu.transaction.TransferInitiated",
  "subject": "transfer:TXN-001",
  "time": "2026-01-26T10:30:00Z",
  "datacontenttype": "application/json",
  "data": {
    "transferId": "TXN-001",
    "sourceAccountId": "ACC-001",
    "targetAccountId": "ACC-002",
    "amount": 500000.00,
    "currency": "IDR"
  },
  "traceparent": "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01",
  "correlationid": "corr-123456"
}
```

---

## 🔄 Saga Patterns: Orchestration vs. Choreography

Distributed transactions in PayU are implemented using the **Saga Pattern**. Choose the right approach based on complexity.

### 1. Choreography (Event-Based)
Services publish and subscribe to each other's events without a central controller.
- **Best for**: Simple workflows with 2-3 services.
- **Pros**: Low overhead, easy to scale.
- **Cons**: Difficult to track the overall state of the transaction.

### 2. Orchestration (Command-Based)
A central "Orchestrator" service coordinates the steps by sending commands and receiving events.
- **Best for**: Complex workflows (e.g., Transfer which involves Balance, Fraud, BI-FAST, and Notifications).
- **Pros**: Centralized logic, easy to monitor, explicit state management.
- **Cons**: Orchestrator becomes a single point of failure (requires high availability).

### Transfer Saga Flow (Choreography Example)

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Transaction    │     │     Wallet      │     │  Notification   │
│    Service      │     │    Service      │     │    Service      │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │ TransferInitiated     │                       │
         ├──────────────────────►│                       │
         │                       │                       │
         │                       │ DebitCompleted        │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ CreditRequested       │                       │
         ├──────────────────────►│                       │
         │                       │                       │
         │                       │ CreditCompleted       │
         │◄──────────────────────┤                       │
         │                       │                       │
         │ TransferCompleted     │                       │
         ├──────────────────────►├──────────────────────►│
         │                       │                       │
         │                       │      SendNotification │
         │                       │◄──────────────────────┤
         ▼                       ▼                       ▼
```

### Saga Implementation

```java
@Service
@RequiredArgsConstructor
public class TransferSagaOrchestrator {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransferRepository transferRepository;

    @KafkaListener(topics = "wallet.debit.completed")
    public void onDebitCompleted(DebitCompletedEvent event, Acknowledgment ack) {
        try {
            var transfer = transferRepository.findById(event.getTransferId())
                .orElseThrow(() -> new TransferNotFoundException(event.getTransferId()));
            
            // Update state
            transfer.markDebitCompleted();
            transferRepository.save(transfer);
            
            // Publish next step
            var creditRequest = new CreditRequestedEvent(
                transfer.getId(),
                transfer.getTargetAccountId(),
                transfer.getAmount()
            );
            kafkaTemplate.send("wallet.credit.requested", transfer.getId(), creditRequest);
            
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process debit completed", e);
            // Will be retried or sent to DLQ
        }
    }

    @KafkaListener(topics = "wallet.debit.failed")
    public void onDebitFailed(DebitFailedEvent event, Acknowledgment ack) {
        // Compensating action - no debit happened, just mark failed
        var transfer = transferRepository.findById(event.getTransferId())
            .orElseThrow();
        
        transfer.markFailed(event.getReason());
        transferRepository.save(transfer);
        
        // Notify user of failure
        kafkaTemplate.send("notification.transfer.failed", event.getTransferId(), 
            new TransferFailedNotification(transfer));
        
        ack.acknowledge();
    }

    @KafkaListener(topics = "wallet.credit.failed")
    public void onCreditFailed(CreditFailedEvent event, Acknowledgment ack) {
        // Compensating action - reverse the debit
        var transfer = transferRepository.findById(event.getTransferId())
            .orElseThrow();
        
        // Publish compensation
        var reverseDebit = new DebitReversalRequestedEvent(
            transfer.getId(),
            transfer.getSourceAccountId(),
            transfer.getAmount()
        );
        kafkaTemplate.send("wallet.debit.reversal.requested", transfer.getId(), reverseDebit);
        
        transfer.markCompensating();
        transferRepository.save(transfer);
        
        ack.acknowledge();
    }
}
```

---

## 🔒 Idempotency Pattern

### Idempotency Key Storage

```java
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;
    
    private String eventType;
    private Instant processedAt;
    private String result;  // SUCCESS, FAILED
}

@Service
@RequiredArgsConstructor
public class IdempotentEventProcessor {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public <T> T processIdempotent(String eventId, Supplier<T> processor) {
        // Check if already processed
        var existing = processedEventRepository.findById(eventId);
        if (existing.isPresent()) {
            log.info("Event {} already processed, skipping", eventId);
            return null; // Or return cached result
        }
        
        try {
            // Process event
            T result = processor.get();
            
            // Mark as processed
            processedEventRepository.save(new ProcessedEvent(
                eventId, 
                "SUCCESS", 
                Instant.now()
            ));
            
            return result;
        } catch (Exception e) {
            processedEventRepository.save(new ProcessedEvent(
                eventId, 
                "FAILED", 
                Instant.now()
            ));
            throw e;
        }
    }
}
```

### Usage in Consumer

```java
@KafkaListener(topics = "wallet.credit.requested")
public void onCreditRequested(
    @Header(KafkaHeaders.RECEIVED_KEY) String key,
    @Payload CreditRequestedEvent event,
    Acknowledgment ack
) {
    idempotentProcessor.processIdempotent(event.getId(), () -> {
        walletService.credit(event.getAccountId(), event.getAmount());
        
        kafkaTemplate.send("wallet.credit.completed", key, 
            new CreditCompletedEvent(event.getTransferId()));
        
        return null;
    });
    
    ack.acknowledge();
}
```

---

## 🏛️ Event Store Design (PostgreSQL)

For services using **Event Sourcing** (like `wallet-service`), we implement a custom Event Store on top of PostgreSQL.

### 1. Schema Definition
```sql
-- Events table: Append-only ledger of facts
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stream_id VARCHAR(255) NOT NULL, -- e.g., "wallet-001"
    stream_type VARCHAR(255) NOT NULL, -- e.g., "Wallet"
    event_type VARCHAR(255) NOT NULL, -- e.g., "BalanceChanged"
    event_data JSONB NOT NULL,
    metadata JSONB DEFAULT '{}',     -- traceId, correlationId
    version BIGINT NOT NULL,         -- Per-stream version for OCC
    global_position BIGSERIAL,       -- Global order across all streams
    created_at TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT unique_stream_version UNIQUE (stream_id, version)
);

-- Snapshots table: To optimize aggregate reconstruction
CREATE TABLE snapshots (
    stream_id VARCHAR(255) PRIMARY KEY,
    snapshot_data JSONB NOT NULL,
    version BIGINT NOT NULL,         -- Last version included in snapshot
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Checkpoints: For real-time event subscribers
CREATE TABLE subscription_checkpoints (
    subscription_id VARCHAR(255) PRIMARY KEY,
    last_position BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### 2. Core Operational Patterns

#### A. Optimistic Concurrency Control (OCC)
Always check the `expected_version` before appending to prevent race conditions.
```sql
-- Before insert: Verify version
SELECT MAX(version) FROM events WHERE stream_id = 'wallet-001';
-- If version matches expected, perform insert in transaction.
```

#### B. Snapshotting
Avoid replaying thousands of events by periodically saving the aggregate state.
1. Load latest snapshot.
2. Replay only events where `version > snapshot.version`.

#### C. Determinism
Workflows or Aggregate logic MUST be deterministic. No `now()`, `random()`, or network calls inside the `apply()` logic.

---

## 📊 Event Sourcing Implementation

### Event Store Entity

```java
@Entity
@Table(name = "event_store")
public class StoredEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sequenceNumber;
    
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    
    @Column(columnDefinition = "jsonb")
    private String payload;
    
    private Instant occurredAt;
    private Long version;
}
```

### Aggregate Reconstruction

```java
public class WalletAggregate {
    private String walletId;
    private BigDecimal balance = BigDecimal.ZERO;
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();

    public static WalletAggregate reconstruct(List<StoredEvent> events) {
        var aggregate = new WalletAggregate();
        events.forEach(e -> aggregate.apply(deserialize(e)));
        return aggregate;
    }

    private void apply(DomainEvent event) {
        if (event instanceof WalletCreatedEvent e) {
            this.walletId = e.walletId();
            this.balance = BigDecimal.ZERO;
        } else if (event instanceof BalanceCreditedEvent e) {
            this.balance = this.balance.add(e.amount());
        } else if (event instanceof BalanceDebitedEvent e) {
            this.balance = this.balance.subtract(e.amount());
        }
    }

    public void credit(BigDecimal amount) {
        // Business rule validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException();
        }
        
        // Raise event
        var event = new BalanceCreditedEvent(walletId, amount, Instant.now());
        apply(event);
        uncommittedEvents.add(event);
    }
}
```

---

## 🏗️ Advanced Workflow Patterns

### 1. Entity Workflows (Actor Model)
Use long-lived workflows to represent a single entity instance (e.g., a "Transaction" or "User Session").
- **Pattern**: Every state change is a signal to the workflow.
- **Benefit**: Guarantees consistency and provides a natural audit trail.

### 2. Fan-Out/Fan-In (Parallel Execution)
Execute multiple tasks in parallel and aggregate results before proceeding.
- **Example**: Checking fraud scoring from 3 different providers concurrently.
- **Constraint**: Aggregate results only after ALL (or a majority) have responded within a timeout.

### 3. Saga Compensation (Rollback logic)
For every forward action, define a backward compensation.
- **Rule**: Register compensation BEFORE executing the step.
- **Execution**: On failure, run compensations in reverse order (LIFO).
- **Idempotency**: All compensations MUST be idempotent.

---

## ⚡ Durable Execution & Workflow Resilience

Menerapkan prinsip "Durable Execution" (seperti pola Inngest) untuk memastikan workflow backend kita tahan terhadap restart service atau network failure.

### 1. Checkpoint & Memoization (Durable Steps)
Pecah workflow besar menjadi langkah-langkah kecil (`Steps`). Setiap langkah yang sukses harus dicatat (*checkpoint*) agar tidak diulang jika terjadi retry.
- **Implementasi**: Gunakan tabel `saga_state` untuk mencatat langkah mana saja yang sudah `COMPLETED`.
- **Aturan**: Sebelum menjalankan Step X, cek apakah Step X sudah tercatat sukses di DB.

### 2. Durable Sleep & Scheduling
HINDARI penggunaan `Thread.sleep()` untuk delay yang lama (misal: menunggu 24 jam sebelum retry).
- **Pola**: Simpan state ke database dengan kolom `scheduled_at`, lalu gunakan scheduler (seperti Quartz atau Kafka delayed messages) untuk men-trigger kembali workflow.

### 3. Concurrency & Rate Limiting
Kontrol beban workflow di level event consumer.
- **In-flight Limit**: Batasi jumlah transaksi paralel per user_id menggunakan Redis lock atau Kafka partition locking.
- **Throttling**: Jika penyedia eksternal (misal: BI-FAST) lambat, turunkan kecepatan consumer secara otomatis (back-off).

### 4. Step-Based Error Handling
Jangan batalkan seluruh Saga hanya karena satu Step *transient error*.
- **Pola**: Bedakan antara *Fatal Error* (trigger Compensation) dan *Retriable Error* (tahan state, coba lagi nanti).

---

## 🧪 Testing Event-Driven Systems

### Embedded Kafka Test

```java
@EmbeddedKafka(
    partitions = 1,
    topics = {"wallet.debit.requested", "wallet.debit.completed"}
)
@SpringBootTest
class TransferSagaTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void shouldCompleteTransferSaga() {
        // Given
        var transferId = "TXN-001";
        var event = new TransferInitiatedEvent(transferId, "ACC-001", "ACC-002", 
            new BigDecimal("100000"));

        // When
        kafkaTemplate.send("transaction.transfer.initiated", transferId, event);

        // Then - verify debit completed event is published
        var consumer = createConsumer("wallet.debit.completed");
        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        
        assertThat(records.count()).isEqualTo(1);
        var debitEvent = deserialize(records.iterator().next().value());
        assertThat(debitEvent.getTransferId()).isEqualTo(transferId);
    }
}
```

### Testcontainers Kafka

```java
@Testcontainers
@SpringBootTest
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldPublishAndConsumeEvent() {
        // ... test implementation
    }
}
```

---

## ⚠️ Error Handling

### Dead Letter Queue (DLQ)

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template, 
        (record, ex) -> new TopicPartition(record.topic() + ".dlq", 0));
    
    var backoff = new ExponentialBackOff(1000L, 2.0);
    backoff.setMaxElapsedTime(60000L); // Max 1 minute of retries
    
    return new DefaultErrorHandler(recoverer, backoff);
}
```

### DLQ Consumer for Manual Intervention

```java
@KafkaListener(topics = "#{__listener.dlqTopics}")
public void processDlq(
    ConsumerRecord<String, Object> record,
    @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String errorMessage
) {
    log.error("DLQ message received: topic={}, key={}, error={}", 
        record.topic(), record.key(), errorMessage);
    
    // Store for manual review
    dlqRepository.save(new DlqMessage(record, errorMessage));
    
    // Alert operations team
    alertService.sendDlqAlert(record.topic(), record.key());
}
```

---

## 📋 Checklist

Before implementing event-driven features:

- [ ] Define event schema with CloudEvents format
- [ ] Set up topic with proper partitioning and replication
- [ ] Implement idempotency handling
- [ ] Configure DLQ for failed messages
- [ ] Add distributed tracing (traceparent header)
- [ ] Write integration tests with EmbeddedKafka
- [ ] Document event flow in architecture docs
- [ ] Set up monitoring for consumer lag

## 🤖 Agent Delegation & Parallel Execution (Event-Driven)

Untuk orkestrasi arsitektur berbasis event yang andal, gunakan pola delegasi paralel (Swarm Mode):

- **Messaging Infrastructure**: Delegasikan ke **`@orchestrator`** untuk konfigurasi topic Kafka dan management consumer group.
- **Saga & Aggregate Logic**: Aktifkan **`@logic-builder`** secara paralel untuk implementasi Saga Orchestrator dan Aggregate reconstruction.
- **Event Persistence**: Panggil **`@migrator`** secara simultan untuk setup skema Event Store (PostgreSQL) dan snapshotting.
- **Distributed Verification**: Jalankan **`@tester`** untuk menulis integrasi test yang melibatkan Kafka (Embedded/Testcontainers) secara paralel.

---

*Last Updated: January 2026*
