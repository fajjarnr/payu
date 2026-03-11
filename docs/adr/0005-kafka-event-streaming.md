# ADR-0005: Kafka as Event Streaming Platform

**Status**: Accepted
**Date**: 2026-01-30
**Last Updated**: 2026-03-11
**Deciders**: Architecture Team, Engineering Leads

## Context

PayU platform requires an event streaming backbone for:

- Event-driven communication between services
- Saga pattern for distributed transactions
- Real-time data synchronization
- Audit trail for all transactions

## Decision Drivers

- **Reliability**: Guaranteed message delivery
- **Scalability**: Handle high throughput
- **Ordering**: Maintain message ordering per key
- **Ecosystem**: Integration with Red Hat AMQ Streams
- **Maturity**: Proven in financial services

## Considered Options

### Option 1: Apache Kafka (AMQ Streams)

- **Pros**:
  - Guaranteed message delivery
  - Message ordering per key
  - Horizontal scalability
  - Proven in banking
  - Red Hat AMQ Streams support
- **Cons**:
  - Operational complexity
  - ~~Requires ZooKeeper (pre-KRaft)~~ → Migrated to KRaft (Mar 2026)
- **Complexity**: High
- **Rationale**: Industry standard for event streaming

### Option 2: RabbitMQ (AMQ Broker)

- **Pros**:
  - Simpler to operate
  - Good for request-response patterns
  - Red Hat AMQ Broker support
- **Cons**:
  - Not designed for high-throughput streaming
  - Limited scalability
  - Different programming model
- **Complexity**: Medium
- **Rationale**: Better for point-to-point messaging

### Option 3: Redis Streams

- **Pros**:
  - Simple to operate
  - Lightweight
- **Cons**:
  - Not enterprise-grade
  - Limited tooling
  - No Red Hat support
- **Complexity**: Low
- **Rationale**: Not suitable for production banking

## Decision

**Choose Apache Kafka** (via Red Hat AMQ Streams) for:

- Event streaming between services
- Saga choreography
- Audit event logs
- Real-time notifications

**Choose AMQ Broker (RabbitMQ)** for:

- Point-to-point messaging (notification queue)
- Request-response patterns where needed

## Rationale

1. **Reliability**: Kafka provides guaranteed delivery and ordering
2. **Scalability**: Horizontal scaling for high throughput
3. **Ecosystem**: Red Hat AMQ Streams integration
4. **Saga Support**: Essential for distributed transactions
5. **Audit Trail**: Immutable log of all events

## Consequences

**Positive**:

- Reliable event delivery
- Excellent for Saga pattern
- Horizontal scalability
- Enterprise support from Red Hat

**Negative**:

- Operational complexity
- ~~Requires ZooKeeper~~ → Eliminated via KRaft migration (Mar 2026)
- More complex than message queues

**Trade-offs Accepted**:

- Accept operational complexity for reliability
- Accept Kafka learning curve for event streaming benefits

> **Amendment (Mar 11, 2026)**: Kafka migrated from ZooKeeper to **KRaft mode** (`cp-kafka:7.7.1`). Zookeeper service, quadlet files, and dependencies fully removed from `podman-compose.yml`. KRaft config uses combined `broker,controller` process roles with static `CLUSTER_ID`. Aligned with AMQ Streams operator on OpenShift.

## Implementation Notes

### Topic Naming Convention

```
payu.{domain}.{event-type}
```

Examples:

- `payu.accounts.account-created`
- `payu.transactions.transaction-initiated`
- `payu.wallet.balance-changed`

### Partitioning Strategy

- Partition by `aggregateId` for ordering guarantee
- Number of partitions: 3 (can be scaled up)

### Replication Factor

- Development: 1
- Production: 3

### Retention Policy

- 7 days for event topics
- 30 days for audit topics

### Producer Configuration

```java
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
```

### Consumer Configuration

```java
spring:
  kafka:
    consumer:
      group-id: payu-${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
```

---

_Created via @principal-architect_
