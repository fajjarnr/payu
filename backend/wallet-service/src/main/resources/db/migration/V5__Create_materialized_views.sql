-- Materialized Views for Wallet Analytics
-- Optimized for reporting and dashboard queries

-- 1. Wallet Balance Summary Materialized View
-- Daily wallet balance summaries
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_balance_summary AS
SELECT
    w.id as wallet_id,
    w.customer_id,
    date_trunc('day', b.updated_at) as date,
    b.currency,
    SUM(b.amount) as total_balance,
    COUNT(b.*) as balance_entries,
    AVG(b.amount) as avg_balance
FROM wallets w
JOIN balances b ON w.id = b.wallet_id
WHERE b.updated_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY w.id, w.customer_id, date_trunc('day', b.updated_at), b.currency;

CREATE INDEX IF NOT EXISTS idx_mv_wallet_balance_wallet_date ON mv_wallet_balance_summary(wallet_id, date);
CREATE INDEX IF NOT EXISTS idx_mv_wallet_balance_customer_date ON mv_wallet_balance_summary(customer_id, date);

-- 2. Pocket Balance Distribution Materialized View
-- Tracks balance distribution across pockets
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_pocket_balance_distribution AS
SELECT
    date_trunc('day', pb.updated_at) as date,
    p.pocket_type,
    COUNT(DISTINCT p.wallet_id) as pockets_count,
    SUM(pb.amount) as total_balance,
    AVG(pb.amount) as avg_balance,
    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY pb.amount) as median_balance,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY pb.amount) as p95_balance
FROM pockets p
JOIN pocket_balances pb ON p.id = pb.pocket_id
WHERE pb.updated_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY date_trunc('day', pb.updated_at), p.pocket_type;

CREATE INDEX IF NOT EXISTS idx_mv_pocket_dist_date_type ON mv_pocket_balance_distribution(date, pocket_type);

-- 3. Ledger Entry Summary Materialized View
-- Daily ledger summaries for reconciliation
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ledger_daily_summary AS
SELECT
    date_trunc('day', created_at) as date,
    entry_type,
    currency,
    COUNT(*) as entry_count,
    SUM(amount) as total_amount,
    SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) as total_credits,
    SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END) as total_debits,
    COUNT(DISTINCT wallet_id) as unique_wallets
FROM ledger_entries
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY date_trunc('day', created_at), entry_type, currency;

CREATE INDEX IF NOT EXISTS idx_mv_ledger_summary_date ON mv_ledger_daily_summary(date, entry_type);

-- 4. Card Transaction Summary Materialized View
-- Daily card transaction metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_card_transaction_summary AS
SELECT
    date_trunc('day', t.created_at) as date,
    c.card_type,
    c.status,
    COUNT(t.id) as transaction_count,
    SUM(t.amount) as total_amount,
    AVG(t.amount) as avg_amount
FROM cards c
JOIN card_transactions t ON c.id = t.card_id
WHERE t.created_at >= CURRENT_DATE - INTERVAL '90 days'
    AND t.status = 'COMPLETED'
GROUP BY date_trunc('day', t.created_at), c.card_type, c.status;

CREATE INDEX IF NOT EXISTS idx_mv_card_tx_summary_date ON mv_card_transaction_summary(date);

-- 5. Wallet Active Users Materialized View
-- Tracks daily active wallet users
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_wallet_active_users AS
SELECT
    date_trunc('day', le.created_at) as date,
    COUNT(DISTINCT le.wallet_id) as active_wallets,
    COUNT(DISTINCT w.customer_id) as active_customers,
    SUM(le.amount) as total_volume,
    COUNT(le.id) as total_transactions
FROM ledger_entries le
JOIN wallets w ON le.wallet_id = w.id
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
COMMENT ON MATERIALIZED VIEW mv_pocket_balance_distribution IS 'Balance distribution across pocket types';
COMMENT ON MATERIALIZED VIEW mv_ledger_daily_summary IS 'Daily ledger summaries for reconciliation';
COMMENT ON MATERIALIZED VIEW mv_card_transaction_summary IS 'Daily card transaction metrics';
COMMENT ON MATERIALIZED VIEW mv_wallet_active_users IS 'Daily active wallet users and volume';
COMMENT ON FUNCTION refresh_wallet_analytics_views() IS 'Refresh all wallet analytics materialized views';
