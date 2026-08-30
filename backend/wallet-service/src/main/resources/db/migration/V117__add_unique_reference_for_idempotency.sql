-- QE-LEDGER-002 / QE-TEST-001: idempotency race without unique constraint
-- Concurrent findByTransactionId/referenceId before SELECT FOR UPDATE → duplicate double-execute.
-- Add partial unique indexes to make second insert fail fast (duplicate key) instead of relying on app-level check.
-- ponytail: global unique on (reference_type, reference_id, entry_type) where not null; allows double-entry DEBIT+CREDIT per reference, prevents duplicate same-type replay

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_transactions_reference_id
    ON wallet_transactions(reference_id)
    WHERE reference_id IS NOT NULL AND reference_id <> '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_entries_reference_id
    ON ledger_entries(reference_type, reference_id, entry_type)
    WHERE reference_id IS NOT NULL AND reference_id <> '' AND reference_id <> 'INTERNAL';

-- idempotency_keys PG table for outbox/idempotency fallback (migrator proposed V120)
-- Already handled via HotRod; keep PG table as durable fallback if Redis unavailable
CREATE TABLE IF NOT EXISTS idempotency_keys (
    idempotency_key VARCHAR(64) PRIMARY KEY,
    fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_body TEXT,
    http_status INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_keys(expires_at);
