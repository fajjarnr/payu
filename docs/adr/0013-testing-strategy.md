# ADR-0013: Testing Strategy (Pyramid)

**Status**: Accepted
**Date**: 2026-01-30
**Last Updated**: 2026-03-11
**Deciders**: QA & Engineering

## Context

To ensure reliability and speed of delivery, we need a comprehensive testing strategy that balances coverage, speed, and cost.

## Decision

Implement the **Testing Pyramid** approach.

### Layers

1. **Unit Tests (70%)**:
    - Scope: Single class/function.
    - Tools: JUnit 5 + Mockito (Java), Vitest (TS), Pytest (Python).
    - Requirement: 100% Logic Coverage. No I/O (Mock all external calls).
2. **Integration Tests (20%)**:
    - Scope: Component interaction (DB, Kafka, Controller).
    - Tools: Testcontainers (PostgreSQL, Kafka), Spring Boot Test.
    - Requirement: Validates happy path and error handling with real dependencies.
3. **Contract/Architecture Tests (5%)**:
    - Scope: API Contracts and Code Structure.
    - Tools: ArchUnit (Java) — 18/19 services, Pact (Contracts) — configured.
    - Requirement: Enforce Hexagonal Architecture rules.
4. **E2E Tests (5%)**:
    - Scope: Full user flows.
    - Tools: **Playwright** (Web/OCP), **Pytest Blackbox** (API/Local), K6 (Performance).
    - Requirement: Critical business flows only (Login, Transfer).

### Current Test Results (Mar 2026)

| Layer        | Framework         | Status                                |
| :----------- | :---------------- | :------------------------------------ |
| E2E (OCP)    | Playwright        | ✅ 399/399 pass                       |
| E2E (Local)  | Pytest Blackbox   | ✅ 103 pass, 55 skip, 0 fail          |
| Performance  | K6                | ✅ CRUD + stress + consistency suites |
| Contract     | Pact              | ✅ Configured                         |
| Integration  | Testcontainers    | ✅ Per service                        |
| Architecture | ArchUnit          | ✅ 18/19 services                     |
| Unit         | JUnit 5 + Mockito | Varies per service                    |

> **Amendment (Mar 11, 2026)**: Original tools listed Maestro (Mobile), Cypress (Web), and Gatling (Performance). Actual implementation uses Playwright for web E2E (399 tests on OCP), Pytest Blackbox for API E2E (103 tests locally), and K6 for performance testing (CRUD, stress, data consistency suites). Gatling is configured but K6 is the primary performance tool. Mobile testing with Maestro is deferred with mobile app.

## Implementation

- CI Pipeline runs Unit + Arch tests on every commit.
- Integration tests run on PR merge.
- E2E tests run nightly or on release.
- K6 performance tests: `tests/performance/k6/` with shared lib (auth, wallet, transaction, card).

## Consequences

- **Positive**: Fast feedback loop, high confidence in changes.
- **Negative**: Maintenance of test infrastructure (Testcontainers resource usage).
