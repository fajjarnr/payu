-- V12__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: upgrade V10 ENABLE-only to FORCE per ADR-0033
ALTER TABLE credit_scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_scores FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_credit_scores ON credit_scores;
CREATE POLICY tenant_isolation_credit_scores ON credit_scores
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE installment_checkouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE installment_checkouts FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_installment_checkouts ON installment_checkouts;
CREATE POLICY tenant_isolation_installment_checkouts ON installment_checkouts
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE loan_pre_approvals ENABLE ROW LEVEL SECURITY;
ALTER TABLE loan_pre_approvals FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_loan_pre_approvals ON loan_pre_approvals;
CREATE POLICY tenant_isolation_loan_pre_approvals ON loan_pre_approvals
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE loans FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_loans ON loans;
CREATE POLICY tenant_isolation_loans ON loans
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE paylater_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE paylater_accounts FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_paylater_accounts ON paylater_accounts;
CREATE POLICY tenant_isolation_paylater_accounts ON paylater_accounts
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE paylater_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE paylater_transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_paylater_transactions ON paylater_transactions;
CREATE POLICY tenant_isolation_paylater_transactions ON paylater_transactions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE repayment_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE repayment_schedules FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_repayment_schedules ON repayment_schedules;
CREATE POLICY tenant_isolation_repayment_schedules ON repayment_schedules
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
