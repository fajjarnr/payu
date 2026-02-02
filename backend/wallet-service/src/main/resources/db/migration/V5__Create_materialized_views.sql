-- Materialized Views for Wallet Analytics
-- Optimized for reporting and dashboard queries
-- Note: Simplified to match actual V1 schema (wallets and wallet_transactions only)

-- 1. Wallet Balance Summary Materialized View
-- Current wallet balances
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_balance_summary AS
SELECT
    w.id as wallet_id,
    w.account_id,
    w.currency,
    w.balance as total_balance,
    w.reserved_balance,
    w.status,
    w.updated_at
FROM wallets w
WHERE w.status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_wallet_balance_wallet_id ON mv_wallet_balance_summary(wallet_id);
CREATE INDEX IF NOT EXISTS idx_mv_wallet_balance_account_id ON mv_wallet_balance_summary(account_id);

-- 2. Wallet Transaction Summary Materialized View
-- Daily transaction metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_transaction_daily_summary AS
SELECT
    date_trunc('day', wt.created_at) as date,
    wt.type,
    COUNT(wt.id) as transaction_count,
    SUM(wt.amount) as total_amount,
    AVG(wt.amount) as avg_amount,
    COUNT(DISTINCT wt.wallet_id) as unique_wallets
FROM wallet_transactions wt
WHERE wt.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', wt.created_at), wt.type;

CREATE INDEX IF NOT EXISTS idx_mv_tx_summary_date_type ON mv_transaction_daily_summary(date, type);

-- 3. Wallet Active Users Materialized View
-- Tracks daily active wallet users
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_active_users AS
SELECT
    date_trunc('day', wt.created_at) as date,
    COUNT(DISTINCT wt.wallet_id) as active_wallets,
    SUM(wt.amount) as total_volume,
    COUNT(wt.id) as total_transactions
FROM wallet_transactions wt
WHERE wt.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', wt.created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_wallet_active_date ON mv_wallet_active_users(date);

-- Refresh function for all wallet materialized views
CREATE OR REPLACE FUNCTION refresh_wallet_analytics_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_wallet_balance_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_transaction_daily_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_wallet_active_users;
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON MATERIALIZED VIEW mv_wallet_balance_summary IS 'Current wallet balances for active wallets';
COMMENT ON MATERIALIZED VIEW mv_transaction_daily_summary IS 'Daily transaction metrics (90 days)';
COMMENT ON MATERIALIZED VIEW mv_wallet_active_users IS 'Daily active wallet users and volume (90 days)';
COMMENT ON FUNCTION refresh_wallet_analytics_views() IS 'Refresh all wallet analytics materialized views';
