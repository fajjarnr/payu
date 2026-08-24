-- V121__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: upgrade V114 ENABLE-only tables to FORCE per ADR-0033
-- V116 already forced wallets; this forces remaining 3 + any tenant_id tables lacking RLS
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallets FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_wallets ON wallets;
DROP POLICY IF EXISTS wallet_tenant_isolation ON wallets;
CREATE POLICY tenant_isolation_wallets ON wallets
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE cards FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_cards ON cards;
CREATE POLICY tenant_isolation_cards ON cards
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE ledger_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE ledger_entries FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_ledger_entries ON ledger_entries;
CREATE POLICY tenant_isolation_ledger_entries ON ledger_entries
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE wallet_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_wallet_transactions ON wallet_transactions;
CREATE POLICY tenant_isolation_wallet_transactions ON wallet_transactions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

-- ancillary tenant tables that had tenant_id but never had RLS (V9, V10, V12)
ALTER TABLE escrow_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE escrow_transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_escrow_transactions ON escrow_transactions;
CREATE POLICY tenant_isolation_escrow_transactions ON escrow_transactions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE settlement_batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE settlement_batches FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_settlement_batches ON settlement_batches;
CREATE POLICY tenant_isolation_settlement_batches ON settlement_batches
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE revenue_splits ENABLE ROW LEVEL SECURITY;
ALTER TABLE revenue_splits FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_revenue_splits ON revenue_splits;
CREATE POLICY tenant_isolation_revenue_splits ON revenue_splits
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE split_payment_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE split_payment_rules FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_split_payment_rules ON split_payment_rules;
CREATE POLICY tenant_isolation_split_payment_rules ON split_payment_rules
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE split_payment_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE split_payment_executions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_split_payment_executions ON split_payment_executions;
CREATE POLICY tenant_isolation_split_payment_executions ON split_payment_executions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
