---
name: kafka-stream-specialist
description: Expert in event streaming with Red Hat AMQ Streams (Kafka). Covers producer/consumer optimization, stream processing with Kafka Streams, CDC with Debezium, and exactly-once semantics for PayU microservices.
---

# Kafka Stream Specialist (AMQ Streams)

Expert skill for implementing production-grade event streaming on **PayU's Red Hat OpenShift** platform using **AMQ Streams**.

## 🎯 Core Capabilities

1.  **High-Performance Messaging**: Tuning producers/consumers for maximum throughput on AMQ Streams.
2.  **Stream Processing**: Implementing stateful logic using **Kafka Streams** (Java/Quarkus) or **Faust** (Python).
3.  **Data Integration**: Real-time CDC pipelines using **Debezium** for legacy system synchronization.
4.  **Reliability patterns**: Implementing Dead Letter Queues (DLQ), Circuit Breakers, and Exactly-Once semantics.

---

## 🏗️ PayU Streaming Architecture

### 1. Technology Stack
- **Broker**: Red Hat AMQ Streams (Apache Kafka 3.6+).
- **Schema**: Apicurio Registry (Avro format).
- **Connect**: AMQ Connect + Debezium (PostgreSQL/MongoDB).
- **Clients**:
    - **Java/Quarkus**: SmallRye Reactive Messaging (`quarkus-smallrye-reactive-messaging-kafka`).
    - **Spring Boot**: Spring Kafka (`spring-kafka`).
    - **Python**: AIOKafka (Async) or Confluent Kafka.
    - **Node.js**: KafkaJS (BFF/Gateway).

### 2. Topic Naming Convention
Format: `payu.<domain>.<event-type>.<version>`
Example: `payu.transaction.payment-created.v1`

---

## ⚡ Quick Implementation Patterns

### Pattern A: Robust Producer (Spring Boot)
Ensure data durability with `acks=all`.

```java
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, PaymentEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Durability
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // No dupes
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### Pattern B: Resilient Consumer (Quarkus)
Use DLQ for poison pill messages.

```java
@ApplicationScoped
public class PaymentConsumer {

    @Incoming("payments-in")
    @Retry(delay = 100, maxRetries = 3)
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public void consume(Record<String, PaymentEvent> record) {
        try {
            processPayment(record.value());
        } catch (Exception e) {
            // Unrecoverable errors go to DLQ automatically via SmallRye config
            throw e; 
        }
    }
}
```

### Pattern C: Change Data Capture (Debezium)
Stream PostgreSQL changes to Kafka.

```json
{
  "name": "wallet-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "wallet-db",
    "plugin.name": "pgoutput",
    "table.include.list": "public.wallets,public.ledger"
  }
}
```

---

## 🛡️ Reliability & Delivery Guarantees

### 1. Delivery Semantics
- **At-Least-Once (Default)**: Use for audit logs, notifications.
    - *Requirement*: Consumers must be idempotent.
- **Exactly-Once (Transactional)**: Use for **Balance Updates** & **Fund Transfers**.
    - *Requirement*: `isolation.level=read_committed` on consumer, `transactional.id` on producer.

### 2. Error Handling Strategy
1.  **Retry (Blocking)**: For transient errors (network blips). Max 3 retries.
2.  **Retry (Non-Blocking)**: Republish to `retry-topic` with delay (e.g., `payment.retry.5m`).
3.  **Dead Letter Queue (DLQ)**: Final destination for corrupt/unprocessable messages. Alert SRE.

---

## 🔧 Performance Tuning Checklist

| Parameter | Recommended Value | Impact |
| :--- | :--- | :--- |
| `batch.size` | `65536` (64KB) | Higher throughput, slight latency increase. |
| `linger.ms` | `5` - `20` | Allows batching to accumulate. |
| `compression.type` | `snappy` or `lz4` | Reduces network bandwidth & disk usage. |
| `num.partitions` | `3` (Default), `6+` (High Load) | Limits consumer parallelism. |

---

## 📂 Reference Guide (In Project)

Use the local guides in `references/` for deep dives:
- **`references/cdc-patterns.md`**: Guide for setting up Debezium.
- **`references/exactly-once.md`**: Implementing transactional messaging.
- **`references/performance-tuning.md`**: Advanced broker/client turning.
- **`references/error-handling.md`**: Design patterns for failure scenarios.
- **`references/observability.md`**: Prometheus alerts & PodMonitor setup for AMQ Streams.
- **`references/topology-patterns.md`**: Advanced Kafka Streams patterns (Windowing, Joins, Testing).
- **`references/strimzi-operator.md`**: Strimzi CRDs (KafkaTopic, KafkaUser) for GitOps.

> **Note**: Ignore generic comparisons in reference files (e.g. RabbitMQ vs Pulsar). PayU exclusively uses AMQ Streams.

---

## 🤖 Related Skills for Dispatch

- **`@event-driven-architecture`**: For high-level system design and Saga patterns.
- **`@backend-engineer`**: For implementing logic within the consumers.
- **`@devops-engineer`**: For deploying AMQ Streams and Kafkdrop/UI.

---
*Last Updated: January 2026 (PayU Standard)*
