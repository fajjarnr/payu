-- Performance Optimization Indexes for Wallet Service
-- These indexes optimize common query patterns for wallet operations

-- 1. Wallet primary indexes
-- Wallet lookups by account_id
CREATE INDEX IF NOT EXISTS idx_wallets_account_created
ON wallets(account_id, created_at DESC)
WHERE status = 'ACTIVE';

-- Active wallets only
CREATE INDEX IF NOT EXISTS idx_wallets_active
ON wallets(id, account_id, status)
WHERE status = 'ACTIVE';

-- 2. Wallet transaction (ledger) indexes
-- Wallet transactions by wallet
CREATE INDEX IF NOT EXISTS idx_wallet_tx_wallet_created
ON wallet_transactions(wallet_id, created_at DESC);

-- Ledger by type (credits vs debits)
CREATE INDEX IF NOT EXISTS idx_wallet_tx_wallet_type_created
ON wallet_transactions(wallet_id, type, created_at DESC);

-- Reference-based lookups (for reconciliation)
CREATE INDEX IF NOT EXISTS idx_wallet_tx_reference
ON wallet_transactions(reference_id)
WHERE reference_id IS NOT NULL;

-- 3. Simple indexes for time-based queries
CREATE INDEX IF NOT EXISTS idx_wallet_tx_created
ON wallet_transactions(created_at DESC);

-- 4. Composite index for wallet and time
CREATE INDEX IF NOT EXISTS idx_wallet_tx_wallet_created_type
ON wallet_transactions(wallet_id, created_at DESC, type);

-- 5. BRIN indexes for time-series data
-- Transaction time series
CREATE INDEX IF NOT EXISTS idx_wallet_tx_created_brin
ON wallet_transactions USING BRIN (created_at);

-- Index maintenance and statistics
ANALYZE wallets, wallet_transactions;

-- Documentation comments
COMMENT ON INDEX idx_wallets_account_created IS 'Wallet lookups by account_id with most recent first';
COMMENT ON INDEX idx_wallet_tx_wallet_created IS 'Critical transaction queries for balance calculation';
COMMENT ON INDEX idx_wallet_tx_created_brin IS 'BRIN index for time-series analysis';
