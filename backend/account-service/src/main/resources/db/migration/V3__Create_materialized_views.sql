-- Materialized Views for Analytics Dashboards
-- These views are optimized for reporting queries and should be refreshed periodically

-- 1. Account Statistics Materialized View
-- Provides aggregated account statistics for dashboard
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_account_statistics AS
SELECT
    date_trunc('day', created_at) as date,
    COUNT(*) as total_accounts,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_accounts,
    COUNT(*) FILTER (WHERE status = 'PENDING') as pending_accounts,
    COUNT(*) FILTER (WHERE status = 'SUSPENDED') as suspended_accounts,
    COUNT(*) FILTER (WHERE status = 'CLOSED') as closed_accounts,
    COUNT(*) FILTER (WHERE kyc_status = 'APPROVED') as kyc_approved,
    COUNT(*) FILTER (WHERE kyc_status = 'PENDING') as kyc_pending,
    COUNT(*) FILTER (WHERE kyc_status = 'REJECTED') as kyc_rejected,
    AVG(EXTRACT(EPOCH FROM (COALESCE(kyc_completed_at, created_at) - created_at))/60) as avg_kyc_completion_minutes
FROM accounts
GROUP BY date_trunc('day', created_at);

-- Create index for efficient querying
CREATE INDEX IF NOT EXISTS idx_mv_account_stats_date ON mv_account_statistics(date);

-- 2. Account Balance Summary Materialized View
-- Provides daily balance summaries for analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_account_balance_summary AS
SELECT
    account_id,
    date_trunc('day', updated_at) as date,
    SUM(amount) as total_balance,
    COUNT(*) as transaction_count,
    AVG(amount) as avg_balance,
    MIN(amount) as min_balance,
    MAX(amount) as max_balance
FROM (
    SELECT
        a.id as account_id,
        b.amount,
        b.updated_at
    FROM accounts a
    JOIN balances b ON a.id = b.account_id
    WHERE b.updated_at >= CURRENT_DATE - INTERVAL '30 days'
) daily_balances
GROUP BY account_id, date_trunc('day', updated_at);

CREATE INDEX IF NOT EXISTS idx_mv_balance_summary_account_date ON mv_account_balance_summary(account_id, date);

-- 3. KYC Processing Time Materialized View
-- Tracks KYC processing performance metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_kyc_processing_metrics AS
SELECT
    date_trunc('day', created_at) as date,
    kyc_status,
    COUNT(*) as total_requests,
    AVG(EXTRACT(EPOCH FROM (COALESCE(kyc_completed_at, CURRENT_TIMESTAMP) - created_at))/3600) as avg_processing_hours,
    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (COALESCE(kyc_completed_at, CURRENT_TIMESTAMP) - created_at))/3600) as median_processing_hours,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (COALESCE(kyc_completed_at, CURRENT_TIMESTAMP) - created_at))/3600) as p95_processing_hours,
    COUNT(*) FILTER (WHERE auto_approved = true) as auto_approved_count
FROM accounts
WHERE kyc_status IN ('APPROVED', 'REJECTED')
    AND created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), kyc_status;

CREATE INDEX IF NOT EXISTS idx_mv_kyc_metrics_date_status ON mv_kyc_processing_metrics(date, kyc_status);

-- 4. Account Creation Trends Materialized View
-- Monthly account creation trends with segmentation
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_account_creation_trends AS
SELECT
    date_trunc('month', created_at) as month,
    COUNT(*) as total_accounts,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_accounts,
    COUNT(*) FILTER (WHERE account_type = 'SAVINGS') as savings_accounts,
    COUNT(*) FILTER (WHERE account_type = 'CHECKING') as checking_accounts,
    COUNT(*) FILTER (WHERE account_type = 'POCKET') as pocket_accounts,
    COUNT(DISTINCT customer_id) as unique_customers
FROM accounts
GROUP BY date_trunc('month', created_at);

CREATE INDEX IF NOT EXISTS idx_mv_creation_trends_month ON mv_account_creation_trends(month);

-- Refresh function for all materialized views
CREATE OR REPLACE FUNCTION refresh_analytics_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_statistics;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_balance_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_kyc_processing_metrics;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_account_creation_trends;
END;
$$ LANGUAGE plpgsql;

-- Comment for documentation
COMMENT ON MATERIALIZED VIEW mv_account_statistics IS 'Daily aggregated account statistics for dashboard analytics';
COMMENT ON MATERIALIZED VIEW mv_account_balance_summary IS 'Daily balance summaries per account';
COMMENT ON MATERIALIZED VIEW mv_kyc_processing_metrics IS 'KYC processing performance metrics';
COMMENT ON MATERIALIZED VIEW mv_account_creation_trends IS 'Monthly account creation trends';
COMMENT ON FUNCTION refresh_analytics_views() IS 'Refresh all analytics materialized views concurrently';
