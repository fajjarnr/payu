-- ADR-0033 RLS scaffold 1/27 — ponytail: single table example, add 26 more tables + FORCE RLS + restrictive policy when PG RLS GUC integration ready
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallets FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS wallet_tenant_isolation ON wallets;
CREATE POLICY wallet_tenant_isolation ON wallets AS RESTRICTIVE USING (tenant_id = current_setting('app.tenant_id', true) OR current_setting('app.tenant_id', true) = 'default');
