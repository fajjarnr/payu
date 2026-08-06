---
name: core-banking-engineer
description: PayU backend engineering for Spring Boot, Quarkus, Java financial domains, hexagonal architecture, transactions, messaging, persistence, caching, resilience, and testing. Use when implementing, debugging, or reviewing backend services, especially payment, transfer, ledger, or event-driven flows; verify all third-party APIs and configuration with Context7 first.
---

# PayU Core Banking Engineer

Use the smallest change that preserves financial integrity, security, and the
existing service architecture. Read the target service's POM, configuration,
shared starters, and tests before changing code. Reuse a PayU starter or an
existing port before adding a dependency or abstraction.

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
   as a stated fallback, then verify the actual API in the repository's POM,
   source, or dependency JAR before editing.
5. Re-resolve and re-query after changing a dependency version. Do not mix
   examples from different major versions.

Use Context7 for Spring Boot/Spring Data/Spring Kafka, Quarkus, Resilience4j,
Flyway, Testcontainers, Micrometer, and similar third-party libraries. Context7
does not replace repository inspection for PayU starters or platform rules.

## Build and dependencies

Services inherit the PayU parent; they do not inherit `spring-boot-starter-parent`
directly:

```xml
<parent>
    <groupId>id.payu</groupId>
    <artifactId>payu-backend-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

- Read the parent POM before naming a version. Let its dependency management and
  BOMs manage third-party versions; add a local version only when the parent
  cannot manage it and the Context7 check confirms compatibility.
- Prefer `security-starter`, `resilience-starter`, `cache-starter`,
  `logging-starter`, `events-starter`, `outbox-starter`, and `archunit-starter`
  where the service needs them.
- Use Lombok when the module already uses it. If the same Lombok compilation
  error survives two fixes, replace the affected boilerplate explicitly and
  continue the build.

## Hexagonal architecture

Keep the domain independent of Spring, Quarkus, JPA, Kafka, HTTP clients, and
configuration. External communication crosses a port.

```text
interfaces (REST, DTOs) -> application (use cases) -> domain
                                  ^                    |
