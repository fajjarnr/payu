-- DEV-106: Add columns originally created by Hibernate ddl-auto=update.
-- Every statement is idempotent (ADD COLUMN IF NOT EXISTS).
ALTER TABLE partners ADD COLUMN IF NOT EXISTS partner_code VARCHAR(64);
ALTER TABLE partners ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION';
ALTER TABLE partners ADD COLUMN IF NOT EXISTS webhook_url VARCHAR(500);
CREATE UNIQUE INDEX IF NOT EXISTS idx_partners_partner_code ON partners(partner_code);
