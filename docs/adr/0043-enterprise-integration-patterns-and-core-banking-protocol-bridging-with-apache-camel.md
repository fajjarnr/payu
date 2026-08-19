# ADR-0043: Enterprise Integration Patterns & Core Banking Protocol Bridging with Apache Camel

**Status**: Accepted  
**Date**: 2026-08-19  
**Deciders**: Principal Architect, Integration Architect, Core Banking Lead  
**Relates to**: ADR-0029 (ISO 20022), ADR-0041 (Outbox), integration-service  

---

## Context

PayU must bridge core banking protocols: `BI-FAST` ISO 20022, `OJK` reporting, `SWIFT` MT, `SNAP-BI` REST. `integration-service/src/main/java/id/payu/integration/adapter/camel/route/OjkRouteBuilder.java` + `SwiftRouteBuilder.java` + `*Transformer/*Validator` already implement Camel EIP, tested by `ArchitectureTest.java:RouteBuilder` reside in `..adapter.camel..` and `NoDirectKafkaEndpointTest` (no `direct kafka`).

Bank pattern: `Content Enricher`, `Message Translator` (ISO 20022 ↔ SNAP-BI), `WireTap` for audit, `CircuitBreaker`, `Idempotent Consumer` by `messageId`.

## Decision Drivers

* **Standard EIP catalog** — 65+ patterns, not bespoke queue code.
* **Protocol mediation** — `ISO 20022` → `SNAP-BI` JSON + vice versa with validation.
* **Testability** — `CamelTestSupport` + `WireMock` already in `integration-service`.

## Considered Options

### Option 1 — Apache Camel 4.x on Spring Boot (dipilih)

Pros: 300+ components, EIP DSL, `resilience-starter` hook, existing `integration-service`. Cons: extra `camel-context` memory — mitigasi `lazy-load`.

### Option 2 — Bespoke HTTP+Kafka glue

Pros: no framework. Cons: reinvents EIP, no pattern audit — ditolak.

## Decision

**Apache Camel 4.x** as PayU `integration-service` bridging standard.

* Route: `from(outbox Kafka payu.clearing.*.v1) → validate → transform ISO20022 ↔ SNAP-BI → enrich → to(BI-FAST/OJK/SWIFT) → wireTap(audit)`.
* Config: `CamelContext` `autoStartup=true`, `errorHandler(deadLetterChannel(payu.integration.dlq))`, `Hystrix/CB` via `resilience-starter`.
* `RouteBuilder` must reside in `..adapter.camel..`, must not publish directly to `kafka` (outbox only) — enforced by `ArchUnit`.
* TLS: `mTLS` to BI/OJK via `Vault`-managed cert (ADR-0044).

## Consequences

**Positive**: governed bridging, testable EIP, DLQ.

**Negative**: Camel learning curve — mitigasi template `OjkRouteBuilder`.

## Implementation Notes

| Step | Target | File |
|---|---|---|
| 1 | Routes | `integration-service/.../adapter/camel/route/*RouteBuilder.java` |
| 2 | Transform | `.../adapter/camel/transformer/SwiftTransformer.java` |
| 3 | Tests | `Camel/SwiftValidatorTest`, `NoDirectKafkaEndpointTest` |

**Verification**: `WireMockIntegrationTest` ISO 20022 → SNAP-BI round-trip green; `ArchitectureTest` green.

---
*Created for integration-service EIP — implementasi wajib refer ADR ini.*
