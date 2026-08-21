# ADR-0067: LLM Integration for PayU Services — RAG, Guardrails & Private Deployment Standard

**Status**: Proposed  
**Date**: 2026-08-22  
**Deciders**: AI Engineer, Core Banking Engineering, Platform Engineering, Risk & Compliance, DPO  
**Relates to**: ADR-0036 (Python FastAPI), ADR-0040 (Encryption), ADR-0046 (TimescaleDB), ADR-0063 (Compliance), ADR-0064 (3scale), NIST AI RMF, OWASP LLM Top 10

---

## Context

PayU owns 4 natural LLM surfaces today (codegraph 2026-08-22):

* `support-service` (`Support Ticket/FAQ` `ADR-0051`) — `~5k tickets/mo` manual triage, `promotion-service` `ADR-0055` copy, `compliance-service` `ADR-0063` audit summarization, `statement-service` `ADR-0019` spend summary, `kyc-service` doc extraction (`ADR-0036` `paddleocr`) — semua masih `rule/heuristic` tanpa LLM.
* `analytics-service` `fraud/score` + `kyc` already Python FastAPI `torch` but no LLM pipeline; `TimescaleDB` hypertables `ADR-0046` ready for `pgvector` extension.
* Constraints lab: **no >10k evt/s**, **no GPU farm**, `payu-dev` 23 services already `ExceededNodeResources` (`DEVSECOPS_ARCHITECTURE.md:531`), data residency `UU PDP` + `PCI-DSS Req10` + `Basel III` — prompts **must not leak** `Email/T2 NIK/Phone/Bank Account/Monetary` (UCL BPPD 6 PII taxonomy) to `honest-but-curious` external LLM.

Industry 2026: UCL BPPD `DetectorLLM (Llama-3.2-3B LoRA 99.9% safety)` + `FF3-1 FPE` + proxy Separation-of-Concerns on single `RTX 4090` (Kong et al.); `ABS Handbook GenAI Guardrails 2026` `RAG + filtering + human-in-loop + temperature 0`; `DSE July 2026` `prompt injection LLM01 / sensitive disclosure LLM02 / excessive agency LLM06` + `GLBA + NIST AI RMF GOVERN/MAP/MEASURE/MANAGE + June 2023 third-party SR 26-2 (LLM not model-risk, but third-party)`; `UK Finance 2025` `private cloud closed LLM + 30d zero retention + operator QA sample`; `FinRAG-12B ACL 2026` `143M tokens, 7.1pp resolution, 3-5x faster 20-50x cheaper, calibrated refusal 12%`.

## Decision Drivers

* **PII residency**: `NIP`/`NIK`/`account` never leaves VPC in plaintext.
* **Accuracy**: RAG grounding `citation` + `refusal 12%` > `GPT-4.1 over-refusal 20.2%` (FinRAG-12B) for KYC/compliance where hallucination = regulatory failure (`ABS`).
* **Cost**: `vLLM` self-host 20-50x cheaper than `GPT-4.1` at `40+ FIs` scale — matters even at lab volume.

## Considered Options

### Option A — External API only (OpenAI/Claude) via 3scale proxy

* **Pros**: no GPU, fastest.
* **Cons**: `BPPD 14.4% hiding rate` baseline leaks PII → `UU PDP` breach; `GLBA` third-party risk without deployment controls.

### Option B — Fully on-prem `OpenShift AI + vLLM` closed LLM + RAG (chosen, hybrid)

* **Pros**: `UCL BPPD 87.3% hiding` `99.9% safety` on single GPU, `FF3-1 FPE` preserves format for downstream, `ABS` RAG + human-in-loop, `UK Finance` private cloud 30d zero retention satisfied, upgrade external model without touching privacy layer.
* **Cons**: needs `1× RTX 4090` or `ROCm` node + `pgvector`.

### Option C — Fine-tune 12B per domain (FinRAG-12B recipe)

* **Pros**: best grounding.
* **Cons**: 143M token pipeline + GPU training — overkill for lab; defer to `MLOps` `payu-mlops` namespace when `analytics` scale warrants.

## Decision

**Option B — Hybrid private: DetectorLLM + FPE proxy → RAG vLLM, 3scale governed.**

