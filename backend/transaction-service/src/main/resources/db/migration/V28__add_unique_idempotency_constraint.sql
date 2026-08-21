-- TXN-HARDEN-001: Idempotency DB hardening Q3
-- Replace non-unique index with partial unique constraint scoped to tenant
-- Evidence: V14 only INDEX, race in InitiateTransferCommandHandler:76
-- ponytail: partial unique prevents duplicate mutation under concurrent X-Idempotency-Key; upgrade to idempotency_keys table if cross-table dedup needed
DROP INDEX IF EXISTS idx_transactions_idempotency;
CREATE UNIQUE INDEX IF NOT EXISTS ux_transactions_tenant_idempotency
  ON transactions(tenant_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
COMMENT ON INDEX ux_transactions_tenant_idempotency IS 'TXN-HARDEN-001: tenant-scoped idempotency dedup';
