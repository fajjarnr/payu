-- Materialized Views for Wallet Analytics
-- Optimized for reporting and dashboard queries

-- 1. Wallet Balance Summary Materialized View
-- Daily wallet balance summaries
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_balance_summary AS
SELECT
    w.id as wallet_id,
    w.account_id,
    date_trunc('day', w.updated_at) as date,
    w.currency,
    w.balance as total_balance,
    COUNT(*) as wallet_entries
FROM wallets w
WHERE w.updated_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY w.id, w.account_id, date_trunc('day', w.updated_at), w.currency, w.balance;

CREATE INDEX IF NOT EXISTS idx_mv_wallet_balance_wallet_date ON mv_wallet_balance_summary(wallet_id, date);
CREATE INDEX IF NOT EXISTS idx_mv_wallet_balance_account_date ON mv_wallet_balance_summary(account_id, date);

-- 2. Pocket Balance Distribution Materialized View
-- Tracks balance distribution across pockets
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_pocket_balance_distribution AS
SELECT
    date_trunc('day', p.updated_at) as date,
    p.currency,
    COUNT(DISTINCT p.account_id) as pockets_count,
    SUM(p.balance) as total_balance,
    AVG(p.balance) as avg_balance,
    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY p.balance) as median_balance,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY p.balance) as p95_balance
FROM pockets p
WHERE p.updated_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY date_trunc('day', p.updated_at), p.currency;

CREATE INDEX IF NOT EXISTS idx_mv_pocket_dist_date_currency ON mv_pocket_balance_distribution(date, currency);

-- 3. Ledger Entry Summary Materialized View
-- Daily ledger summaries for reconciliation
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ledger_daily_summary AS
SELECT
    date_trunc('day', created_at) as date,
    entry_type,
    currency,
    COUNT(*) as entry_count,
    SUM(amount) as total_amount,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) as total_credits,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) as total_debits,
    COUNT(DISTINCT account_id) as unique_wallets
FROM ledger_entries
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), entry_type, currency;

CREATE INDEX IF NOT EXISTS idx_mv_ledger_summary_date ON mv_ledger_daily_summary(date, entry_type);

-- 4. Card Transaction Summary Materialized View
-- Daily card transaction metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_card_transaction_summary AS
SELECT
    date_trunc('day', wt.created_at) as date,
    COUNT(wt.id) as transaction_count,
    SUM(wt.amount) as total_amount,
    AVG(wt.amount) as avg_amount
FROM wallet_transactions wt
WHERE wt.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', wt.created_at);

CREATE INDEX IF NOT EXISTS idx_mv_card_tx_summary_date ON mv_card_transaction_summary(date);

-- 5. Wallet Active Users Materialized View
-- Tracks daily active wallet users
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_active_users AS
SELECT
    date_trunc('day', le.created_at) as date,
    COUNT(DISTINCT le.account_id) as active_wallets,
    SUM(le.amount) as total_volume,
    COUNT(le.id) as total_transactions
FROM ledger_entries le
WHERE le.created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', le.created_at);

CREATE INDEX IF NOT EXISTS idx_mv_wallet_active_date ON mv_wallet_active_users(date);

-- Refresh function for all wallet materialized views
CREATE OR REPLACE FUNCTION refresh_wallet_analytics_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_wallet_balance_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pocket_balance_distribution;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ledger_daily_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_card_transaction_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_wallet_active_users;
END;
$$ LANGUAGE plpgsql;

-- Comments for documentation
COMMENT ON MATERIALIZED VIEW mv_wallet_balance_summary IS 'Daily wallet balance summaries';
COMMENT ON MATERIALIZED VIEW mv_pocket_balance_distribution IS 'Balance distribution across pockets';
COMMENT ON MATERIALIZED VIEW mv_ledger_daily_summary IS 'Daily ledger summaries for reconciliation';
COMMENT ON MATERIALIZED VIEW mv_card_transaction_summary IS 'Daily card transaction metrics';
COMMENT ON MATERIALIZED VIEW mv_wallet_active_users IS 'Daily active wallet users and volume';
COMMENT ON FUNCTION refresh_wallet_analytics_views() IS 'Refresh all wallet analytics materialized views';
