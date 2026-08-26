# ADR-0070: E2E Test Environment Strategy — Tiered Local → SIT → Preprod Smoke

**Status**: Accepted  
**Date**: 2026-08-26  
**Deciders**: Principal Architect, QA & Engineering  
**Relates to**: ADR-0013 (Testing Pyramid), ADR-0023 (MVP Scope — E2E live di `payu-dev`), ADR-0024 (Tiered Chaos — same tiering philosophy), ADR-0056 (Simulator Fidelity & Contract Testing), ADR-0066 (Polyrepo Pipeline Gates)  
**Complements**: `docs/operations/INFRASTRUCTURE_DEPLOYMENT.md` (env topology, promotion pipeline, traceability table)

---

## Context

ADR-0013 defines the E2E layer of the testing pyramid (5%, critical flows only) and its tools — **Playwright** (Web/OCP, 399/399 pass), **Pytest Blackbox** (`tests/e2e_blackbox`, 103 pass / 55 skip / 0 fail), K6 (performance) — but does **not** normatively pin *which environment* each suite targets. This produced ambiguity (raised 2026-08-26): "E2E dijalankan di UAT?"

Current de-facto wiring:

| Suite | Current target | Evidence |
|:---|:---|:---|
| Pytest Blackbox API | Local podman stack by default | `tests/e2e_blackbox/conftest.py` — `os.getenv("GATEWAY_URL", "http://localhost:8080")`; skips all if gateway unreachable ("Start services first: make podman-test-up"); sends `X-E2E-Test` header for rate-limit bypass |
| Playwright Web | Local podman web-app by default; OCP runs historically on `payu-dev` | `frontend/web-app/playwright.config.ts` — `baseURL: process.env.PLAYWRIGHT_BASE_URL \|\| 'http://localhost:3001'`; ADR-0023 MVP acceptance criterion "E2E live di payu-dev"; ADR-0013 records "E2E (OCP) Playwright ✅ 399/399" |
| Cluster-facing E2E/DAST/fuzzing | SIT only | `INFRASTRUCTURE_DEPLOYMENT.md` §Execution Rules item 10 — SIT exposes gateway Route `gateway-sit.apps.fajjjar.my.id` (edge TLS) explicitly for DAST/fuzzing/E2E |

Platform topology is five environments (`payu-dev`, `payu-sit`, `payu-uat`, `payu-preprod`, `payu`) promoted exclusively via `payu-deploy-gitops-pipeline` (Tekton, SIT → UAT → preprod → prod). Industry practice separates **automated regression/E2E** (machine-paced, production-parity, synthetic data) from **UAT** (human-paced business sign-off on stable state). Without a normative decision, the two collide: mutating E2E suites create-and-delete resources continuously, which pollutes the state business stakeholders validate during acceptance.

## Decision Drivers

- **Environment parity**: an E2E result is only meaningful against configuration mirroring production (technical 40%).
- **Isolation of concerns**: UAT state must stay stable for business sign-off; automated mutation churn must not run there (business 30%).
- **Automation cadence**: E2E gates must run nightly/on-release without a human gate (DORA lead time & change-failure-rate support) (technical).
- **Compliance evidence**: OJK/BI audit needs per-environment gate evidence routed through the existing runbook traceability table (business).
- **Team reality**: current wiring already matches a tiered model; the decision should codify, not re-platform (team 30%).

## Considered Options

### Option 1 — Full E2E suite in UAT

- **Pros**: one fewer environment to wire; UAT is production-like so parity holds.
- **Cons**: mutating suites corrupt acceptance data mid-sign-off; cadence mismatch (CI nightly vs human-paced approval); conflates regression signal with business acceptance; audit trail mixes automated noise with human evidence.

### Option 2 — Single dedicated staging for everything (E2E + perf + chaos)

- **Pros**: cheapest cluster footprint; one parity surface.
- **Cons**: K6 load and LitmusChaos fault injection (ADR-0024 runs those in SIT) would starve or destabilize E2E runs sharing the namespace; serializing all heavy testing behind one env lengthens lead time.

### Option 3 — Tiered matrix (chosen)

Local podman for PR feedback → **SIT as canonical automated E2E/perf/DAST environment** → UAT restricted to smoke + manual acceptance → preprod smoke/migration rehearsal → prod read-only synthetic probes. Matches existing wiring exactly (conftest defaults, SIT gateway route, MVP criteria on `payu-dev`) and mirrors ADR-0024's tiered philosophy.

## Decision

Adopt the **tiered E2E environment matrix**:

