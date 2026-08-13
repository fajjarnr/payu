-- V114__enable_row_level_security.sql
-- ACCOUNT-003-RLS / ARCH-RLS-001 (wallet): tenant-scoped RLS as defense-in-depth.
--
-- Design:
--  * RLS is ENABLED on tenant-scoped tables (wallet + ledger data).
--  * The policy scopes rows by tenant_id against the `app.tenant_id` GUC.
--    current_setting('app.tenant_id', true) is NULL when unset, and
--    `USING (NULL)` denies all rows — fail-closed for any non-privileged session
--    that does not declare a tenant.
--  * The app connects as a superuser (dev) or a BYPASSRLS role (OCP), so
--    application traffic is unaffected; every OTHER database role is now
--    tenant-scoped. When the app later sets `app.tenant_id` per session
--    (connection init SQL), FORCE ROW LEVEL SECURITY can be enabled.

ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_wallets ON wallets;
CREATE POLICY tenant_isolation_wallets ON wallets
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE cards ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_cards ON cards;
CREATE POLICY tenant_isolation_cards ON cards
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE ledger_entries ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_ledger_entries ON ledger_entries;
CREATE POLICY tenant_isolation_ledger_entries ON ledger_entries
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE wallet_transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_wallet_transactions ON wallet_transactions;
CREATE POLICY tenant_isolation_wallet_transactions ON wallet_transactions
    USING (tenant_id = current_setting('app.tenant_id', true));
