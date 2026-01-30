# ADR-0013: Testing Strategy (Pyramid)

**Status**: Accepted
**Date**: 2026-01-30
**Deciders**: QA & Engineering

## Context

To ensure reliability and speed of delivery, we need a comprehensive testing strategy that balances coverage, speed, and cost.

## Decision

Implement the **Testing Pyramid** approach.

### Layers

1.  **Unit Tests (70%)**:
    - Scope: Single class/function.
    - Tools: JUnit 5 (Java), Jest (TS), Pytest (Python).
    - Requirement: 100% Logic Coverage. No I/O (Mock all external calls).
2.  **Integration Tests (20%)**:
    - Scope: Component interaction (DB, Kafka, Controller).
    - Tools: Testcontainers (PostgreSQL, Kafka), Spring Boot Test.
    - Requirement: Validates happy path and error handling with real dependencies.
3.  **Contract/Architecture Tests (5%)**:
    - Scope: API Contracts and Code Structure.
    - Tools: ArchUnit (Java), Pact (Contracts).
    - Requirement: Enforce Hexagonal Architecture rules.
4.  **E2E Tests (5%)**:
    - Scope: Full user flows.
    - Tools: Maestro (Mobile), Playwright/Cypress (Web), Gatling (Performance).
    - Requirement: Critical business flows only (Login, Transfer).

## Implementation

- CI Pipeline runs Unit + Arch tests on every commit.
- Integration tests run on PR merge.
- E2E tests run nightly or on release.

## Consequences

- **Positive**: Fast feedback loop, high confidence in changes.
- **Negative**: Maintenance of test infrastructure (Testcontainers resource usage).
