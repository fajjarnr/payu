-- V20__enable_row_level_security.sql
-- ARCH-RLS-001: tenant-scoped RLS as defense-in-depth (see wallet V114 for design notes).
-- Policy scopes by tenant_id against the app.tenant_id GUC; NULL (unset) denies all.
-- App connects with a BYPASSRLS/superuser role so app traffic is unaffected;
-- every other DB role is now tenant-scoped. FORCE RLS can be enabled later.

ALTER TABLE partners ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_partners ON partners
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE webhook_subscriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_webhook_subscriptions ON webhook_subscriptions
    USING (tenant_id = current_setting('app.tenant_id', true));
