---
name: quality-engineer
description: PayU quality engineering for Java/Spring and Quarkus services, Python APIs, Next.js and React Native clients, contract testing, integration testing, financial invariants, performance, accessibility, security, migration, and release gates. Use when designing, writing, reviewing, debugging, or improving tests; verify third-party test APIs and commands with Context7 first.
---

# PayU Quality Engineer

Test observable behavior at the cheapest layer that can detect the risk. Start
with a failing test for a bug or feature, then implement the smallest change
that makes it pass. A green test that accepts the wrong behavior is a defect.

## Context7 documentation gate

Before writing or changing a test, fixture, runner, plugin, configuration, or CI
command that uses a library or service:

1. Inspect the target module's `pom.xml`, `package.json`, lockfile, Python
   requirements, test config, and existing test command.
2. Resolve the exact library in Context7. Prefer official, high-reputation docs
   and query one topic at a time: API, fixture, assertion, lifecycle,
   configuration, migration, or CLI behavior.
3. Pin the query to the repository version when indexed. Treat the result as the
   source of truth; do not copy annotations or command flags from memory.
4. If the exact version is not indexed, state the fallback and verify against
   the local dependency graph, source, or installed CLI before editing.
5. Re-resolve after dependency upgrades. Do not mix major-version APIs.

Apply this gate to JUnit/Jupiter, Mockito, Spring Boot test slices, Quarkus test
extensions, Testcontainers, ArchUnit, Spring Cloud Contract/Pact, Playwright,
Vitest/React Testing Library, Jest/React Native Testing Library, Maestro, k6,
Gatling, pytest, OWASP ZAP, and mutation-testing tools.

## Repository map

Locate the real test surface before choosing a layer:

```text
backend/*/src/test/java             Java unit, slice, integration, ArchUnit
backend/*/tests                      Python unit, integration, and E2E
tests/contract                       Spring Cloud Contract sources
tests/performance                    Gatling simulations and k6 suites
tests/e2e_blackbox                   Python black-box service journeys
frontend/web-app/e2e                 Playwright web journeys
frontend/web-app/src                 Vitest + React Testing Library
frontend/mobile                      Jest + React Native Testing Library/Maestro
```

Read the nearest existing test, fixture, runner, and module build file. Reuse
its factories, container setup, auth fixture, test data, and naming conventions.
Do not create a second test harness for the same boundary.

## Test strategy

Use a risk-based pyramid, not fixed percentages copied between services:

| Layer | Use for | Default rule |
|---|---|---|
| Domain unit | Invariants and deterministic logic | Fast, framework-free, exhaustive |
| Adapter/slice | Mapping, validation, HTTP, persistence wiring | Verify real framework behavior at the boundary |
| Integration | PostgreSQL, Kafka, Redis, Vault, outbox, migrations | Use the real dependency in a disposable environment |
| Contract | Provider/consumer compatibility | Verify request, response, headers, errors, and versioning |
| E2E | Critical user journeys | Keep few, stable, and user-visible |
| Performance | Capacity, latency, saturation, failure behavior | Run controlled scenarios against an approved environment |

Target 100% coverage for core domain logic and 80–90% for other code, but use
coverage as a gap signal rather than proof of correctness. Prioritize money,
authorization, idempotency, migrations, messaging, and failure paths over trivial
getters or framework bootstrapping.

## Java backend testing

### Unit and domain tests

- Use JUnit Jupiter and the repository's assertion library; keep test instances
  isolated and avoid mutable static state.
- Test domain objects without Spring, JPA, Kafka, HTTP, or Mockito.
- Use parameterized tests for amount boundaries, status transitions, invalid
  states, and authorization matrices.
- Mock only an outbound port when a real adapter would move the test to another
  layer. Assert the returned result and state transition; do not make `verify()`
  the only assertion.
- Compare monetary values by numeric value, not scale-sensitive object equality.

