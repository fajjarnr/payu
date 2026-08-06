---
name: integration-architect
description: Event-driven and integration architecture — Kafka/Strimzi topics and consumers, transactional outbox, CloudEvents, saga orchestration, CDC (Debezium), queue-based commands, and API gateway patterns. Use when designing, implementing, debugging, reviewing, or testing event-driven flows, message producers/consumers, outbox publishing, sagas, CDC, or API management in any software project.
---

# Integration Architect

Design event-driven flows that are reliable, idempotent, and traceable: events
published atomically with the business transaction, delivered at least once,
and consumed safely. Read the service's POM and event code, and the messaging
infrastructure, before changing behavior. Reuse the project's messaging
primitives (outbox, event envelope, saga framework) before adding a dependency
or abstraction.

## Context7 documentation gate

Before writing or changing code that uses a library, framework, SDK, API, CLI,
or cloud service:

1. Read the module POM and parent/BOM to determine the exact version in use.
2. Resolve the library in Context7. Prefer the official, high-reputation result
   and pin the query to the repository version when that version is available.
3. Query one concrete topic at a time: API, configuration, testing, migration,
   or integration behavior. Use the returned documentation as the source of
   truth; do not rely on remembered annotations, artifact names, or property
   namespaces.
4. If the exact version is not indexed, use the nearest official version only
   as a stated fallback, then verify the actual API in the project source or
   dependency JAR before editing.
5. Re-resolve and re-query after changing a dependency version. Do not mix
   examples from different major versions.

Use Context7 for Strimzi/AMQ Streams (`KafkaNodePool`, `KafkaTopic`, KRaft),
Debezium (connector config, outbox), Kafka clients, CloudEvents, and similar
third-party libraries. Context7 does not replace project inspection for
platform conventions.

## Events and topic design

- Publish domain events atomically with the business transaction using the
  transactional outbox pattern (write an outbox row in the same DB transaction
  as the business change), never a direct broker `send()` from application
  code. Debezium CDC or the outbox dispatcher then publishes to the broker.
- Use the CloudEvents 1.0.2 envelope for cross-service events: `specversion`,
  `id`, `source`, `type`, `datacontenttype`, `time`, `subject`, and data.
  Preserve correlation/trace context across the flow.
- Follow a versioned topic convention such as `<domain>.<entity>.<event-type>.v<n>`
  (for example `orders.order-created.v1`) and version topics when the payload
  contract changes. Add `.dlq` topics for poisoned or undeliverable messages.
- Declare topics declaratively (Strimzi `KafkaTopic` resources on
  Kubernetes/OpenShift) with partitions and replicas matching the cluster
  policy (for example replication factor 3, `min.insync.replicas` 2). Size
  partitions for consumer parallelism and growth; derive from throughput and
  consumer group count, do not guess.
- Keep message payloads bounded and schema-versioned. Prefer compacted topics
  for state and delete retention for events.
- Use queues (for example AMQ Artemis or RabbitMQ) for request-style or
  scheduled commands, and Kafka topics for domain events — matching the
  project's existing split.

## Producer and consumer contracts

