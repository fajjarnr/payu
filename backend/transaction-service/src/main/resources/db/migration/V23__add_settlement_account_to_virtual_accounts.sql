-- V23: MVP-003 — VA collection settlement target.
-- VA callback must credit the merchant/partner settlement account, not vanish.
-- Settlement account is captured at VA creation (settlementAccountId on create request),
-- per banking best practice (explicit ledger target, not derived from partnerId).

ALTER TABLE virtual_accounts ADD COLUMN IF NOT EXISTS settlement_account_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_va_settlement_account ON virtual_accounts(settlement_account_id) WHERE settlement_account_id IS NOT NULL;
