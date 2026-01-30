# Saga Patterns & Distributed Transactions Reference

## Saga Orchestration Pattern

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    SAGA ORCHESTRATOR                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐                                               │
│  │    Saga      │                                               │
│  │  Orchestrator│                                               │
│  │   (Central)  │                                               │
│  └──────┬───────┘                                               │
│         │                                                        │
│    ┌────┴────┬────────────┬────────────┐                       │
│    │         │            │            │                        │
│    ▼         ▼            ▼            ▼                        │
│ ┌──────┐ ┌──────┐    ┌──────┐    ┌──────┐                      │
│ │Wallet│ │Trans │    │Partner│   │Notif │                      │
│ │Debit │ │Record│    │Credit │   │Send  │                      │
│ └──────┘ └──────┘    └──────┘    └──────┘                      │
│    │         │            │            │                        │
│    └────┬────┴────────────┴────────────┘                       │
│         │                                                        │
│         ▼                                                        │
│  ┌──────────────┐                                               │
│  │    State     │   States: INITIATED → DEBITED → RECORDED →   │
│  │   Machine    │           CREDITED → NOTIFIED → COMPLETED    │
│  └──────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

### Saga State Machine Implementation

```java
@Entity
@Table(name = "transfer_sagas")
public class TransferSaga {
    
    @Id
    private UUID sagaId;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    private SagaState state;
    
    @Enumerated(EnumType.STRING)
    private SagaState previousState;
    
    @Column(columnDefinition = "jsonb")
    private String payload;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "retry_count")
    private int retryCount;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Version
    private Long version;
}

public enum SagaState {
    INITIATED,
    WALLET_DEBITED,
    TRANSACTION_RECORDED,
    PARTNER_CREDITED,
    NOTIFICATION_SENT,
    COMPLETED,
    
    // Compensation states
    COMPENSATING,
    PARTNER_CREDIT_REVERSED,
    TRANSACTION_REVERSED,
    WALLET_CREDITED_BACK,
    COMPENSATION_COMPLETED,
    
    // Terminal states
    FAILED
}
```

