-- V105: Blind index (HMAC-SHA256) for PII lookup parity (ACCOUNT-001)
-- email/phone_number are AES-GCM encrypted at rest with a random IV, so the old
-- equality queries (findByEmail/findByPhoneNumber) and the dropped unique
-- constraints could never match. A deterministic blind index column restores
-- exact-match lookup and uniqueness per tenant.
-- Backfill note: requires plaintext, which migrations cannot read (columns are
-- encrypted). No production data exists yet (ACCOUNT-007); dev data is
-- disposable. A re-index runner can be added before any prod migration.

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_hash
    ON users(tenant_id, email_hash) WHERE email_hash IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_phone_number_hash
    ON users(tenant_id, phone_number_hash) WHERE phone_number_hash IS NOT NULL;

COMMENT ON COLUMN users.email_hash IS 'HMAC-SHA256 blind index over normalized email for equality lookup/uniqueness';
COMMENT ON COLUMN users.phone_number_hash IS 'HMAC-SHA256 blind index over normalized phone number for equality lookup/uniqueness';
