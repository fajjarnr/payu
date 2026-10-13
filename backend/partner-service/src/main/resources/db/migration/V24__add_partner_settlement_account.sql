ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS settlement_account_id VARCHAR(64);
