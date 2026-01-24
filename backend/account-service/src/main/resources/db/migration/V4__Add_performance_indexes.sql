-- Performance Optimization Indexes for Account Service
-- These indexes optimize common query patterns based on access patterns

-- 1. Account lookup indexes
-- Optimizes queries filtering by customer_id and status
CREATE INDEX IF NOT EXISTS idx_accounts_customer_status
ON accounts(customer_id, status)
WHERE status IN ('ACTIVE', 'PENDING');

-- Optimizes account lookups by account number (very common)
CREATE INDEX IF NOT EXISTS idx_accounts_account_number
ON accounts(account_number)
WHERE status = 'ACTIVE';

-- Optimizes KYC status queries
CREATE INDEX IF NOT EXISTS idx_accounts_kyc_status_created
ON accounts(kyc_status, created_at DESC)
WHERE kyc_status IN ('PENDING', 'APPROVED', 'REJECTED');

-- 2. Balance query indexes
-- Optimizes balance lookups by account_id
CREATE INDEX IF NOT EXISTS idx_balances_account_updated
ON balances(account_id, updated_at DESC);

-- Optimizes multi-currency balance queries
CREATE INDEX IF NOT EXISTS idx_balances_account_currency
ON balances(account_id, currency);

-- 3. Profile lookup indexes
-- Optimizes profile queries by email (for login)
CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_email_unique
ON profiles(email)
WHERE email IS NOT NULL AND deleted_at IS NULL;

-- Optimizes phone number lookups (for verification)
CREATE INDEX IF NOT EXISTS idx_profiles_phone
ON profiles(phone_number)
WHERE phone_number IS NOT NULL;

-- 4. Partial indexes for common query patterns
-- Active accounts only (reduces index size)
CREATE INDEX IF NOT EXISTS idx_accounts_active_only
ON accounts(id, customer_id, status, created_at)
WHERE status = 'ACTIVE';

-- Recent accounts for dashboards (last 90 days)
CREATE INDEX IF NOT EXISTS idx_accounts_recent
ON accounts(created_at DESC, status)
WHERE created_at >= CURRENT_DATE - INTERVAL '90 days';

-- 5. Composite indexes for reporting queries
-- Account statistics queries
CREATE INDEX IF NOT EXISTS idx_accounts_stats
ON accounts(date_trunc('day', created_at), status, kyc_status)
WHERE created_at >= CURRENT_DATE - INTERVAL '365 days';

-- Customer account counts
CREATE INDEX IF NOT EXISTS idx_accounts_customer_count
ON accounts(customer_id, id)
WHERE status = 'ACTIVE';

-- 6. Covering indexes for hot queries
-- Account detail queries (includes all commonly accessed columns)
CREATE INDEX IF NOT EXISTS idx_accounts_covering
ON accounts(customer_id)
INCLUDE (account_number, status, kyc_status, created_at)
WHERE status IN ('ACTIVE', 'PENDING');

-- Balance summary queries
CREATE INDEX IF NOT EXISTS idx_balances_covering
ON balances(account_id)
INCLUDE (currency, amount, updated_at);

-- 7. GIN indexes for JSONB columns (if any)
-- Assuming profiles has JSONB metadata
-- CREATE INDEX IF NOT EXISTS idx_profiles_metadata_gin
-- ON profiles USING GIN (metadata)
-- WHERE metadata IS NOT NULL;

-- 8. BRIN indexes for time-series data (more efficient for large tables)
-- For account creation time series
CREATE INDEX IF NOT EXISTS idx_accounts_created_brin
ON accounts USING BRIN (created_at);

-- For balance updates time series
CREATE INDEX IF NOT EXISTS idx_balances_updated_brin
ON balances USING BRIN (updated_at);

-- 9. Text search indexes (if needed)
-- For full-text search on account names or profile data
-- CREATE INDEX IF NOT EXISTS idx_profiles_name_trgm
-- ON profiles USING GIN (full_name gin_trgm_ops);

-- Index maintenance and statistics
-- Update statistics for better query planning
ANALYZE accounts, balances, profiles;

-- Documentation comments
COMMENT ON INDEX idx_accounts_customer_status IS 'Optimizes customer account lookups by status';
COMMENT ON INDEX idx_accounts_account_number IS 'Fast lookup for active accounts by number';
COMMENT ON INDEX idx_accounts_kyc_status_created IS 'KYC status queries with recent first';
COMMENT ON INDEX idx_balances_account_updated IS 'Balance queries with latest first';
COMMENT ON INDEX idx_accounts_active_only IS 'Partial index for active accounts only';
COMMENT ON INDEX idx_accounts_recent IS 'Recent accounts for dashboard queries';
COMMENT ON INDEX idx_accounts_created_brin IS 'BRIN index for time-series analysis';
COMMENT ON INDEX idx_balances_updated_brin IS 'BRIN index for balance time-series';