1. **Architecture** (`BPPD Separation-of-Concerns`): `App (support/compliance) → BPPD Proxy (DetectorLLM Llama-3.2-3B LoRA 0.6% params, 8-bit, 1× GPU) → FPE (FF3-1) → vLLM (Mistral 7B Instruct / Llama-3-8B via OpenShift AI Model Serving, temperature 0, meta-prompt cite+sources) → pgvector RAG (TimescaleDB `pgvector` cosine `0.75` threshold, `payu.docs.*` + `ledger summaries` sanitized) → output scrubber (regex `IBAN/account/phone` + DSE PII filter) → human confirmation gate for **irreversible** `submit dispute/update contact/initiate payment` (`excessive agency`).
2. **PayU surfaces (allowlist)**:
   * `support-service`: agent-assist `summarize transcript + suggest reply` + closed `I don't know 12%` refusal (FinRAG `22% unanswerable training`).
   * `compliance-service`: `AuditReport` narrative generation `temperature 0` + `relevancy KPI semantic similarity >0.85`.
   * `statement-service`/`promotion-service`: spend/promo personalization **after** `DataAccessAudit` `purpose` check.
   * `kyc-service`: doc extraction **not** LLM auto-approve — LLM drafts → operator `sample QA` (ABS human-in-loop) before `KYC verified`.
3. **Gateway**: `3scale` (ADR-0064) `leaky_bucket 10/min per user {{jwt.claim.sub}}` on `POST /api/v1/support/assist` + `IP check`, `ActiveDocs` OAS 3.0 `x-data-threescale-name=user_keys` try-it.
4. **Governance** (`NIST AI RMF` + `GLBA`): `GOVERN` policy `allowlist actions`, `MAP` inventory `sensitivity=TIER1 PII`, `MEASURE` red-team `LLM01/02/06` payloads + cross-session leakage test, `MANAGE` `Wazuh` immutable `traceId→prompt→FPE→response→human decision` 1y hot/7y cold (PCI Req10). `June 2023 third-party` lifecycle for `vLLM` image (Quay signed `cosign` per ADR-0045).
5. **Data**: `vectorize` via `security-starter` `EncryptedStringConverter` already masked; embeddings store only `hash(account)` + `tenant_id` RLS (ADR-0061 `FORCE RLS`); cross-border `data residency` per `docs/architecture/ARCHITECTURE.md:16` — inference stays in `payu` VPC, no `region=eu` transfer unless `DPDI` contract.

## Rationale

* `UCL BPPD 87.3%` hiding + `99.9%` safety on **commodity single GPU** beats naive external 14.4% — fits lab budget.
* `ABS` + `FinRAG-12B` show `RAG + calibrated refusal` solves `hallucination/overconfidence` for banking KYC/compliance where accuracy critical.
* `DSE July 2026` proves `prompt injection` via `dispute text` is real banking vector — `sanitize bracket` + `retrieval authorization` at RAG layer (not app layer) is the fix.

## Consequences

**Positive**: 3-5x faster, 20-50x cheaper than external, `UU PDP` residency intact, reusable proxy for any future `GPT-4` upgrade.
**Negative**: `~300ms` FPE overhead per prompt, 1 GPU `payu-mlops` namespace quota needed, model drift review 12-24m (HKMA) required.

## Implementation Notes

* Deploy `infrastructure/platform/mlops/llm/values.yaml`: `OpenShift AI` `ServingRuntime vLLM` `Mistral-7B-Instruct-v0.3` `Toleration gpu`, `PG 16 pgvector` extension on `CNPG payu-analytics`, `Service payu-llm-proxy:8080` (`DetectorLLM` sidecar).
* `backend/support-service/src/main/java` add `LlmAssistPort` → `outbox` `payu.support.assist-requested.v1` → `python-llm-proxy` (FastAPI) via `gRPC` (ADR-0037) or `REST` with `DPoP` (ADR-0062).
* Contract test `Pact` `POST /assist` `promptId→responseId`, red-team `scripts/llm-redteam.sh` `LLM01` payloads.
* Metric `llm.request.hidden_entities`, `llm.refusal.rate`, `llm.retrieval.auth.denied` to `Grafana` (ADR-0034).

---
*Created via AI Engineer — refs UCL BPPD 2026-04, ABS 2026-03, DSE 2026-07, UK Finance 2025, FinRAG-12B ACL 2026 + CodeGraph `support-service`, `compliance-service`, `kyc-service`*