### Spring and Quarkus boundaries

- Use the current Spring Boot slice or test module declared by the service. Do
  not copy `@MockBean`, slice annotations, or auto-configuration exclusions
  across Spring major versions without a Context7 check.
- Test validation, RFC 9457 errors, security decisions, headers, serialization,
  and transaction boundaries at the adapter level.
- Use `@QuarkusTest` and Quarkus test extensions only in Quarkus modules and
  only with the current extension/API documented for that module.
- Keep ArchUnit rules in every service with hexagonal boundaries: domain does
  not depend on adapters/frameworks, controllers call input ports, and adapters
  implement output ports.
- Test shared starters with a focused application-context test instead of
  booting every service.

### Integration and database tests

Use Testcontainers for behavior that depends on PostgreSQL, Kafka, Redis, or
another production boundary. Use the modules and artifact names resolved from
the service's BOM; never paste a stale image or module version into a test.

- Keep containers disposable and isolated; avoid shared mutable databases.
- Apply Flyway migrations against PostgreSQL rather than relying only on H2.
- Verify indexes, constraints, optimistic locking, transaction rollback, and
  serialization of `DECIMAL(19,4)` values.
- For Kafka/outbox tests, prove the business row and outbox row commit together,
  disappear together on rollback, and publish an idempotently consumable event.
- Use Testcontainers only for the boundary under test; do not turn every unit
  test into a full application boot.

## Financial integrity and security tests

Every payment, transfer, ledger, balance, settlement, and disbursement change
must test:

- `BigDecimal` only; no `float` or `double` in financial code;
- scale 4 and `RoundingMode.HALF_EVEN` with half-way and large-value cases;
- double-entry balance: total debits equal total credits;
- non-negative balance and all domain authorization limits;
- immutable financial facts and reversal-based correction;
- database uniqueness, optimistic-lock/concurrency behavior, and transaction
  atomicity;
- `X-Idempotency-Key`: ten concurrent identical requests create one mutation,
  return the same result, and do not duplicate ledger/outbox entries;
- reuse of one key with a different payload is rejected;
- CloudEvent fields, versioned topic, ordering key, retry, and DLQ behavior;
- masked PII, no secrets in fixtures/logs, and authorization at every boundary.

Use deterministic account IDs and amounts in tests. Never use real customer data,
real PINs, reusable production tokens, or uncontrolled random financial writes.

## Contract testing

Use the contract technology already declared by the module. This repository
contains Spring Cloud Contract sources under `tests/contract`; use Pact only
where a module explicitly owns that dependency.

- Treat the consumer request and provider response as a versioned public API.
- Cover success, validation, authorization, RFC 9457 errors, idempotency
  headers, pagination, and backward-compatible field changes.
- Verify provider states with isolated data and no external production calls.
- Publish contracts and run the provider verification/compatibility gate before
  promotion. A generated stub is not proof that the deployed provider is safe.
- Contract event payloads and CloudEvents separately from HTTP contracts when
  Kafka or other messaging is part of the integration.

## Web, mobile, and accessibility testing

### Next.js web app

Use the versions and scripts in `frontend/web-app/package.json`:

- Use Vitest + React Testing Library for component behavior. Query by role,
  label, and accessible name; use `user-event` for user interaction.
- Test what a user can see and do, not React state, implementation methods, or
  CSS class names. Mock network boundaries, not local child components by
  default.
- Use Playwright for critical journeys. Prefer semantic locators and web-first
  assertions; let locators auto-wait instead of adding arbitrary sleeps.
- Isolate tests with fixtures and fresh browser contexts. Store auth setup in a
  fixture, not a login flow repeated in every test.
- Capture traces on retry/failure and screenshots only when they help diagnosis.
- Run axe-based accessibility checks, then verify keyboard navigation, focus,
  labels, dialog semantics, contrast, and error announcements.

### React Native mobile app

