-- Materialized Views for Transaction Analytics
-- Optimized for reporting and dashboard queries

-- 1. Transaction Volume and Value Metrics
-- Daily transaction metrics for analytics dashboard
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
    COUNT(DISTINCT receiver_account_id) as unique_receivers
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), type, status;

CREATE INDEX IF NOT EXISTS idx_mv_tx_metrics_date_type_status ON mv_transaction_daily_metrics(date, type, status);

-- 2. Transaction Success Rate Metrics
-- Success rates by transaction type and channel
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_success_rates AS
SELECT
    date_trunc('day', created_at) as date,
    type,
    channel,
    COUNT(*) as total_transactions,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_transactions,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_transactions,
    COUNT(*) FILTER (WHERE status = 'PENDING') as pending_transactions,
    ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'COMPLETED') / NULLIF(COUNT(*), 0), 2) as success_rate,
    ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'FAILED') / NULLIF(COUNT(*), 0), 2) as failure_rate
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), type, channel;

CREATE INDEX IF NOT EXISTS idx_mv_tx_success_date ON mv_transaction_success_rates(date);

-- 3. High-Value Transactions Materialized View
-- Tracks high-value transactions for compliance and analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_high_value_transactions AS
SELECT
    date_trunc('day', created_at) as date,
    type,
    status,
    COUNT(*) as high_value_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM transactions
WHERE amount >= 10000000  -- 10 million IDR threshold
    AND created_at >= CURRENT_DATE - INTERVAL '180 days'
GROUP BY date_trunc('day', created_at), type, status;

CREATE INDEX IF NOT EXISTS idx_mv_hvt_date ON mv_high_value_transactions(date);

-- 4. Transaction Hourly Patterns Materialized View
-- Hourly transaction patterns for capacity planning
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_hourly_patterns AS
SELECT
    EXTRACT(DOW FROM created_at) as day_of_week,
    EXTRACT(HOUR FROM created_at) as hour,
    type,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY EXTRACT(DOW FROM created_at), EXTRACT(HOUR FROM created_at), type;

CREATE INDEX IF NOT EXISTS idx_mv_tx_patterns_day_hour ON mv_transaction_hourly_patterns(day_of_week, hour);

-- 5. Transaction Fee Summary Materialized View
-- Daily fee summaries for revenue analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_fee_summary AS
SELECT
    date_trunc('day', created_at) as date,
    type,
    COUNT(*) as transaction_count,
    SUM(fee_amount) as total_fees,
    AVG(fee_amount) as avg_fee,
    SUM(CASE WHEN fee_amount = 0 THEN 1 ELSE 0 END) as zero_fee_count
FROM transactions
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
    AND status = 'COMPLETED'
GROUP BY date_trunc('day', created_at), type;

CREATE INDEX IF NOT EXISTS idx_mv_fee_summary_date ON mv_transaction_fee_summary(date, type);

-- Refresh function for all transaction materialized views
CREATE OR REPLACE FUNCTION refresh_transaction_analytics_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_daily_metrics;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_success_rates;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_high_value_transactions;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_hourly_patterns;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_fee_summary;
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON MATERIALIZED VIEW mv_transaction_daily_metrics IS 'Daily transaction volume and value metrics';
COMMENT ON MATERIALIZED VIEW mv_transaction_success_rates IS 'Transaction success rates by type and channel';
COMMENT ON MATERIALIZED VIEW mv_high_value_transactions IS 'High-value transactions (>= 10M IDR) tracking';
COMMENT ON MATERIALIZED VIEW mv_transaction_hourly_patterns IS 'Hourly transaction patterns for capacity planning';
COMMENT ON MATERIALIZED VIEW mv_transaction_fee_summary IS 'Daily fee summaries for revenue analytics';
COMMENT ON FUNCTION refresh_transaction_analytics_views() IS 'Refresh all transaction analytics materialized views';
