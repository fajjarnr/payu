-- V15: Add multi-tenancy support to all transaction-service tables
-- Each table gets a tenant_id column with default 'default' for backward compatibility

-- transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_transactions_tenant_id ON transactions(tenant_id);

-- scheduled_transfers
ALTER TABLE scheduled_transfers ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_tenant_id ON scheduled_transfers(tenant_id);

-- split_bills
ALTER TABLE split_bills ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_split_bills_tenant_id ON split_bills(tenant_id);

-- split_bill_participants
ALTER TABLE split_bill_participants ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_split_bill_participants_tenant_id ON split_bill_participants(tenant_id);

-- transaction_archives
ALTER TABLE transaction_archives ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_transaction_archives_tenant_id ON transaction_archives(tenant_id);

-- virtual_accounts
ALTER TABLE virtual_accounts ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_virtual_accounts_tenant_id ON virtual_accounts(tenant_id);

-- disbursements
ALTER TABLE disbursements ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_disbursements_tenant_id ON disbursements(tenant_id);

-- batch_disbursements
ALTER TABLE batch_disbursements ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_batch_disbursements_tenant_id ON batch_disbursements(tenant_id);
