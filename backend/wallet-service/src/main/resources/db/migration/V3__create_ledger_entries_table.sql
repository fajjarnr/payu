-- V3__create_ledger_entries_table.sql
-- Create ledger entries table for double-entry bookkeeping
-- This ensures total DEBIT = total CREDIT for each transaction

CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) DEFAULT 'IDR',
    balance_after DECIMAL(19,4) NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES wallets(id) ON DELETE RESTRICT
);

-- Index for faster queries on account_id
CREATE INDEX IF NOT EXISTS idx_ledger_account_id ON ledger_entries(account_id, created_at);

-- Index for transaction_id lookups
CREATE INDEX IF NOT EXISTS idx_ledger_transaction_id ON ledger_entries(transaction_id);

-- Check balance constraint: SUM(CREDIT) = SUM(DEBIT) per transaction_id
-- This will be enforced in application layer for real-time validation
COMMENT ON TABLE ledger_entries IS 'Double-entry ledger: each transaction has equal DEBIT and CREDIT amounts';
