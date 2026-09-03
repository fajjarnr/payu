-- RELAY-007: TransactionStatus gained PENDING_COMPLIANCE_REVIEW (25 chars, ADR-0030)
-- and PENDING_STEP_UP (15 chars, ADR-0028) but status columns are VARCHAR(20),
-- so persisting a held transfer aborts with SQLState 22001 and the API 500s.
-- Widen lifecycle status columns to VARCHAR(40). Currency/money columns untouched.
--
-- Three materialized views reference transactions.status, blocking ALTER TYPE
-- (rule _RETURN dependency): drop and recreate them around the alters.
-- Definitions mirror V6__Create_materialized_views.sql.
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_daily_metrics;
DROP MATERIALIZED VIEW IF EXISTS mv_transaction_success_rates;
DROP MATERIALIZED VIEW IF EXISTS mv_high_value_transactions;

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

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_success_rates AS
SELECT
    date_trunc('day', created_at) as date,
    type,
    COUNT(*) as total_transactions,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_transactions,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_transactions,
    COUNT(*) FILTER (WHERE status = 'PENDING') as pending_transactions,
    ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'COMPLETED') / NULLIF(COUNT(*), 0), 2) as success_rate,
    ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'FAILED') / NULLIF(COUNT(*), 0), 2) as failure_rate
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), type;

CREATE INDEX IF NOT EXISTS idx_mv_tx_success_date ON mv_transaction_success_rates(date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_high_value_transactions AS
SELECT
    date_trunc('day', created_at) as date,
    type,
    status,
    COUNT(*) as high_value_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM transactions
WHERE amount >= 10000000
    AND created_at >= CURRENT_DATE - INTERVAL '180 days'
GROUP BY date_trunc('day', created_at), type, status;

CREATE INDEX IF NOT EXISTS idx_mv_hvt_date ON mv_high_value_transactions(date);

COMMENT ON MATERIALIZED VIEW mv_transaction_daily_metrics IS 'Daily transaction volume and value metrics';
COMMENT ON MATERIALIZED VIEW mv_transaction_success_rates IS 'Transaction success rates by type';
COMMENT ON MATERIALIZED VIEW mv_high_value_transactions IS 'High-value transactions (>= 10M IDR) tracking';
