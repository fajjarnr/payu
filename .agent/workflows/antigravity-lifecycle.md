---
description: Standard workflow for implementing features or fixing bugs using the Antigravity lifecycle (Planning -> Execution -> Verification) adapted for PayU.
---

# Antigravity Lifecycle for PayU

This workflow guides the AI agent through the standard Antigravity lifecycle, strictly enforcing PayU's engineering standards (TDD, Architecture, Security).

## 🌌 The Algorithm (Execution Engine)

**Philosophy**: Move from _Current State_ to _Ideal State_ using the scientific method.
**Goal**: Create "Euphoric Surprise" — deliver results that exceed expectations.

### Phase 0: Observe & Think (Pattern Recognition)

- **Context Loading**: "Have I seen this before?" (Check `docs/adr/`).
- **Ideal State Criteria (ISC)**: Define what "Perfect" looks like.
  - _Example_: "Not just clean code, but zero-config setup and 100% test coverage."

### Phase 1: Plan (Strategy Optimization)

**Goal**: Understand the requirements and design the solution.

1.  **Context Loading**
    - Read `CLAUDE.md` to understand the PayU ecosystem and rules.
    - Read `SKILL.md` in `.agent/skills/payu-development/` to load specific tech stack commands.
    - Read `PRD.md` and `ARCHITECTURE.md` if the task involves feature changes.

2.  **Analysis**
    - Identify the impacted services (e.g., `account-service` for banking logic, `billing-service` for payments).
    - Determine the architectural impact (Clean Architecture for Core, Layered for Supporting).
    - Check for existing "Skills" or similar patterns in Context7.

3.  **Implementation Plan**
    - Create `implementation_plan.md` in the artifact directory.
    - Define the **Goal Description**.
    - Detail **Proposed Changes** (New files, modified files, API changes).
    - Outline **Verification Plan** (Unit tests, Integration tests, Manual checks).
    - **CRITICAL**: Use `notify_user` to get approval before proceeding.

## Phase 2: Execution

**Goal**: Implement the changes using TDD and strictly following the plan.

1.  **Test-Driven Development (TDD)**
    - **RED**: Write the failing test first.
      - Unit Tests: `src/test/java/id/payu/<service>/service/`
      - Controller Tests: `src/test/java/id/payu/<service>/controller/`
      - Integration Tests: `src/test/java/id/payu/<service>/integration/`
    - Run the test to confirm failure: `mvn test -Dtest=YourTestClassName`.

2.  **Implementation**
    - **GREEN**: Write the minimal code to pass the test.
    - Follow conventions: `camelCase` variables, `PascalCase` classes, `kebab-case` files.
    - Use proper annotations (e.g., `@Service`, `@Transactional`, `@RestController`).

3.  **Refactoring**
    - **REFACTOR**: Improve code quality without changing behavior.
    - **Architecture Check**: Enforce **Hexagonal Architecture (Ports & Adapters)** for Core Banking.
      - **Domain Layer** (Inner Core): Entities, Business Rules, Repository _Interfaces_ (Output Ports). MUST NOT depend on Infrastructure/Frameworks.
      - **Application Layer**: Use Cases, Command/Query Handlers (Input Ports). Depends ONLY on Domain.
      - **Infrastructure Layer** (Output Adapters): JPA Implementations (Repositories), Kafka Producers, External Clients. Implements Ports.
      - **API Layer** (Driving Adapters): REST Controllers, gRPC. Calls Application Layer.

## Phase 3: Verification

**Goal**: Prove that the changes work and adhere to standards.

// turbo

1.  **Automated Verification**
    - Run all tests for the service: `mvn test` (Spring) or `./mvnw test` (Quarkus).
    - Run Architecture tests: `mvn test -Dtest=*Arch*`.
    - Check Code Coverage (if strictly required): `mvn test jacoco:report`.

2.  **Manual Verification**
    - If adding APIs, verify the endpoint structure matches `ARCHITECTURE.md`.
    - If modifying DB, verify migration scripts (Flyway) are correct.

3.  **Documentation**
    - Create `walkthrough.md` in the artifact directory.
    - Summarize changes.
    - Provide "Proof of Work" (Test results, screenshots if UI, logs).

## Phase 4: Sign-off

1.  **Update Task Status**
    - Mark all items in `task.md` as completed.

2.  **Notify User**
    - Present the `walkthrough.md`.
    - Ask for final review.