### Saga Orchestrator Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSagaOrchestrator {
    
    private final SagaRepository sagaRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final PartnerClient partnerClient;
    private final NotificationService notificationService;
    
    @Transactional
    public TransferResult executeTransfer(TransferCommand command) {
        // 1. Create or retrieve saga
        TransferSaga saga = sagaRepository.findByIdempotencyKey(command.getIdempotencyKey())
            .orElseGet(() -> createSaga(command));
        
        // 2. Resume from current state
        return processSaga(saga);
    }
    
    private TransferResult processSaga(TransferSaga saga) {
        try {
            while (!saga.getState().isTerminal()) {
                saga = executeStep(saga);
                sagaRepository.save(saga);
            }
            return buildResult(saga);
        } catch (Exception e) {
            return handleFailure(saga, e);
        }
    }
    
    private TransferSaga executeStep(TransferSaga saga) {
        return switch (saga.getState()) {
            case INITIATED -> debitWallet(saga);
            case WALLET_DEBITED -> recordTransaction(saga);
            case TRANSACTION_RECORDED -> creditPartner(saga);
            case PARTNER_CREDITED -> sendNotification(saga);
            case NOTIFICATION_SENT -> completeSaga(saga);
            case COMPENSATING -> executeCompensation(saga);
            default -> saga;
        };
    }
    
    // Step implementations
    private TransferSaga debitWallet(TransferSaga saga) {
        TransferPayload payload = saga.getPayloadAs(TransferPayload.class);
        
        walletService.debit(
            payload.getSourceAccountId(),
            payload.getAmount(),
            "Transfer: " + saga.getSagaId()
        );
        
        return saga.transitionTo(SagaState.WALLET_DEBITED);
    }
    
    private TransferSaga creditPartner(TransferSaga saga) {
        TransferPayload payload = saga.getPayloadAs(TransferPayload.class);
        
        PartnerResponse response = partnerClient.creditAccount(
            payload.getDestinationAccountId(),
            payload.getAmount(),
            saga.getSagaId().toString()
        );
        
        if (!response.isSuccess()) {
            throw new PartnerCreditFailedException(response.getErrorMessage());
        }
        
        return saga.transitionTo(SagaState.PARTNER_CREDITED);
    }
    
    // Compensation logic
    private TransferResult handleFailure(TransferSaga saga, Exception e) {
        log.error("Saga {} failed at state {}: {}", 
            saga.getSagaId(), saga.getState(), e.getMessage());
        
        saga.setFailureReason(e.getMessage());
        saga.transitionTo(SagaState.COMPENSATING);
        sagaRepository.save(saga);
        
        // Execute compensation
        executeCompensation(saga);
        
        return TransferResult.failed(saga.getSagaId(), e.getMessage());
    }
    
    private TransferSaga executeCompensation(TransferSaga saga) {
        SagaState compensateFrom = saga.getPreviousState();
        
        return switch (compensateFrom) {
            case PARTNER_CREDITED -> reversePartnerCredit(saga);
            case TRANSACTION_RECORDED -> reverseTransaction(saga);
            case WALLET_DEBITED -> creditWalletBack(saga);
            default -> saga.transitionTo(SagaState.COMPENSATION_COMPLETED);
        };
    }
    
    private TransferSaga creditWalletBack(TransferSaga saga) {
        TransferPayload payload = saga.getPayloadAs(TransferPayload.class);
        
        walletService.credit(
            payload.getSourceAccountId(),
            payload.getAmount(),
            "Reversal: " + saga.getSagaId()
        );
        
        return saga.transitionTo(SagaState.COMPENSATION_COMPLETED);
    }
}
```

---

## Saga Choreography Pattern

### Event-Driven Saga

```
┌─────────────────────────────────────────────────────────────────┐
│                    SAGA CHOREOGRAPHY                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────┐    TransferInitiated    ┌──────────┐             │
│  │  Wallet  │ ─────────────────────►  │Transaction│            │
│  │  Service │                         │  Service  │             │
│  └────┬─────┘                         └─────┬─────┘             │
│       │                                     │                    │
│       │ WalletDebited                       │ TransactionRecorded│
│       ▼                                     ▼                    │
│  ┌──────────┐                         ┌──────────┐             │
│  │  Kafka   │ ◄─────────────────────  │  Kafka   │             │
│  │  Topic   │                         │  Topic   │              │
│  └────┬─────┘                         └─────┬─────┘             │
│       │                                     │                    │
│       │                                     │PartnerCredited    │
│       ▼                                     ▼                    │
│  ┌──────────┐                         ┌──────────┐             │
│  │ Partner  │ ◄─────────────────────  │  Notif   │             │
│  │ Service  │     TransferCompleted   │  Service │             │
│  └──────────┘                         └──────────┘             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Event Handlers

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventHandler {
    
    private final WalletService walletService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "payu.transfer.initiated.v1")
    @Transactional
    public void handleTransferInitiated(TransferInitiatedEvent event) {
        try {
            // Execute local transaction
            walletService.debit(
                event.getSourceAccountId(),
                event.getAmount(),
                event.getTransferId()
            );
            
            // Publish success event
            kafkaTemplate.send(
                "payu.wallet.debited.v1",
                event.getTransferId(),
                WalletDebitedEvent.builder()
                    .transferId(event.getTransferId())
                    .accountId(event.getSourceAccountId())
                    .amount(event.getAmount())
                    .timestamp(Instant.now())
                    .build()
            );
            
        } catch (InsufficientBalanceException e) {
            // Publish failure event
            kafkaTemplate.send(
                "payu.wallet.debit-failed.v1",
                event.getTransferId(),
                WalletDebitFailedEvent.builder()
                    .transferId(event.getTransferId())
                    .reason("INSUFFICIENT_BALANCE")
                    .timestamp(Instant.now())
                    .build()
            );
        }
    }
    
    // Compensation handler
    @KafkaListener(topics = "payu.transfer.compensation.v1")
    @Transactional
    public void handleCompensation(TransferCompensationEvent event) {
        if (event.getCompensationStep() == CompensationStep.CREDIT_WALLET_BACK) {
            walletService.credit(
                event.getSourceAccountId(),
                event.getAmount(),
                "Reversal: " + event.getTransferId()
            );
            
            kafkaTemplate.send(
                "payu.wallet.credited-back.v1",
                event.getTransferId(),
                WalletCreditedBackEvent.builder()
                    .transferId(event.getTransferId())
                    .timestamp(Instant.now())
                    .build()
            );
        }
    }
}
```

---

## Transactional Outbox Pattern

### Database Schema

```sql
-- Outbox table for reliable event publishing
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    retry_count INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status ON outbox_events(status) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_created ON outbox_events(created_at);
```

### Outbox Publisher (Polling)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Scheduled(fixedDelay = 100) // Poll every 100ms
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository
            .findTop100ByStatusOrderByCreatedAtAsc("PENDING");
        
        for (OutboxEvent event : events) {
            try {
                String topic = String.format("payu.%s.%s.v1", 
                    event.getAggregateType().toLowerCase(),
                    event.getEventType().toLowerCase().replace("_", "-"));
                
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);
                
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
                
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                }
                outboxRepository.save(event);
            }
        }
    }
}
```

