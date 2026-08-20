# ADR-0048: Lending Eligibility and Pricing via DMN Decision Tables (ADR-0015 Phase 2)

**Status**: Accepted  
**Date**: 2026-08-20  
**Deciders**: Core Banking Engineering, Platform Engineering, Risk & Compliance  
**Relates to**: ADR-0015 (RHPAM/Kogito phased), ARCH-BESTP-003, CONTEXT.md §Lending

---

## Context

`backend/lending-service` credit scoring already externalized via Drools DRL `rules/credit_scoring.drl` (15 rules) through `shared/rules-starter/RulesEngineService.java:33` `classpath*:rules/**/*.drl` and `EnhancedCreditScoringService.java:72 fireRules`. 

Hardcoded `if-else` remains in two places, violating ADR-0015 Phase 2 intent (DMN decision tables):

* `LoanPreApprovalService.java:41` `MIN_CREDIT_SCORE_FOR_APPROVAL 650` / `MIN_CREDIT_SCORE_FOR_CONDITIONAL 600`, `:144` `calculateInterestRate` tiers `750→12% / 700→14% / 650→16% / else 18%`, `:156` `calculateConditionalAmount` `640→0.85 else 0.70`, `:113` `maxTenure 24`, `determineRiskCategory` `EXCELLENT..VERY_POOR`
* `LendingApplicationService.java:261` `isEligibleForLoan`, `:269` duplicate `calculateInterestRate`, fallback `0.18 + 5M` hardcode

Duplicate DRL fork `backend/lending-rules/src/main/resources/id/payu/lendingrules/rules/credit_scoring.drl` with package `id.payu.lendingrules.domain.CreditScoringFact` drifts from `id.payu.lending.domain.model.CreditScoringFact`. `CONTEXT.md` had no lending glossary, causing overload between `CreditScore` / `CreditScoringFact` / `RiskCategory` / `PreApproval`.

OJK/BI threshold changes (e.g. 650→670) require <1h updates without full Java deploy.

## Decision Drivers

* **Compliance agility**: risk/compliance can change thresholds/tiers without Java rebuild.
* **Auditability**: DMN tables versioned in Git, diff-readable, FEEL standard.
* **Single source**: one `rules-starter` for DRL+DMN, remove fork.
* **Separation**: additive scoring (DRL) vs deterministic eligibility/pricing (DMN) vs installment math (Java `BigDecimal HALF_EVEN`).
* **Zero infra Phase 2**: classpath first, `KieScanner` later.

## Considered Options

### Option A — DMN for eligibility/pricing, DRL keep scoring (chosen)

* **Pros**: DMN `FEEL` tables readable by risk team, standard-based, `DMN exec-model` fast; DRL best for additive `addScore`; incremental from current `rules-starter`; aligns with `kie-docs` guidance (DMN = shareable graphical, DRL = max flexibility).
* **Cons**: need `DMNRuntime` wiring, team learns FEEL.

### Option B — DRL for all

* **Pros**: single DRL style.
* **Cons**: tables as `when/then` less readable for compliance, no graphical, harder audit.

### Option C — Kogito BPMN now

* **Pros**: full workflow orchestration.
* **Cons**: heavy (Business Central, KIE Server), overkill for decision-only; Phase 3 per ADR-0015.

## Decision

**DMN decision tables for eligibility and pricing; DRL stays for credit scoring; math stays Java.**

* `backend/lending-service/src/main/resources/rules/credit_scoring.drl` stays (scoring).
* New `backend/lending-service/src/main/resources/rules/dmn/pricing.dmn` and `eligibility.dmn` (DMN 1.4, FEEL, hit policy `UNIQUE`):
  * `pricing.dmn` — input `creditScore` (Decimal) → `PricingOutput {interestRate: Decimal, riskCategory: string}` — rows: ≥750→12%/EXCELLENT, 700-749→14%/GOOD, 650-699→16%/FAIR, 600-649→18%/POOR, <600→18%/VERY_POOR
  * `eligibility.dmn` — inputs `creditScore, requestedAmount, requestedTenure` + invocation `pricing(creditScore)` → `EligibilityOutput {status: PreApprovalStatus, maxApprovedAmount: Decimal, maxTenureMonths: number, reason: string}` — rows: ≥650 APPROVED (`requestedAmount`, `requestedTenure`), 600-649 CONDITIONALLY_APPROVED (`creditScore≥640?amount*0.85:amount*0.70`, `min(tenure,24)`), <600 REJECTED (`0,0`)
* `lending-rules` module deleted; single source `lending-service/rules/`.
* `RulesEngineService` extended to scan `classpath*:rules/dmn/*.dmn` and evaluate via `DMNRuntime` (`kieContainer.getKieRuntime(DMNRuntime.class)`); `KieScanner` (`kie-ci`) for SNAPSHOT/file hot-reload next iteration.
* Glossary in `CONTEXT.md` §Lending: `CreditScoringFact` (transient), `CreditScore` (persisted), `RiskCategory`, `PreApproval`, `PreApprovalStatus`, `LoanApplication`, `PricingTier`.

## Rationale

Maps to drivers: DMN tables give compliance self-service diffs in Git, FEEL deterministic, versioned. DRL remains optimal for additive scoring (`addScore`). BPMN deferred per ADR-0015 Phase 3 (loan origination workflow). Classpath-first avoids infra; `KieScanner` path proven in `kie-docs` (`kScanner.start(10000L)`, `updatePolicy always`) for later hot-reload without redeploy.

## Consequences

**Positive**:

* Threshold/rate changes = DMN edit + `git push`, no Java change; audit via Git + `security-starter` `DMN evaluation` log.
* Eliminates duplicate pricing logic and fork drift.
* FEEL tables testable with boundary values `600/640/650/700/750`.

**Negative**:

* Team must learn DMN FEEL and `DMNRuntime` API — mitigated by `rules-starter` helper `evaluateEligibility(...)`.
* DMN exec-model adds ~5MB `kie-dmn` dep.

## Implementation Notes

| Step | Target | File / Action |
|---|---|---|
| 1 | Glossary | `CONTEXT.md` §Lending (done) |
| 2 | Backlog | `docs/roadmap/TODOS.md` ARCH-BESTP-003 ref this ADR |
| 3 | DMN | `lending-service/src/main/resources/rules/dmn/pricing.dmn`, `eligibility.dmn` (DMN 1.4, namespace `http://payu.id/dmn`) |
| 4 | Engine | `rules-starter/RulesEngineService.java` + `RulesAutoConfiguration.java` scan `.dmn`, expose `DMNRuntime` bean, `kie-dmn` + `kie-ci` deps |
| 5 | Wiring | `LoanPreApprovalService` + `LendingApplicationService` call `pricing`+`eligibility` DMN, remove hardcoded `calculate*` |
| 6 | Delete | `backend/lending-rules/` module + `id.payu.lendingrules.domain.CreditScoringFact` |
| 7 | Tests | Boundary tests 599/600/639/640/649/650/700/750, `BigDecimal HALF_EVEN`, `ArchUnit` no hardcoded tiers |

**Verification**: `LoanPreApprovalServiceTest` green for 600→REJECTED, 640→CONDITIONAL 0.85, 600→0.70, 650→APPROVED; `RulesEngineService` DMN eval `<10ms`; `git diff` on `.dmn` readable.

---
*Created for ARCH-BESTP-003 — implementasi wajib refer ADR-0015 + ADR ini.*
