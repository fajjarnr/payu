-- V7: Add optimistic locking version column to loyalty_points
-- Fixes: Schema-validation: missing column [version] in table [loyalty_points]
-- Ref: BUG-BE-186
ALTER TABLE loyalty_points ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
