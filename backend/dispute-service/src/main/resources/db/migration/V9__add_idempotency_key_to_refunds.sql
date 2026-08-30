-- PER-SVC-003: Refund idempotency no column → add idempotency_key for replay dedup
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_refunds_idempotency_key ON refunds(idempotency_key) WHERE idempotency_key IS NOT NULL;
