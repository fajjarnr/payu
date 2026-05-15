-- V9: Add missing settlement columns to merchants table
-- Fixes: Schema-validation: missing column [settlement_account] in table [merchants]
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS settlement_account VARCHAR(32);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS settlement_bank VARCHAR(20);
