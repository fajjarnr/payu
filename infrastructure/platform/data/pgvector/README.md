# pgvector — PayU Analytics (Deferred with ADR-0067)

## Status: DEFERRED (tied to LLM-HARDEN-001)

`pgvector` extension on `CNPG payu_analytics` (`payu_analytics` DB in `cnpg-databases.yaml`, TimescaleDB hypertables ADR-0046) is required only for ADR-0067 RAG (`payu.docs.*` cosine 0.75).

**Why deferred:** ADR-0067 is NO-GO (see `infrastructure/platform/mlops/README.md` B4.6 2026-08-24). Extension adds `shared_preload_libraries`, index build, and backup size for zero current consumers. No LLM pipeline writes embeddings today (`analytics-service` torch fraud/score is non-vector).

**When to enable (KEDA-style minimal):**
1. Enable extension via CNPG `postInitSQL` or `Database` + `Job`:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   CREATE TABLE payu_docs_embeddings (id uuid PK, tenant_id uuid, doc_id text, embedding vector(1536), tenant RLS);
   CREATE INDEX ON payu_docs_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
   ALTER TABLE payu_docs_embeddings ENABLE ROW LEVEL SECURITY; -- ADR-0061 FORCE RLS
   ```
   Masked via `security-starter EncryptedStringConverter`, store `hash(account)` + `tenant_id` only.
2. Gate behind `analytics-service` feature flag; measure recall@k before prod.

*Ponytail: 1 Job + extension when LLM GO; no schema churn now.*
