-- Materialized Views for Analytics Dashboards
-- These views are optimized for reporting queries and should be refreshed periodically

-- 1. Account Statistics Materialized View
-- Provides aggregated account statistics for dashboard
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_account_statistics AS
SELECT
    date_trunc('day', a.created_at) as date,
    COUNT(*) as total_accounts,
    COUNT(*) FILTER (WHERE a.status = 'ACTIVE') as active_accounts,
    COUNT(*) FILTER (WHERE a.status = 'PENDING') as pending_accounts,
    COUNT(*) FILTER (WHERE a.status = 'SUSPENDED') as suspended_accounts,
    COUNT(*) FILTER (WHERE a.status = 'CLOSED') as closed_accounts
FROM accounts a
GROUP BY date_trunc('day', a.created_at);

-- Create index for efficient querying
CREATE INDEX IF NOT EXISTS idx_mv_account_stats_date ON mv_account_statistics(date);

-- 2. Account Balance Summary Materialized View
-- Provides daily balance summaries for analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_account_balance_summary AS
SELECT
    a.id as account_id,
    date_trunc('day', a.updated_at) as date,
    COALESCE(a.balance, 0) as total_balance,
    0 as transaction_count,
    COALESCE(a.balance, 0) as avg_balance,
    COALESCE(a.balance, 0) as min_balance,
    COALESCE(a.balance, 0) as max_balance
FROM accounts a
WHERE a.updated_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY a.id, date_trunc('day', a.updated_at);

CREATE INDEX IF NOT EXISTS idx_mv_balance_summary_account_date ON mv_account_balance_summary(account_id, date);

-- 3. KYC Processing Time Materialized View
-- Tracks KYC processing performance metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_kyc_processing_metrics AS
SELECT
    date_trunc('day', u.created_at) as date,
    u.kyc_status,
    COUNT(*) as total_requests,
    AVG(EXTRACT(EPOCH FROM (COALESCE(u.created_at, CURRENT_TIMESTAMP) - u.created_at))/3600) as avg_processing_hours
FROM users u
WHERE u.kyc_status IN ('APPROVED', 'REJECTED', 'PENDING')
    AND u.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', u.created_at), u.kyc_status;

CREATE INDEX IF NOT EXISTS idx_mv_kyc_metrics_date_status ON mv_kyc_processing_metrics(date, kyc_status);

-- 4. Account Creation Trends Materialized View
-- Monthly account creation trends with segmentation
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_account_creation_trends AS
SELECT
    date_trunc('month', a.created_at) as month,
    COUNT(*) as total_accounts,
    COUNT(*) FILTER (WHERE a.status = 'ACTIVE') as active_accounts,
    COUNT(DISTINCT a.user_id) as unique_customers
FROM accounts a
GROUP BY date_trunc('month', a.created_at);

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