infrastructure/adapters (JPA, Kafka, HTTP, config) ---+
```

- Put request/response DTOs in `interfaces.dto`; do not expose domain objects
  at an API boundary.
- Put inbound use cases and outbound ports beside the application/domain model;
  adapters implement ports.
- Keep persistence entities and framework annotations in adapters. Define domain
  enums as top-level files.
- Add or update ArchUnit rules so domain code cannot depend on adapters,
  configuration, or framework packages.
- Match the service's established package names when they differ; do not move a
  whole service merely to satisfy this guide.

## Financial integrity

- Represent money with `BigDecimal`; never use `float` or `double`.
- Normalize monetary values to scale 4 with `RoundingMode.HALF_EVEN`; persist
  financial columns as `DECIMAL(19,4)`.
- Compare amounts with `compareTo`, not `equals`.
- Use double-entry postings and an immutable ledger. Never update or delete a
  financial fact; correct it with a reversal entry. A status transition is not a
  license to rewrite the original posting.
- Protect concurrent balance changes with optimistic locking or the service's
  established database locking strategy. Make the database constraint part of
  the invariant.
- Mask NIK, PIN, tokens, credentials, and other PII in logs. Keep secrets in
  Vault or the configured secret manager.

## Transactions and events

Put the business mutation and its outbox row in one database transaction:

```java
@Transactional
public Transfer execute(TransferCommand command) {
    Account source = accountPort.require(command.sourceId());
    Account target = accountPort.require(command.targetId());

    source.debit(command.amount());
    target.credit(command.amount());
    accountPort.save(source);
    accountPort.save(target);
    eventPort.transferCompleted(command, source, target);
    return Transfer.completed(command);
}
```

The messaging adapter must use PayU `outbox-starter`/`OutboxService` and
`events-starter`'s CloudEvent envelope, following the platform-required
CloudEvents 1.0.2 shape. Do not call `kafkaTemplate.send()` from application or
domain code. Use topics shaped as
`payu.<domain>.<event-type>.v<n>` and append `.dlq` only for a dead-letter
topic. Verify the starter's current CloudEvents representation before adding
custom serialization.

Keep event type, subject, source, correlation/trace identifiers, and the
versioned topic stable. Consumers must be idempotent because delivery is at
least once. Publish only after the domain state is valid and persisted.

## Persistence and JPA

- Repositories are outbound ports; JPA repositories and mappers stay in the
  persistence adapter.
- Use `@Transactional(readOnly = true)` for read paths and a write transaction
  for each atomic mutation. Avoid self-invocation when relying on proxy-based
  transaction or resilience annotations.
- Set `spring.jpa.open-in-view=false` and map data needed by the response before
  leaving the transaction.
- Prevent N+1 queries with a targeted projection, `JOIN FETCH`, or a bounded
  batch query. Verify the generated SQL for high-volume paths.
- Do not expose `delete` on a financial repository. Use archival/status handling
  only for non-financial data and follow the immutable-ledger rule for postings.

## API and idempotency

- Use versioned plural kebab-case paths such as `/v1/transfers`.
- Use RFC 9457 errors with a stable, unique PayU error code.
- Every payment, transfer, disbursement, and other mutation endpoint requires
  `X-Idempotency-Key`. Prefer PayU's `@Idempotent(required = true)` and its
  interceptor/repository over a hand-rolled Redis lock.
- Persist or atomically reserve the request fingerprint and result. Reject the
  same key with a different payload; never execute two financial mutations for
  one key.
- Validate untrusted input at the boundary and keep DTO validation separate
  from domain invariants.

## Caching

Use `cache-starter` and the service's configured cache provider. Cache immutable
or read-heavy reference data with an explicit key, TTL, serialization format,
and invalidation path. Do not treat a cache as the source of truth for balances,
ledger entries, idempotency state, or authorization. Add a custom multi-layer
cache only after a measured bottleneck and a defined consistency policy.

## Resilience and external calls

Resolve the exact Resilience4j and HTTP-client versions through the Context7
gate, then use the repository's `resilience-starter` configuration. Apply
resilience at the adapter boundary:

```java
@CircuitBreaker(name = "fx-service", fallbackMethod = "cachedRate")
@Retry(name = "fx-service")
public ExchangeRate getRate(String from, String to) {
    return fxClient.fetchRate(from, to);
}
```

- Retry only transient failures and only when the operation is idempotent or
  carries a safe idempotency key. Never blindly retry a financial write.
- Ignore business and validation exceptions so they do not trip a circuit.
- Use a fallback that preserves truth: cached/read-only data or an explicit
  unavailable result. Never report a financial write as successful from a
  fallback.
- Use a thread-pool bulkhead/time limiter only for the async API shape supported
  by the resolved library version. Keep timeouts, retry counts, and concurrency
  limits bounded and observable.

## Quarkus services

Use the Quarkus BOM and extensions already declared by the module. Resolve the
exact Quarkus version in Context7 before using REST, Reactive Messaging,
transactions, testing, or native-image APIs. Keep the same hexagonal, money,
outbox, idempotency, and security rules as Spring services; framework choice
does not relax financial invariants. Use the module's current Quarkus test
extension for endpoint and messaging tests instead of guessing compatibility
annotations.

## Testing and verification

Follow red-green-refactor for production changes: first write a failing test
that reproduces the behavior, then implement the smallest fix.

- Cover domain invariants exhaustively; target 80–90% for adapters and services.
- Test real behavior, not mock interactions. Use Testcontainers for PostgreSQL
  and Kafka integration where the production boundary matters.
- Add ArchUnit tests for layering and a concurrency/idempotency test for every
  financial mutation path.
- For transaction/outbox work, verify rollback leaves no business row or
  outbox row, and commit creates exactly one durable event record.
- Run the smallest relevant Maven module test first, then the service build and
  broader build when the change crosses modules.

## Observability and security

Use `logging-starter` and structured fields such as request ID, trace ID,
correlation ID, safe aggregate ID, outcome, error code, and duration. Never log
request bodies, credentials, raw tokens, PINs, or unmasked PII. Keep tracing,
health, metrics, and audit events enabled according to the service's existing
starter configuration.

## Platform workflow

Before a service-specific change, read its current POM and the relevant roadmap
or lesson entry; the compliance state changes over time and must not be copied
from a stale matrix. For flat-package services, refactor only when requested:
introduce domain model/ports, application use cases, persistence adapters,
`interfaces.dto`, ArchUnit coverage, and the required shared starters in that
order.

Read the detailed patterns only when needed:

- [Backend and JPA patterns](./references/BACKEND_PATTERNS.md)
- [Spring Boot patterns](./references/springboot-patterns.md)
- [Resilience patterns](./references/resilience_patterns.md)
- [Hexagonal architecture guide](./references/hexagonal_architecture_guide.md)
- [Database optimization guide](./references/database_optimization_guide.md)
- [Backend security practices](./references/backend_security_practices.md)
