-- Performance Optimization Indexes for Wallet Service
-- These indexes optimize common query patterns for wallet operations

-- 1. Wallet primary indexes
-- Customer wallet lookups (most common)
CREATE INDEX IF NOT EXISTS idx_wallets_customer_created
ON wallets(customer_id, created_at DESC)
WHERE status = 'ACTIVE';

-- Active wallets only
CREATE INDEX IF NOT EXISTS idx_wallets_active
ON wallets(id, customer_id, status)
WHERE status = 'ACTIVE';

-- 2. Balance lookup indexes
-- Wallet balance queries
CREATE INDEX IF NOT EXISTS idx_balances_wallet_updated
ON balances(wallet_id, updated_at DESC);

-- Multi-currency wallet queries
CREATE INDEX IF NOT EXISTS idx_balances_wallet_currency
ON balances(wallet_id, currency, updated_at DESC);

-- 3. Pocket indexes
-- Pockets by wallet
CREATE INDEX IF NOT EXISTS idx_pockets_wallet_type
ON pockets(wallet_id, pocket_type, status)
WHERE status = 'ACTIVE';

-- Pocket balance lookups
CREATE INDEX IF NOT EXISTS idx_pocket_balances_pocket
ON pocket_balances(pocket_id, updated_at DESC);

-- Pocket type analytics
CREATE INDEX IF NOT EXISTS idx_pockets_type_created
ON pockets(pocket_type, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- 4. Card operation indexes
-- Active cards by wallet
CREATE INDEX IF NOT EXISTS idx_cards_wallet_status
ON cards(wallet_id, status, created_at DESC)
WHERE status IN ('ACTIVE', 'FROZEN');

-- Card number lookups (for transactions)
CREATE INDEX IF NOT EXISTS idx_cards_number_unique
ON cards(card_number)
WHERE status = 'ACTIVE' AND deleted_at IS NULL;

-- Card transactions by card
CREATE INDEX IF NOT EXISTS idx_card_transactions_card_created
ON card_transactions(card_id, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '180 days';

-- Card transactions by status (for reconciliation)
CREATE INDEX IF NOT EXISTS idx_card_transactions_status
ON card_transactions(status, created_at DESC)
WHERE status IN ('PENDING', 'PROCESSING');

-- 5. Ledger entry indexes
-- Wallet ledger queries (critical for balance calculations)
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_created
ON ledger_entries(wallet_id, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '365 days';

-- Ledger by entry type (credits vs debits)
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_type_created
ON ledger_entries(wallet_id, entry_type, created_at DESC)
WHERE created_at >= CURRENT_DATE - INTERVAL '180 days';

-- Reference-based lookups (for reconciliation)
CREATE INDEX IF NOT EXISTS idx_ledger_reference
ON ledger_entries(reference_id)
WHERE reference_id IS NOT NULL;

-- 6. Composite indexes for reporting
-- Daily wallet summaries
CREATE INDEX IF NOT EXISTS idx_ledger_daily_summary
ON ledger_entries(date_trunc('day', created_at), entry_type)
WHERE created_at >= CURRENT_DATE - INTERVAL '365 days';

-- Customer wallet balances
CREATE INDEX IF NOT EXISTS idx_balances_customer
ON balances(wallet_id, currency, amount)
JOIN wallets w ON balances.wallet_id = w.id
WHERE w.status = 'ACTIVE';

-- 7. Partial indexes for common patterns
-- Recent ledger entries (reduces index size)
CREATE INDEX IF NOT EXISTS idx_ledger_recent
ON ledger_entries(created_at DESC, entry_type, amount)
WHERE created_at >= CURRENT_DATE - INTERVAL '30 days';

-- Active pockets only
CREATE INDEX IF NOT EXISTS idx_pockets_active
ON pockets(id, wallet_id, pocket_type, balance)
WHERE status = 'ACTIVE';

-- Pending card transactions
CREATE INDEX IF NOT EXISTS idx_card_tx_pending
ON card_transactions(created_at, amount)
WHERE status = 'PENDING';

-- 8. Covering indexes for hot queries
-- Wallet detail queries
CREATE INDEX IF NOT EXISTS idx_wallets_covering
ON wallets(customer_id)
INCLUDE (status, created_at, balance)
WHERE status = 'ACTIVE';

-- Balance queries with currency
CREATE INDEX IF NOT EXISTS idx_balances_covering
ON balances(wallet_id, currency)
INCLUDE (amount, updated_at);

-- Ledger statements
CREATE INDEX IF NOT EXISTS idx_ledger_statement_covering
ON ledger_entries(wallet_id, created_at DESC)
INCLUDE (entry_type, amount, reference_id, balance_after)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- 9. BRIN indexes for time-series data
-- Ledger entry time series
CREATE INDEX IF NOT EXISTS idx_ledger_created_brin
ON ledger_entries USING BRIN (created_at);

-- Balance update time series
CREATE INDEX IF NOT EXISTS idx_balances_updated_brin
ON balances USING BRIN (updated_at);

-- Transaction time series
CREATE INDEX IF NOT EXISTS idx_card_tx_created_brin
ON card_transactions USING BRIN (created_at);

-- 10. Unique constraints and indexes
-- Ensure unique card numbers
CREATE UNIQUE INDEX IF NOT EXISTS idx_cards_card_number_unique
ON cards(card_number)
WHERE deleted_at IS NULL;

-- Unique wallet per customer (if single wallet per customer)
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_wallets_customer_unique
-- ON wallets(customer_id)
-- WHERE status = 'ACTIVE';

-- Index maintenance and statistics
ANALYZE wallets, balances, pockets, pocket_balances, cards, card_transactions, ledger_entries;

-- Documentation comments
COMMENT ON INDEX idx_wallets_customer_created IS 'Customer wallet lookups with most recent first';
COMMENT ON INDEX idx_balances_wallet_updated IS 'Balance queries with latest updates';
COMMENT ON INDEX idx_pockets_wallet_type IS 'Active pockets by wallet and type';
COMMENT ON INDEX idx_cards_wallet_status IS 'Active cards by wallet';
COMMENT ON INDEX idx_ledger_wallet_created IS 'Critical ledger queries for balance calculation';
COMMENT ON INDEX idx_ledger_recent IS 'Recent ledger entries (30 days) for dashboard';
COMMENT ON INDEX idx_card_transactions_card_created IS 'Card transaction history (180 days)';
COMMENT ON INDEX idx_ledger_created_brin IS 'BRIN index for time-series analysis';
