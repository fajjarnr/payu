-- Performance Optimization Indexes for Transaction Service
-- These indexes optimize common query patterns based on transaction access patterns

-- 1. Primary transaction lookup indexes
-- Optimizes queries by sender_account_id with most recent first
CREATE INDEX IF NOT EXISTS idx_transactions_sender_created
ON transactions(sender_account_id, created_at DESC);

-- Optimizes queries by recipient_account_id
CREATE INDEX IF NOT EXISTS idx_transactions_recipient_created
ON transactions(recipient_account_id, created_at DESC);

-- Optimizes account-to-account transaction lookups
CREATE INDEX IF NOT EXISTS idx_transactions_sender_recipient
ON transactions(sender_account_id, recipient_account_id, created_at DESC);

-- 2. Status and type filtering indexes
-- Optimizes pending/failed transaction queries
CREATE INDEX IF NOT EXISTS idx_transactions_status_created
ON transactions(status, created_at DESC)
WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');

-- Optimizes transaction type queries
CREATE INDEX IF NOT EXISTS idx_transactions_type_created
ON transactions(type, created_at DESC);

-- Combined type and status for reporting
CREATE INDEX IF NOT EXISTS idx_transactions_type_status_created
ON transactions(type, status, created_at DESC);

-- 3. Amount-based indexes for analytics
-- High-value transactions (for compliance)
CREATE INDEX IF NOT EXISTS idx_transactions_amount_desc
ON transactions(amount DESC, created_at DESC);

-- 4. Reference Number lookup (for idempotency and status checks)
-- Note: reference_number is already unique from V5, but this ensures index existence
CREATE INDEX IF NOT EXISTS idx_transactions_reference_lookup
ON transactions(reference_number);

-- 5. Composite covering indexes for hot queries
-- Transaction detail queries (covering common columns to avoid heap lookup)
CREATE INDEX IF NOT EXISTS idx_transactions_detail_covering
ON transactions(sender_account_id, created_at DESC)
INCLUDE (recipient_account_id, amount, type, status, reference_number);

-- 6. BRIN indexes for time-series data (very efficient for large tables ordered by time)
-- Transaction creation time series
CREATE INDEX IF NOT EXISTS idx_transactions_created_brin
ON transactions USING BRIN (created_at);

-- Transaction updated time series
CREATE INDEX IF NOT EXISTS idx_transactions_updated_brin
ON transactions USING BRIN (updated_at);

-- 7. Archive table indexes
-- Archived transaction lookups
CREATE INDEX IF NOT EXISTS idx_transaction_archives_created
ON transaction_archives(created_at DESC);

-- Archive by sender
CREATE INDEX IF NOT EXISTS idx_transaction_archives_sender
ON transaction_archives(sender_account_id, created_at DESC);

-- Index maintenance and statistics
-- Note: ANALYZE might take time on large tables, consider running it separately in production
-- ANALYZE transactions;

-- Documentation comments
COMMENT ON INDEX idx_transactions_sender_created IS 'Optimizes sender transaction history';
COMMENT ON INDEX idx_transactions_recipient_created IS 'Optimizes recipient transaction history';
COMMENT ON INDEX idx_transactions_status_created IS 'Pending/failed transaction queries';
COMMENT ON INDEX idx_transactions_amount_desc IS 'High-value transactions for compliance';
COMMENT ON INDEX idx_transactions_created_brin IS 'BRIN index for time-series analysis';
