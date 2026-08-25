-- V30__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: upgrade V27 ENABLE-only to FORCE per ADR-0033
ALTER TABLE batch_disbursements ENABLE ROW LEVEL SECURITY;
ALTER TABLE batch_disbursements FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_batch_disbursements ON batch_disbursements;
CREATE POLICY tenant_isolation_batch_disbursements ON batch_disbursements
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE disbursements ENABLE ROW LEVEL SECURITY;
ALTER TABLE disbursements FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_disbursements ON disbursements;
CREATE POLICY tenant_isolation_disbursements ON disbursements
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE scheduled_transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_transfers FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_scheduled_transfers ON scheduled_transfers;
CREATE POLICY tenant_isolation_scheduled_transfers ON scheduled_transfers
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE split_bill_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE split_bill_participants FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_split_bill_participants ON split_bill_participants;
CREATE POLICY tenant_isolation_split_bill_participants ON split_bill_participants
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE split_bills ENABLE ROW LEVEL SECURITY;
ALTER TABLE split_bills FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_split_bills ON split_bills;
CREATE POLICY tenant_isolation_split_bills ON split_bills
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE transaction_archives ENABLE ROW LEVEL SECURITY;
ALTER TABLE transaction_archives FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_transaction_archives ON transaction_archives;
CREATE POLICY tenant_isolation_transaction_archives ON transaction_archives
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_transactions ON transactions;
CREATE POLICY tenant_isolation_transactions ON transactions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE virtual_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE virtual_accounts FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_virtual_accounts ON virtual_accounts;
CREATE POLICY tenant_isolation_virtual_accounts ON virtual_accounts
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
