# Event-Driven Architecture & Integration Patterns

## 📥 Transactional Outbox Pattern
*   **The Problem**: Publishing Kafka events directly after a DB update is non-transactional. If Kafka is down, the event is lost but the DB change remains.
*   **The Pattern**: Save the event in an `outbox_events` table within the SAME database transaction as the business operation.
*   **Implementation**:
    1.  Use `outbox-starter`'s `OutboxService.createEvent()`.
    2.  An asynchronous `OutboxPublisher` polls and sends events to Kafka with retries.
    3.  Guaranteed delivery ("at-least-once").
*   **Required Services**: `transaction-service`, `wallet-service`, `lending-service`, `billing-service`.

## 🎭 Saga Orchestration
*   **The Problem**: Handcrafted saga logic is complex and hard to maintain across multiple services.
*   **The Pattern**: Use `saga-starter` for centralized orchestration with built-in compensation.
*   **Implementation**:
    ```java
    SagaDefinition<TransferContext> saga = SagaDefinition.<TransferContext>builder()
        .step("debit-source").action(ctx -> ...).compensation(ctx -> ...)
        .step("credit-destination").action(ctx -> ...).compensation(ctx -> ...)
        .build();
    orchestrator.execute(saga, ctx);
    ```
*   **Benefits**: Automatic rollback (compensation) on intermediate step failure, uniform logging/monitoring.

## 🗄️ Saga Infrastructure — `saga_instances` Table Requirement (L-013)

Services including `saga-starter` automatically initialize `SagaRecoveryService`, which requires a local `saga_instances` table for state persistence. If this table is missing, the application will fail to start or crash during recovery cycles.

**Symptoms**:
*   Hibernate error: `Relation "saga_instances" does not exist`
*   Service health check failing or 503 errors on dependent endpoints

**Rule**: Every microservice using `saga-starter` MUST have a Flyway migration creating the `saga_instances` table. The schema must remain consistent across all services to ensure compatibility with the shared `saga-starter` entity mappings.

## ⚡ Kafka Recovery & Failover
*   **Recovery Test**: Verify topic integrity and message consumption continuity after broker pod failure.
*   **Strimzi Patterns**: Use MirrorMaker 2 (`KafkaMirrorMaker2`) for multi-region active-standby replication.
*   **KRaft Mode**: PayU uses Kafka with KRaft (no Zookeeper). Startup is simplified — just start the Kafka broker. No Zookeeper coordination or `NodeExists` registration errors to worry about.

## 🔄 Kafka Deserialization — Cross-Service Class Mismatch (L-012)

When sharing events via Kafka between microservices with different package structures (e.g., `fx-service` publishing `id.payu.fx.adapter.messaging.FxRatesUpdatedEvent` consumed by `wallet-service`), the default Jackson deserializer fails with `ClassNotFoundException` because the FQCN in the message header doesn't exist in the consumer.

**Fix**: Use `spring.json.type.mapping` in `application.yml`:
```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.type.mapping: >-
          id.payu.fx.adapter.messaging.FxRatesUpdatedEvent:id.payu.wallet.adapter.messaging.fx.FxRatesUpdatedEvent
```

**Alternatives**:
*   Shared event library with identical package names (best for high-traffic event contracts)
*   Use `spring.kafka.consumer.properties.spring.json.trusted.packages=*` alongside explicit type mapping

**Rule**: Always use explicit type mapping or shared event libraries with identical package names for cross-service Kafka events. Never assume the consumer has the same FQCN as the producer.

## 🚀 Quarkus Integration
*   **Security/Resilience Gap**: Quarkus services (notification, gateway) currently lack shared starters.
*   **Recommendation**: 
    1.  Implement Quarkus-native `ContainerRequestFilter` for JWT validation using `SmallRye JWT`.
    2.  OR migrate services to Spring Boot parent POM for long-term consistency with the platform ecosystem.
