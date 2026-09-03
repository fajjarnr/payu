-- RELAY-007: TransactionStatus gained PENDING_COMPLIANCE_REVIEW (25 chars, ADR-0030)
-- and PENDING_STEP_UP (15 chars, ADR-0028) but status columns are VARCHAR(20),
-- so persisting a held transfer aborts with SQLState 22001 and the API 500s.
-- Widen lifecycle status columns to VARCHAR(40). Currency/money columns untouched.
--
-- mv_transaction_daily_metrics selects transactions.status, so ALTER TYPE is
-- blocked (rule _RETURN dependency): drop and recreate it around the alters.
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_daily_metrics;

ALTER TABLE transactions ALTER COLUMN status TYPE VARCHAR(40);
ALTER TABLE scheduled_transfers ALTER COLUMN status TYPE VARCHAR(40);
ALTER TABLE transaction_archives ALTER COLUMN status TYPE VARCHAR(40);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_daily_metrics AS
SELECT
    date_trunc('day', created_at) as date,
    type,
    status,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount,
    MIN(amount) as min_amount,
    MAX(amount) as max_amount,
    COUNT(DISTINCT sender_account_id) as unique_senders,
    COUNT(DISTINCT recipient_account_id) as unique_receivers
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), type, status;

CREATE INDEX IF NOT EXISTS idx_mv_tx_metrics_date_type_status ON mv_transaction_daily_metrics(date, type, status);
COMMENT ON MATERIALIZED VIEW mv_transaction_daily_metrics IS 'Daily transaction volume and value metrics';