Use Jest/React Native Testing Library for component and user behavior, and
Maestro only for a small set of critical flows (login, transfer, payment,
logout, deep link). Keep credentials and PINs in CI secrets. Test offline,
resume, interrupted navigation, secure storage, permission denial, and
idempotent retry behavior.

### Black-box API E2E

Use the existing pytest fixtures and clients under `tests/e2e_blackbox` or the
service's own test package. Separate infrastructure unavailability from a real
product failure: skip only a clearly identified 429/502/503/504 dependency
outage when the test contract permits it; fail on unexpected 4xx/5xx responses.
Never make `200` and `500` equivalent passing outcomes.

## Performance and capacity

Use the existing suites under `tests/performance` and `tests/load-tests` rather
than creating another load-test tree. Verify which source directory the selected
Maven profile actually compiles before running it.

Model at least these scenarios:

1. Smoke: one or a few users, short duration, detects broken wiring.
2. Load: expected traffic mix and sustained duration.
3. Stress: increasing load until a bounded failure point.
4. Soak: long duration to expose leaks, queue growth, and degradation.

Measure p50/p95/p99 latency, error rate, throughput, saturation, queue lag,
database contention, and downstream failures. Set thresholds from the service
SLO and capacity evidence, not global numbers copied from another service.

For financial writes, use unique `X-Idempotency-Key` values for distinct test
operations and repeat the same key only when testing idempotency. Keep test data
bounded and reversible; do not run destructive load against production.

Run performance tests in an environment with known topology and resource
limits. Record commit, image, dataset, concurrency, duration, warm-up, and
results so a baseline is reproducible.

## Migration and regression testing

Before Quarkus/Spring, framework, database, or schema migration:

1. Add characterization tests for current externally visible behavior.
2. Freeze HTTP/event contracts and financial invariants.
3. Migrate one boundary at a time; do not mechanically map annotations.
4. Run old/new parity tests with sanitized fixtures and compare errors,
   serialization, timing budgets, and side effects.
5. Verify migration rollback, replay/idempotency, and data reconciliation.
6. Remove compatibility code only after the deployment window closes and the
   rollback path is no longer required.

## CI and failure handling

Run the narrowest relevant check first, then broaden:

```text
failing test -> module test -> service integration/contract tests
             -> frontend/mobile checks -> performance/security gates
             -> release/E2E verification
```

Use repository commands such as `make test` and
`./scripts/test-single-service.sh <service>`; read the script before assuming
its filters or environment. Run independent suites in parallel only when they
do not share ports, containers, databases, files, or mutable fixtures.

Do not:

- delete or weaken a failing test to make CI green;
- accept broad status-code ranges that hide defects;
- add arbitrary sleeps, retries, or timeouts to mask races;
- mock the system under test or every child component;
- commit secrets, production data, or tokens in fixtures;
- claim coverage, performance, or release readiness without command evidence.

Quarantine a genuinely flaky test only with an owner, failure evidence, an
expiry/removal condition, and a separate signal that still reports the failure.

## Quality checklist

- [ ] A failing test existed first for the requested bug/behavior.
- [ ] Test layer matches the risk and uses the real boundary where required.
- [ ] Core domain invariants and all failure paths are covered.
- [ ] Financial precision, ledger balance, idempotency, concurrency, outbox,
      and reversal behavior are verified where applicable.
- [ ] Contracts cover compatible requests, responses, errors, headers, and
      events.
- [ ] E2E tests use stable user-visible selectors and isolated fixtures.
- [ ] Accessibility and security checks run for relevant UI/API changes.
- [ ] Performance thresholds come from current SLO/capacity evidence.
- [ ] Test output, environment, commit/image, and known limitations are recorded.

## References

Read only the matching reference:

- [Testing patterns](./references/TESTING_PATTERNS.md)
- [Performance baselines](./references/performance-baselines.md)

Treat other bundled references as secondary until their project scope and
dependency versions are verified against the target module.
