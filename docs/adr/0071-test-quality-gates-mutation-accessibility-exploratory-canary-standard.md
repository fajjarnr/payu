# ADR-0071: Test Quality Gate Formalization — Mutation Scoring, Accessibility Gating, Exploratory Ritual & Canary Deferral

**Status**: Accepted  
**Date**: 2026-08-26  
**Deciders**: Principal Architect, QA & Engineering  
**Relates to**: ADR-0013 (Testing Pyramid), ADR-0070 (E2E Environment Strategy), ADR-0066 (Pipeline Gates), ADR-0067 (deferral discipline precedent), ADR-0024 (Chaos Tiering)

---

## Context

Follow-up to the ADR-0070 environment review (2026-08-26) surveyed remaining industry-standard practices. The initial assessment flagged four gaps; **repository verification corrected two of them** — the tools exist, what is missing is the normative decision layer:

| Practice | Actual state (verified 2026-08-26) | What is missing |
|:---|:---|:---|
| Mutation testing | **Already present**: `backend/pom.xml:417-544` pins `pitest-maven 1.25.9` + `pitest-junit5-plugin 1.2.3`, configures `mutationThreshold 60` / `coverageThreshold 70`, off-by-default profile `mutation-testing` | No ADR states which services must reach which score, when it runs, or whether it gates anything |
| Accessibility | **Already present**: `@axe-core/playwright ^4.11.0` (`frontend/web-app/package.json:25`), scripts `test:a11y` (Vitest, dashboard components) and `a11y:audit` (`scripts/a11y-audit.ts`); WCAG 2.1 AA committed in `docs/product/PRD.md:171` | No enforcement scope: which flows gate a release, how pre-existing violations are tracked |
| Exploratory/manual testing | Undocumented | No defined ritual, owner, or evidence trail |
| Canary / progressive delivery | Absent — Argo Rollouts unused; deploys are rolling via ArgoCD GitOps | No recorded decision, so the question recurs |

Coverage percentages measure execution, not assertion strength: a 100%-covered core domain with weak assertions still ships money bugs. Mutation scoring closes exactly that blind spot — which matters given AGENTS.md rule 12 promises *100% core-domain coverage*. Meanwhile UU No. 8/2016 (disability rights) plus the PRD's own WCAG 2.1 AA commitment make accessibility a compliance surface, not a nicety.

Platform constraint (owner directive, 2026-08-26): **GitHub Actions is not used** — hosted runner minutes are billable. All CI execution stays on the in-cluster **Tekton** platform in `payu-cicd` (ADR-0066).

## Decision Drivers

- **Assertion quality**: convert coverage claims into verified defect-detection capability for financial logic (technical 40%).
- **Regulatory alignment**: WCAG 2.1 AA is already promised in PRD; UU 8/2016 exposure for banking apps (business 30%).
- **CI cost**: zero SaaS CI spend — every scheduled/gated job must run on in-cluster Tekton, never GitHub-hosted runners (business 30%).
- **Cost & sequencing**: PIT runtime across 19+ services; frontend currently carries open P1s (WEB-CSP-001 login broken, WEB-CSP-002 hydration/CSP) — gating releases on a11y now would block everything on pre-existing debt (team 30%).
- **Scale honesty**: single-cluster lab, rolling deploy blast radius currently small — canary buys little today.

## Considered Options

### Option 1 — Status quo (tools exist, ungoverned)

- **Pros**: zero effort; nothing breaks.
- **Cons**: mutation score stays a dormant number nobody is accountable for; a11y violations ship silently despite the PRD promise; the canary question resurfaces every quarter without a record; exploratory knowledge lives in individuals' heads.

### Option 2 — Adopt all four immediately with hard gates

- **Pros**: strongest posture on paper.
- **Cons**: PIT on every PR path roughly doubles backend CI time; an a11y release gate while login is broken (WEB-CSP-001, P1) halts delivery on legacy debt; canary requires multi-zone production traffic that does not exist — infrastructure for its own sake.

### Option 3 — Tiered formalization on in-cluster Tekton (chosen)

Codify the tools that exist into tiered norms, sequence the a11y gate behind the open P1s, make exploratory testing a lightweight documented ritual, defer canary with an explicit revisit trigger — all scheduled execution on Tekton `payu-cicd`, same deferral discipline as ADR-0067.

## Decision

Formalize five dispositions:

1. **PIT mutation testing** becomes a scored quality gate, off the PR path:
   - Core money-path services (**transaction, wallet, partner, account, auth**): mutation score **≥ 70%**.
   - All other Java services: **≥ 60%** (current parent threshold unchanged).
   - Execution: `mvn -Pmutation-testing` nightly / on-demand via a checked-in **Tekton** task in `payu-cicd` — **never** blocking PR builds (ADR-0013 cadence: heavier checks run nightly/release).