- Configure producers for at-least-once with idempotence enabled (`acks=all`,
  `enable.idempotence=true`); reserve transactional/exactly-once only where a
  financial operation genuinely requires it (the trade-offs are covered in
  Kafka's exactly-once docs).
- Make consumers at-least-once safe: process idempotently (claim
  `(source, event_id)` or an equivalent idempotency key), commit database work
  before acknowledging/advancing the offset, and test duplicate delivery,
  crash/retry, poison messages, and schema evolution.
- Use `read_committed` isolation and manual acknowledgment for transactional
  consumers. Handle poison pills with an error-handling deserializer or a DLQ,
  and bound retries with backoff.
- Treat consumer lag, retry rate, and DLQ depth as SLO metrics; alert before
  lag becomes critical rather than after.

## Sagas and durable workflows

- Use saga orchestration for distributed transactions with explicit
  compensation per step. Persist saga state in a database before external
  calls; every step and every compensation must be idempotent.
- Choose orchestration over choreography when the flow needs a central decision
  point (for example escrow or multi-party settlement); choreography only when
  the steps are simple event reactions.
- Define timeouts for every step and a terminal failure state; never leave a
  saga in an in-between state with infinite retries.
- Do not use 2PC across services, do not build long synchronous call chains,
  and do not keep saga state in memory.
- Consider a durable workflow engine (for example Temporal) for long-running,
  stateful processes with automatic retries and compensation, when the
  complexity is justified.

## CDC (Debezium)

- Use Debezium for change data capture where the outbox or audit trail must be
  streamed from the database: PostgreSQL connector with `pgoutput` plugin, a
  dedicated replication slot and publication, and the outbox event router
  transform (`EventRouter`) mapping `id`, `aggregate_id`, `event_type`,
  `payload` to the target topic.
- Exclude sensitive columns from CDC output and never emit encrypted/PII data
  to the broker without masking.
- Test snapshot vs incremental modes, slot reuse, and connector restart
  behavior; a stalled replication slot grows WAL and is an operational hazard.

## API management

- Keep a clean boundary: partner-facing concerns (API keys, rate plans,
  developer portal, analytics) belong to the API management tier; business
  security (auth, idempotency, business rules) belongs to the backend gateway
  or service. Do not push gateway responsibilities into the API tier or vice
  versa.
- Never expose admin endpoints (for example the 3scale Admin Portal or Kong
  Admin API) externally. Production should use a secret manager, HA backing
  stores, and mTLS between the API tier and the gateway through a service mesh.
- Extend API management only when a partner tier or rate plan requires it; do
  not over-build.

## Integration quality gate

- Test event flows with real components: Testcontainers (PostgreSQL + Kafka)
  or the project's existing fixtures for outbox, consumers, and sagas. Do not
  mock the broker and call it integration coverage.
- Verify outbox adoption per service before refactoring: grep for direct broker
  `send()` calls in application code and confirm the service includes the
  outbox module.
- Test failure paths: duplicate delivery, crash between DB commit and offset
  commit, poison messages, DLQ routing, saga compensation, and schema
  evolution.
- Verify topic resources reconcile (`oc get kafkatopic` on OpenShift) and that
  ACLs/users exist for the connecting services when SCRAM/TLS auth is enabled.
- Do not claim an integration works without command output from the actual
  service or broker.

## Review checklist

- [ ] Context7 resolved the exact library and the pinned version was checked.
- [ ] Events publish through the transactional outbox in the same DB transaction, not a direct broker `send()`.
- [ ] Events follow CloudEvents 1.0.2 and the versioned topic convention with `.dlq` topics.
- [ ] Topics are declarative with replicas and `min.insync.replicas` matching the cluster policy; partitions sized to the consumer group.
- [ ] Producers use at-least-once with idempotence; consumers are idempotent and commit after DB work.
- [ ] Poison pills, retries, and DLQ handling are explicit and bounded.
- [ ] Saga steps persist state, define compensation, set timeouts, and are idempotent.
- [ ] CDC excludes or masks sensitive columns and handles slot/restart behavior.
- [ ] API management respects the tier boundary and never exposes admin endpoints.
- [ ] Secrets come from a secret manager; no credentials in code, config, or messages.
- [ ] Tests cover real broker behavior, failure paths, and the service quality gate with command output.

## References

- [Strimzi documentation (KRaft, node pools, KafkaTopic)](https://strimzi.io/documentation/)
- [Apache Kafka documentation](https://kafka.apache.org/documentation/)
- [CloudEvents specification](https://cloudevents.io/)
- [Debezium documentation](https://debezium.io/documentation/)
- [Saga pattern (Microsoft Azure architecture)](https://learn.microsoft.com/en-us/azure/architecture/reference-architectures/saga/saga)
- [Transactional outbox (Microservices.io)](https://microservices.io/patterns/data/transactional-outbox.html)
- [Temporal documentation](https://docs.temporal.io/)
