-- Add tags field to transactions table (IMP-037)
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tags JSONB DEFAULT '[]'::jsonb;

-- Create GIN index for efficient JSONB queries
CREATE INDEX IF NOT EXISTS idx_transactions_tags ON transactions USING GIN (tags);

-- Add comment for documentation
COMMENT ON COLUMN transactions.tags IS 'Transaction tags as JSONB array (predefined categories + custom tags)';
