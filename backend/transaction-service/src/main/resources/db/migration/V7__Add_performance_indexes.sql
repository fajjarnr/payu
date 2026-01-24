-- Performance Optimization Indexes for Transaction Service
-- These indexes optimize common query patterns based on transaction access patterns

-- 1. Primary transaction lookup indexes
-- Optimizes queries by sender_account_id with most recent first
CREATE INDEX IF NOT EXISTS idx_transactions_sender_created
ON transactions(sender_account_id, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '180 days';

-- Optimizes queries by receiver_account_id
CREATE INDEX IF NOT EXISTS idx_transactions_receiver_created
ON transactions(receiver_account_id, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '180 days';

-- Optimizes account-to-account transaction lookups
CREATE INDEX IF NOT EXISTS idx_transactions_sender_receiver
ON transactions(sender_account_id, receiver_account_id, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- 2. Status and type filtering indexes
-- Optimizes pending/failed transaction queries
CREATE INDEX IF NOT EXISTS idx_transactions_status_created
ON transactions(status, created_at DESC)
WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');

-- Optimizes transaction type queries
CREATE INDEX IF NOT EXISTS idx_transactions_type_created
ON transactions(type, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- Combined type and status for reporting
CREATE INDEX IF NOT EXISTS idx_transactions_type_status_created
ON transactions(type, status, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- 3. Channel-based indexes
-- Optimizes queries by channel (BI-FAST, QRIS, etc.)
CREATE INDEX IF NOT EXISTS idx_transactions_channel_created
ON transactions(channel, created_at DESC)
WHERE channel IS NOT NULL AND created_at >= CURRENT_DATE - INTERVAL '90 days';

-- Channel + status for monitoring
CREATE INDEX IF NOT EXISTS idx_transactions_channel_status
ON transactions(channel, status, created_at DESC)
WHERE channel IS NOT NULL AND status IN ('PENDING', 'FAILED');

-- 4. Amount-based indexes for analytics
-- High-value transactions (for compliance)
CREATE INDEX IF NOT EXISTS idx_transactions_amount_desc
ON transactions(amount DESC, created_at DESC)
WHERE amount >= 10000000 AND created_at >= CURRENT_DATE - INTERVAL '180 days';

-- Amount range queries
CREATE INDEX IF NOT EXISTS idx_transactions_amount_range
ON transactions(amount, created_at DESC)
WHERE amount BETWEEN 100000 AND 100000000;

-- 5. Reference ID lookup (for idempotency and status checks)
CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_reference_unique
ON transactions(reference_id)
WHERE reference_id IS NOT NULL;

-- 6. Date range indexes for reporting
-- Daily transaction counts
CREATE INDEX IF NOT EXISTS idx_transactions_date_type_status
ON transactions(date_trunc('day', created_at), type, status)
WHERE created_at >= CURRENT_DATE - INTERVAL '365 days';

-- Weekly aggregates
CREATE INDEX IF NOT EXISTS idx_transactions_week_type
ON transactions(date_trunc('week', created_at), type)
WHERE created_at >= CURRENT_DATE - INTERVAL '365 days';

-- 7. Partial indexes for common query patterns
-- Recent transactions only (reduces index size significantly)
CREATE INDEX IF NOT EXISTS idx_transactions_recent
ON transactions(created_at DESC, status)
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days';

-- Failed transactions for retry processing
CREATE INDEX IF NOT EXISTS idx_transactions_failed_retry
ON transactions(created_at, retry_count)
WHERE status = 'FAILED' AND retry_count < 3;

-- 8. Composite covering indexes for hot queries
-- Transaction detail queries
CREATE INDEX IF NOT EXISTS idx_transactions_detail_covering
ON transactions(sender_account_id, created_at DESC)
INCLUDE (receiver_account_id, amount, type, status, reference_id)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- Account statement queries
CREATE INDEX IF NOT EXISTS idx_transactions_statement_covering
ON transactions(sender_account_id, created_at DESC)
INCLUDE (amount, type, status, fee_amount, channel)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- 9. BRIN indexes for time-series data (very efficient for large tables)
-- Transaction creation time series
CREATE INDEX IF NOT EXISTS idx_transactions_created_brin
ON transactions USING BRIN (created_at);

-- Transaction updated time series
CREATE INDEX IF NOT EXISTS idx_transactions_updated_brin
ON transactions USING BRIN (updated_at);

-- 10. Scheduled transactions indexes
-- Pending scheduled transactions
CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_pending
ON scheduled_transfers(scheduled_for, status)
WHERE status = 'PENDING' AND scheduled_for >= CURRENT_TIMESTAMP;

-- Recurring transaction lookups
CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_recurring
ON scheduled_transfers(sender_account_id, frequency, status)
WHERE is_recurring = true;

-- 11. Archive table indexes
-- Archived transaction lookups
CREATE INDEX IF NOT EXISTS idx_transaction_archives_created
ON transaction_archives(created_at DESC);

-- Archive by sender
CREATE INDEX IF NOT EXISTS idx_transaction_archives_sender
ON transaction_archives(sender_account_id, created_at DESC);

-- Index maintenance and statistics
ANALYZE transactions, scheduled_transfers, transaction_archives;

-- Documentation comments
COMMENT ON INDEX idx_transactions_sender_created IS 'Optimizes sender transaction history (recent 180 days)';
COMMENT ON INDEX idx_transactions_receiver_created IS 'Optimizes receiver transaction history (recent 180 days)';
COMMENT ON INDEX idx_transactions_status_created IS 'Pending/failed transaction queries';
COMMENT ON INDEX idx_transactions_amount_desc IS 'High-value transactions for compliance';
COMMENT ON INDEX idx_transactions_recent IS 'Recent transactions (30 days) for dashboard';
COMMENT ON INDEX idx_transactions_created_brin IS 'BRIN index for time-series analysis';
COMMENT ON INDEX idx_scheduled_transfers_pending IS 'Pending scheduled transfers for processing';