2. **axe-core accessibility (WCAG 2.1 AA)** becomes a release gate on **critical flows only** (login, onboarding, dashboard/transfer) — activated once WEB-CSP-001/002 close. Pre-existing violations live in an axe baseline file with mandatory burn-down; new pages must pass component-level Vitest a11y tests at PR time from now on.
3. **Exploratory testing** becomes a charter-based session (60–90 min, one tester, one charter) per release candidate before prod promotion; findings route to `docs/roadmap/TODOS.md`; session evidence appends to the UAT runbook — consistent with ADR-0070 placing human judgment in UAT.
4. **Canary / progressive delivery (Argo Rollouts)**: **DEFERRED (NO-GO today)**. Revisit trigger: production serving real multi-zone traffic where a bad rolling batch materially breaches SLO (ADR-0034 burn-rate signals) — reviewed at the next capacity planning after prod traffic instrumentation.
5. **CI execution platform**: in-cluster **Tekton (`payu-cicd`) only**. No GitHub Actions workflows — hosted runner minutes are billable, and the cluster already provides scheduling (CronJob/PipelineRun), secrets (Vault/ESO), and evidence routing. This applies to the nightly PIT task and any future quality-gate automation.

## Rationale

Weighted 40/30/30. Technical (40): mutation scoring is the cheapest instrument that makes the existing coverage claim falsifiable; scoping ≥70% to five money services keeps runtime proportional to risk. Business (30): a11y gating on critical flows satisfies the published WCAG commitment without letting legacy debt veto every release — baseline-with-burn-down is the standard escape hatch; running all gates on already-paid in-cluster Tekton keeps marginal CI cost at zero. Team (30): Option 3 changes zero dependencies (both tools already pinned), sequences around the open P1s instead of colliding with them, and defers rather than drops canary — the question is answered permanently instead of re-debated.

## Consequences

**Positive**:

- Mutation score becomes a tracked engineering metric alongside DORA gates in `docs/roadmap/PROGRESS.md`.
- A11y debt becomes visible, finite, and burnable; new UI cannot add to it silently.
- Exploratory findings get a durable trail (TODOS + UAT runbook) instead of tribal memory.
- The canary decision has a recorded trigger — governance stops relitigating it.
- Zero incremental CI spend: Tekton capacity is already provisioned and operated.

**Negative**:

- Nightly PIT adds compute load to the cluster (`payu-cicd` namespace); mitigated by keeping it off the PR path entirely and scheduling outside business-hour deploy windows.
- Two new artifacts need maintenance: axe baseline file and per-service mutation reports — L-260 discipline applies (fix baselines with the change that violates them, never let them rot silently).

**Risks**:

- Raising core services to ≥70% mutation score will likely expose weak assertions in the money path. That is the intended finding — remediation is scheduled test work, never threshold suppression.
- axe produces false positives on Radix portal/overlay components; mitigation is configuring specific known-safe rule exceptions per component, never disabling the engine globally.
- Tekton nightly load competes with image builds — mitigated by sequential PipelineRun scheduling (registry 429 pitfall already documented).

## Implementation Notes

1. **No new dependency anywhere** — verified present and pinned: `pitest-maven 1.25.9`, `pitest-junit5-plugin 1.2.3` (`backend/pom.xml`), `@axe-core/playwright ^4.11.0` (`frontend/web-app/package.json`). Context7 resolution confirms both are current, high-reputation projects (`/hcoles/pitest`, `/dequelabs/axe-core`).
2. Per-service threshold override: set `<mutationThreshold>70</mutationThreshold>` in the five core-money service poms (parent stays 60); schedule `-Pmutation-testing` via a checked-in Tekton CronJob/PipelineRun in `payu-cicd` following repo pitfalls (`volumeClaimTemplate`, never `claimName`; sequential runs).
3. A11y gate sequencing is blocked by WEB-CSP-001/002 (login broken, P1) — activate the release gate in the same change-set that closes those.
4. Add an "Exploratory Charter" template section to the UAT environment runbook under `docs/operations/`.
5. Record the canary revisit checkpoint in `docs/roadmap/TODOS.md` so the trigger is tracked, not remembered.

---
*Created via @principal-architect — refs backend/pom.xml:417-544, frontend/web-app/package.json:17-25, PRD.md:171, TODOS WEB-CSP-001/002, owner directive no-GitHub-Actions (2026-08-26), ADR-0013/0034/0066/0067/0070*
