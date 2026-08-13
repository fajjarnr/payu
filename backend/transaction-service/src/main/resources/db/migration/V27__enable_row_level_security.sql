-- V27__enable_row_level_security.sql
-- ARCH-RLS-001: tenant-scoped RLS as defense-in-depth (see wallet V114 for design notes).
-- Policy scopes by tenant_id against the app.tenant_id GUC; NULL (unset) denies all.
-- App connects with a BYPASSRLS/superuser role so app traffic is unaffected;
-- every other DB role is now tenant-scoped. FORCE RLS can be enabled later.

ALTER TABLE batch_disbursements ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_batch_disbursements ON batch_disbursements
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE disbursements ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_disbursements ON disbursements
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE scheduled_transfers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_scheduled_transfers ON scheduled_transfers
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE split_bill_participants ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_split_bill_participants ON split_bill_participants
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE split_bills ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_split_bills ON split_bills
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE transaction_archives ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_transaction_archives ON transaction_archives
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_transactions ON transactions
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE virtual_accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_virtual_accounts ON virtual_accounts
    USING (tenant_id = current_setting('app.tenant_id', true));
