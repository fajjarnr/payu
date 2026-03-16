-- V6: Add multi-tenancy support to lending-service tables

-- loans
ALTER TABLE loans ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_loans_tenant_id ON loans(tenant_id);

-- credit_scores
ALTER TABLE credit_scores ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_credit_scores_tenant_id ON credit_scores(tenant_id);

-- paylater_accounts
ALTER TABLE paylater_accounts ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_paylater_accounts_tenant_id ON paylater_accounts(tenant_id);

-- paylater_transactions
ALTER TABLE paylater_transactions ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_paylater_transactions_tenant_id ON paylater_transactions(tenant_id);

-- repayment_schedules
ALTER TABLE repayment_schedules ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_repayment_schedules_tenant_id ON repayment_schedules(tenant_id);

-- loan_pre_approvals
ALTER TABLE loan_pre_approvals ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_loan_pre_approvals_tenant_id ON loan_pre_approvals(tenant_id);

-- installment_checkouts
ALTER TABLE installment_checkouts ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_installment_checkouts_tenant_id ON installment_checkouts(tenant_id);
