-- V10__enable_row_level_security.sql
-- ARCH-RLS-001: tenant-scoped RLS as defense-in-depth (see wallet V114 for design notes).
-- Policy scopes by tenant_id against the app.tenant_id GUC; NULL (unset) denies all.
-- App connects with a BYPASSRLS/superuser role so app traffic is unaffected;
-- every other DB role is now tenant-scoped. FORCE RLS can be enabled later.

ALTER TABLE credit_scores ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_credit_scores ON credit_scores;
CREATE POLICY tenant_isolation_credit_scores ON credit_scores
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE installment_checkouts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_installment_checkouts ON installment_checkouts;
CREATE POLICY tenant_isolation_installment_checkouts ON installment_checkouts
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE loan_pre_approvals ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_loan_pre_approvals ON loan_pre_approvals;
CREATE POLICY tenant_isolation_loan_pre_approvals ON loan_pre_approvals
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE loans ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_loans ON loans;
CREATE POLICY tenant_isolation_loans ON loans
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE paylater_accounts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_paylater_accounts ON paylater_accounts;
CREATE POLICY tenant_isolation_paylater_accounts ON paylater_accounts
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE paylater_transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_paylater_transactions ON paylater_transactions;
CREATE POLICY tenant_isolation_paylater_transactions ON paylater_transactions
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE repayment_schedules ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_repayment_schedules ON repayment_schedules;
CREATE POLICY tenant_isolation_repayment_schedules ON repayment_schedules
    USING (tenant_id = current_setting('app.tenant_id', true));