### Debezium CDC (Alternative)

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "wallet-db",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "${vault:database.password}",
    "database.dbname": "wallet",
    "database.server.name": "payu",
    "table.include.list": "public.outbox_events",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.route.topic.replacement": "payu.${routedByValue}",
    "transforms.outbox.table.fields.additional.placement": "aggregate_type:header:aggregateType"
  }
}
```

---

## Saga Monitoring & Observability

### Metrics to Track

```promql
# Saga success rate
sum(rate(saga_completed_total[5m])) / sum(rate(saga_started_total[5m]))

# Saga compensation rate
sum(rate(saga_compensation_total[5m])) / sum(rate(saga_started_total[5m]))

# Average saga duration
histogram_quantile(0.95, sum(rate(saga_duration_seconds_bucket[5m])) by (le, saga_type))

# Stuck sagas (not progressing)
count(saga_state{state!="COMPLETED", state!="FAILED"} unless saga_updated_at > time() - 300)
```

### Grafana Dashboard Panels

```yaml
panels:
  - title: "Saga Throughput"
    type: graph
    targets:
      - expr: sum(rate(saga_started_total[5m])) by (saga_type)
        legendFormat: "{{saga_type}}"
  
  - title: "Saga Success Rate"
    type: gauge
    targets:
      - expr: sum(rate(saga_completed_total[5m])) / sum(rate(saga_started_total[5m])) * 100
    thresholds:
      - color: red
        value: 95
      - color: yellow
        value: 99
      - color: green
        value: 99.5
  
  - title: "Compensation Rate"
    type: stat
    targets:
      - expr: sum(rate(saga_compensation_total[5m])) / sum(rate(saga_started_total[5m])) * 100
  
  - title: "Saga State Distribution"
    type: piechart
    targets:
      - expr: count(saga_state) by (state)
```

---

## Best Practices

### Do's

1. **Idempotency First**: Every saga step must be idempotent
2. **Timeout Handling**: Set reasonable timeouts for each step
3. **State Persistence**: Always persist saga state before external calls
4. **Compensation Order**: Reverse of execution order
5. **Monitoring**: Track saga success/failure rates

### Don'ts

1. **No Distributed Transactions**: Never use 2PC across services
2. **No Synchronous Chains**: Avoid long synchronous call chains
3. **No State in Memory**: Always persist to database
4. **No Silent Failures**: Always emit events on failure
5. **No Infinite Retries**: Set max retry limits
