-- Add memo field to transactions table (IMP-034)
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS memo VARCHAR(140);

-- Add comment for documentation
COMMENT ON COLUMN transactions.memo IS 'Optional transaction note/memo (max 140 chars)';
