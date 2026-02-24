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

## ⚡ Kafka Recovery & Failover
*   **Recovery Test**: Verify topic integrity and message consumption continuity after broker pod failure.
*   **Strimzi Patterns**: Use MirrorMaker 2 (`KafkaMirrorMaker2`) for multi-region active-standby replication.
*   **Sequential Startup**: Stop both Kafka and Zookeeper -> Start Zookeeper -> Wait -> Start Kafka to avoid `NodeExists` registration errors.

## 🚀 Quarkus Integration
*   **Security/Resilience Gap**: Quarkus services (notification, gateway) currently lack shared starters.
*   **Recommendation**: 
    1.  Implement Quarkus-native `ContainerRequestFilter` for JWT validation using `SmallRye JWT`.
    2.  OR migrate services to Spring Boot parent POM for long-term consistency with the platform ecosystem.
