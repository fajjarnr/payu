-- V8__create_journal_entries_and_chart_of_accounts.sql
-- IMP-001: True Double-Entry Ledger - JournalEntry parent table
-- IMP-002: Chart of Accounts (CoA) for GL classification
-- Flyway migration for wallet-service

-- ============================================================
-- 1. JOURNAL ENTRIES TABLE (IMP-001)
-- Parent entity that groups paired DEBIT + CREDIT ledger entries.
-- Constraint: sum(debit) == sum(credit) enforced at application layer.
-- ============================================================

CREATE TABLE IF NOT EXISTS journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_number VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(500),
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    CONSTRAINT chk_journal_status CHECK (status IN ('PENDING', 'POSTED', 'REVERSED'))
);

CREATE INDEX IF NOT EXISTS idx_journal_reference ON journal_entries(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_journal_posted_at ON journal_entries(posted_at);
CREATE INDEX IF NOT EXISTS idx_journal_status ON journal_entries(status);
CREATE INDEX IF NOT EXISTS idx_journal_number ON journal_entries(journal_number);

COMMENT ON TABLE journal_entries IS 'Double-entry journal: groups paired DEBIT+CREDIT ledger entries. sum(debit) must equal sum(credit) per journal.';

-- ============================================================
-- 2. CHART OF ACCOUNTS TABLE (IMP-002)
-- Hierarchical GL account classification for banking operations.
-- Follows PSAK (Indonesian Accounting Standards) structure.
-- ============================================================

CREATE TABLE IF NOT EXISTS chart_of_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    account_type VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    parent_id UUID,
    level INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    normal_balance VARCHAR(10) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT chk_normal_balance CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
    CONSTRAINT fk_coa_parent FOREIGN KEY (parent_id) REFERENCES chart_of_accounts(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_coa_code ON chart_of_accounts(code);
CREATE INDEX IF NOT EXISTS idx_coa_account_type ON chart_of_accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_coa_parent_id ON chart_of_accounts(parent_id);
CREATE INDEX IF NOT EXISTS idx_coa_category ON chart_of_accounts(category);

COMMENT ON TABLE chart_of_accounts IS 'Chart of Accounts for GL classification. Hierarchical code structure for banking operations per PSAK.';

-- ============================================================
-- 3. ALTER LEDGER_ENTRIES: Add journal_entry_id and coa_code
-- Links existing ledger entries to journals and CoA.
-- ============================================================

ALTER TABLE ledger_entries
    ADD COLUMN IF NOT EXISTS journal_entry_id UUID,
    ADD COLUMN IF NOT EXISTS coa_code VARCHAR(20);

-- Foreign key to journal_entries
ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_journal
    FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ledger_journal_id ON ledger_entries(journal_entry_id);
CREATE INDEX IF NOT EXISTS idx_ledger_coa_code ON ledger_entries(coa_code);

-- Also fix the account_id column type: the original V3 migration created it as UUID
-- but the entity uses String. Change column type to VARCHAR to match.
-- Must drop the existing FK constraint first (created in V3 as fk_ledger_account
-- referencing wallets.id which is UUID), then alter column type, then re-add FK.
ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS fk_ledger_account;

ALTER TABLE ledger_entries
    ALTER COLUMN account_id TYPE VARCHAR(50) USING account_id::VARCHAR;

-- Re-add the FK constraint now that both sides are compatible types
-- (account_id is now VARCHAR, so we reference wallets.account_id or just drop the FK
-- since the ledger can reference accounts by string identifier)
-- Note: We intentionally do NOT re-add fk_ledger_account because account_id
-- now stores string identifiers that may not directly map to wallets.id (UUID).

-- ============================================================
-- 4. SEED DATA: Standard Banking Chart of Accounts
-- ============================================================

-- Level 1: Top-level categories
INSERT INTO chart_of_accounts (id, code, name, description, account_type, category, parent_id, level, active, normal_balance, currency)
VALUES
    -- ASSETS (1xxx) - Normal Balance: DEBIT
    ('a0000001-0000-0000-0000-000000000001', '1000', 'Assets', 'All asset accounts', 'ASSET', NULL, NULL, 1, true, 'DEBIT', 'IDR'),
    ('a0000001-0000-0000-0000-000000000011', '1100', 'User Wallets', 'Customer wallet balances', 'ASSET', 'USER_WALLET', 'a0000001-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR'),
    ('a0000001-0000-0000-0000-000000000012', '1200', 'Bank Accounts', 'PayU bank account balances', 'ASSET', 'BANK_ACCOUNT', 'a0000001-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR'),
    ('a0000001-0000-0000-0000-000000000013', '1300', 'Escrow Receivables', 'Pending escrow collections', 'ASSET', 'ESCROW_RECEIVABLE', 'a0000001-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR'),
    ('a0000001-0000-0000-0000-000000000014', '1400', 'Settlement Receivables', 'Pending settlement from partners', 'ASSET', 'SETTLEMENT_RECEIVABLE', 'a0000001-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR'),

    -- LIABILITIES (2xxx) - Normal Balance: CREDIT
    ('a0000002-0000-0000-0000-000000000001', '2000', 'Liabilities', 'All liability accounts', 'LIABILITY', NULL, NULL, 1, true, 'CREDIT', 'IDR'),
    ('a0000002-0000-0000-0000-000000000011', '2100', 'Escrow Holdings', 'Funds held in escrow for buyers', 'LIABILITY', 'ESCROW_HOLDING', 'a0000002-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000002-0000-0000-0000-000000000012', '2200', 'Merchant Payables', 'Amounts owed to merchants', 'LIABILITY', 'MERCHANT_PAYABLE', 'a0000002-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000002-0000-0000-0000-000000000013', '2300', 'Partner Payables', 'Amounts owed to partners (TokoBapak, Nobar)', 'LIABILITY', 'PARTNER_PAYABLE', 'a0000002-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000002-0000-0000-0000-000000000014', '2400', 'Fee Payables', 'Accrued fee obligations', 'LIABILITY', 'FEE_PAYABLE', 'a0000002-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),

    -- EQUITY (3xxx) - Normal Balance: CREDIT
    ('a0000003-0000-0000-0000-000000000001', '3000', 'Equity', 'Owner equity accounts', 'EQUITY', NULL, NULL, 1, true, 'CREDIT', 'IDR'),
    ('a0000003-0000-0000-0000-000000000011', '3100', 'Capital', 'Paid-in capital', 'EQUITY', 'CAPITAL', 'a0000003-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000003-0000-0000-0000-000000000012', '3200', 'Retained Earnings', 'Accumulated profits', 'EQUITY', 'RETAINED_EARNINGS', 'a0000003-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),

    -- REVENUE (4xxx) - Normal Balance: CREDIT
    ('a0000004-0000-0000-0000-000000000001', '4000', 'Revenue', 'All revenue accounts', 'REVENUE', NULL, NULL, 1, true, 'CREDIT', 'IDR'),
    ('a0000004-0000-0000-0000-000000000011', '4100', 'Transaction Fees', 'Fees from payment processing', 'REVENUE', 'TRANSACTION_FEE', 'a0000004-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000004-0000-0000-0000-000000000012', '4200', 'Interest Income', 'Interest earned on deposits', 'REVENUE', 'INTEREST_INCOME', 'a0000004-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000004-0000-0000-0000-000000000013', '4300', 'FX Spread Income', 'Foreign exchange margin income', 'REVENUE', 'FX_SPREAD', 'a0000004-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),
    ('a0000004-0000-0000-0000-000000000014', '4400', 'Service Fees', 'Service and subscription fees', 'REVENUE', 'SERVICE_FEE', 'a0000004-0000-0000-0000-000000000001', 2, true, 'CREDIT', 'IDR'),

    -- EXPENSES (5xxx) - Normal Balance: DEBIT
    ('a0000005-0000-0000-0000-000000000001', '5000', 'Expenses', 'All expense accounts', 'EXPENSE', NULL, NULL, 1, true, 'DEBIT', 'IDR'),
    ('a0000005-0000-0000-0000-000000000011', '5100', 'Operational Costs', 'Day-to-day operating expenses', 'EXPENSE', 'OPERATIONAL_COST', 'a0000005-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR'),
    ('a0000005-0000-0000-0000-000000000012', '5200', 'Settlement Costs', 'Bank transfer and settlement fees', 'EXPENSE', 'SETTLEMENT_COST', 'a0000005-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR'),
    ('a0000005-0000-0000-0000-000000000013', '5300', 'Refund Costs', 'Refund processing costs', 'EXPENSE', 'REFUND_COST', 'a0000005-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR')

ON CONFLICT (code) DO NOTHING;
