-- Performance Optimization Indexes for Account Service
-- These indexes optimize common query patterns based on access patterns

-- Note: Only creating indexes for columns that exist in the schema
-- Using standard indexes instead of partial indexes to avoid IMMUTABLE function issues

-- 1. Account lookup indexes (account_number already has unique index, skipping)
-- User ID composite index for account lookups
CREATE INDEX IF NOT EXISTS idx_accounts_user_id_status
ON accounts(user_id, status);

-- 2. Profile lookup indexes
-- Optimizes profile queries by user_id
CREATE INDEX IF NOT EXISTS idx_profiles_user_id
ON profiles(user_id);

-- 3. Composite index for active account queries
-- Includes id, user_id, status, created_at for covering index
CREATE INDEX IF NOT EXISTS idx_accounts_composite
ON accounts(user_id, status, created_at DESC);

-- 4. Recent accounts index for dashboard queries
CREATE INDEX IF NOT EXISTS idx_accounts_created_desc
ON accounts(created_at DESC);

-- 5. BRIN index for time-series data (more efficient for large tables)
CREATE INDEX IF NOT EXISTS idx_accounts_created_brin
ON accounts USING BRIN (created_at);

-- Index maintenance and statistics
-- Update statistics for better query planning
ANALYZE accounts, profiles;

-- Documentation comments
COMMENT ON INDEX idx_accounts_user_id_status IS 'Optimizes account lookups by user_id and status';
COMMENT ON INDEX idx_profiles_user_id IS 'Optimizes profile queries by user_id';
COMMENT ON INDEX idx_accounts_composite IS 'Composite covering index for account queries';
COMMENT ON INDEX idx_accounts_created_desc IS 'Recent accounts for dashboard queries';
COMMENT ON INDEX idx_accounts_created_brin IS 'BRIN index for time-series analysis';
