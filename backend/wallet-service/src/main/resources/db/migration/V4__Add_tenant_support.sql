-- Add tenant_id column for multitenancy support

-- Add tenant_id to wallets table
ALTER TABLE wallets ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_wallets_tenant_id ON wallets(tenant_id);

-- Add tenant_id to cards table
ALTER TABLE cards ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_cards_tenant_id ON cards(tenant_id);

-- Add tenant_id to wallet_transactions table
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_tenant_id ON wallet_transactions(tenant_id);

-- Add tenant_id to ledger_entries table
ALTER TABLE ledger_entries ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_ledger_entries_tenant_id ON ledger_entries(tenant_id);

-- Update existing records to use default tenant
UPDATE wallets SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE cards SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE wallet_transactions SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE ledger_entries SET tenant_id = 'default' WHERE tenant_id IS NULL;