| # | Suite / activity | Canonical target | Trigger | Mutating allowed |
|:---|:---|:---|:---|:---|
| 1 | Pytest Blackbox API | Local podman stack (`GATEWAY_URL` default `http://localhost:8080`) | PR-local / dev loop | Yes (own synthetic data) |
| 2 | Pytest Blackbox API — cluster gate | **SIT** (`GATEWAY_URL=https://gateway-sit.apps.fajjjar.my.id`) | Nightly + release gate | Yes (synthetic, uuid-per-run) |
| 3 | Playwright Web | Local podman `:3001`; OCP run against `payu-dev` route | Nightly + release gate | Yes (own users) |
| 4 | K6 performance | SIT only | Release candidate | Yes |
| 5 | Smoke (happy path subset) | UAT | Release sign-off window | Read-mostly only |
| 6 | Migration dry-run + DR rehearsal smoke | preprod | Promotion to prod | Per runbook |
| 7 | Post-deploy synthetic probes | prod | Every deploy | **No — read-only** |

**Normative rules**:

1. Full mutating automated E2E suites are **never** run against UAT, preprod, or prod. UAT automation is capped at the smoke subset (rule 5).
2. **SIT is the canonical automated E2E environment.** Parity contract: E2E targets only image digests promoted by `payu-deploy-gitops-pipeline` — never hand-deployed builds.
3. Local podman runs are fast-feedback only; they are never a standalone release signal.
4. Prod gets only non-mutating synthetic checks (health, auth roundtrip, read endpoints) after deploy — anything else requires a change window and CAB approval.
5. Every cluster-tier E2E run appends its evidence to the owning environment runbook per the architecture-to-runbook traceability table (`INFRASTRUCTURE_DEPLOYMENT.md`).

## Rationale

Weighted 40/30/30. Technical (40): Option 3 is the only option that preserves both parity (same digests as production promotion path) and isolation (perf/chaos/E2E share SIT deliberately, serialized by schedule, instead of fighting over one mixed staging). Business (30): keeps UAT sign-off defensible for OJK/BI audits — acceptance evidence stays human-authored, while automated evidence lives in SIT/UAT runbooks already mapped in the traceability table. Team (30): zero re-platforming — conftest defaults, the SIT gateway route, and the `payu-dev` Playwright precedent already implement this matrix; the ADR converts de-facto into normative. Option 1 was rejected for audit-integrity risk; Option 2 for contention and lead-time cost.

## Consequences

**Positive**:

- Unambiguous answer to "which env?" for every suite; new engineers onboard without tribal knowledge.
- UAT acceptance state stays stable; business sign-off evidence stays clean.
- Nightly SIT gate strengthens DORA change-failure-rate measurement (failures caught pre-UAT) — metrics already tracked in `docs/roadmap/PROGRESS.md`.
- Zero infra cost: uses existing five-env topology and existing routes.

**Negative**:

- SIT becomes a shared bottleneck for E2E + K6 + chaos + DAST scheduling; mitigated by nightly windows and the fact that all four are release-gated, not commit-gated (ADR-0013: E2E runs nightly or on release).
- Two-tier blackbox results (local vs SIT) can diverge when SIT carries real Keycloak/3scale/WAF edges; that divergence is information, not noise — triage belongs in `docs/guides/LESSONS.md`.

**Risks**:

- Flaky nightly runs erode trust in the gate → quarantine policy: failing-but-unrelated specs get a documented skip with a TODOS entry, fixed with the next change touching that flow (**L-260**: stale E2E tests rot faster than unit tests — fix them with the change).
- Parity drift if someone deploys an un-promoted image to SIT → already structurally prevented: Argo sync/prune/self-heal plus the single Tekton promotion path.

## Implementation Notes

1. **No code change required** — this ADR codifies existing wiring (`conftest.py` default, SIT route, Playwright baseURL). It is a governance record.
2. Cluster-gate invocation (the only new operator knowledge):
   ```bash
   cd tests/e2e_blackbox
   GATEWAY_URL=https://gateway-sit.apps.fajjjar.my.id ./run_tests.sh -t all
   ```
3. Nightly schedule: checked-in Tekton CronJob/PipelineRun in `payu-cicd` following the repo pitfall rules (use `volumeClaimTemplate`, never `claimName`; sequential to avoid registry 429s). Evidence line appended to the SIT/UAT runbooks per run.
4. Test data discipline already satisfied: `conftest.py` generates uuid-unique identities per run (`user_{uuid}@payu.fajjjar.my.id`); cleanup best-effort per suite README.
5. `tests/e2e_blackbox/README.md` prerequisites section should reference this ADR when next touched (L-260: fix docs with the change, not in a sweep).

---
*Created via @principal-architect — refs ADR-0013/0023/0024/0056/0066, `tests/e2e_blackbox/conftest.py`, `frontend/web-app/playwright.config.ts`, `INFRASTRUCTURE_DEPLOYMENT.md` §Execution Rules:104 + Promotion Pipeline, LESSONS L-260*
