-- Add index on phone_number for P2P lookup (IMP-036)
-- Note: phone_number is already encrypted, this index helps with exact match lookups
CREATE INDEX IF NOT EXISTS idx_users_phone_lookup ON users(phone_number) WHERE phone_number IS NOT NULL;

-- Add account lookup metadata column for P2P transfers
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS allow_phone_lookup BOOLEAN DEFAULT TRUE;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS qr_code_hash VARCHAR(64);

-- Create unique index on QR code hash
CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_qr_hash ON accounts(qr_code_hash) WHERE qr_code_hash IS NOT NULL;

-- Add comments
COMMENT ON COLUMN accounts.allow_phone_lookup IS 'Whether this account allows phone number lookup for P2P transfers';
COMMENT ON COLUMN accounts.qr_code_hash IS 'Hash for QR code P2P payments (payu://p2p?account={id}&check={hash})';
