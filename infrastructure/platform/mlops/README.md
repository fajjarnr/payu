# MLOps Platform — PayU

## B4.6 Decision 2026-08-24: ADR-0067 LLM Integration → DEFERRED (NO-GO)

**Decision: DEFERRED — ponytail YAGNI.**

Spec at `docs/adr/0067-llm-integration-for-payu-services-standard.md` (Proposed) is architecturally sound (Option B hybrid BPPD DetectorLLM Llama-3.2-3B LoRA 99.9% + FF3-1 FPE + vLLM Mistral-7B temp 0 + pgvector RAG cosine 0.75 + 3scale leaky_bucket 10/min + NIST AI RMF + Wazuh 1y/7y). Cost/benefit at 1.18.10 fails:

| Factor | Cost | Benefit | Verdict |
|---|---|---|---|
| Infra | 1× GPU node (RTX 4090/ROCm) + OpenShift AI ServingRuntime + pgvector on CNPG payu-analytics + `payu-mlops` namespace quota + 300ms FPE overhead + drift review 12-24m | Lab has 0 GPU quota, `payu-dev 23 svcs ExceededNodeResources` already, 0 artifacts in repo (`values.yaml`, `LlmAssistPort`, `python-llm-proxy`, `llm-redteam.sh` all absent 2026-08-24 audit) | Cost immediate, benefit speculative |
| Demand | 5k tickets/mo manual triage, rule/heuristic today, no SLA breach tracked; `promotion` copy + `compliance` audit + `statement` summary + `kyc` draft all function without LLM | No validated KPI that LLM beats rules (no A/B, no refusal/accuracy baseline) | YAGNI |
| Risk | External LLM leaks 14.4% PII (BPPD baseline) but hybrid 87.3% hiding fixes it — requires the GPU proxy to get the fix | Existing `EncryptedStringConverter` + blind index + `tenant_id` RLS already satisfies UU PDP residency without LLM | Risk already mitigated cheaper |
| Alternatives | vLLM self-host 20-50x cheaper than GPT-4.1 at 40 FIs scale — not at lab volume; FinRAG-12B 143M token fine-tune deferred correctly | - | Defer to when scale warrants |

**Go criteria (re-evaluate quarterly):**
1. `payu-mlops` namespace + GPU node quota provisioned (ROCm or RTX 4090 toleration) + ResourceQuota relief (ExceededNodeResources cleared).
2. Validated demand: support ticket volume or compliance audit time shows rule/heuristic SLA miss (e.g., triage >4h or hallucination-free narrative required by auditor).
3. pgvector extension approved on CNPG `payu_analytics` (see `infrastructure/platform/data/pgvector/` deferred).

Until then: ADR-0067 stays `Proposed` → `Deferred` in `docs/adr/README.md`, `TODOS LLM-HARDEN-001` marked deferred, no code/manifests created (per assignment "No code unless go").

**If GO later, minimal manifests (per Implementation Notes):**
- `infrastructure/platform/mlops/llm/values.yaml` — ServingRuntime vLLM Mistral-7B-Instruct-v0.3 toleration gpu, Service payu-llm-proxy:8080 DetectorLLM sidecar
- `backend/support-service/... LlmAssistPort → outbox payu.support.assist-requested.v1` + `python-llm-proxy` FastAPI gRPC/REST DPoP
- `scripts/llm-redteam.sh` LLM01 payloads + Pact contract `POST /assist`
- Metrics `llm.request.hidden_entities`, `llm.refusal.rate` → Grafana (ADR-0034)

---
*Ponytail: deletion over addition — defer GPU/operator until demand proven; upgrade path documented above.*
