-- Add idempotency_key to transactions table for duplicate protection
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_transactions_idempotency ON transactions(idempotency_key);
COMMENT ON COLUMN transactions.idempotency_key IS 'Unique key for duplicate protection';
