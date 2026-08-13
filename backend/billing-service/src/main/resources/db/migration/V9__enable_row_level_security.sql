-- V9__enable_row_level_security.sql
-- ARCH-RLS-001: tenant-scoped RLS as defense-in-depth (see wallet V114 for design notes).
-- Policy scopes by tenant_id against the app.tenant_id GUC; NULL (unset) denies all.
-- App connects with a BYPASSRLS/superuser role so app traffic is unaffected;
-- every other DB role is now tenant-scoped. FORCE RLS can be enabled later.

ALTER TABLE bill_payments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_bill_payments ON bill_payments;
CREATE POLICY tenant_isolation_bill_payments ON bill_payments
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE subscription_charges ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_subscription_charges ON subscription_charges;
CREATE POLICY tenant_isolation_subscription_charges ON subscription_charges
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_subscription_plans ON subscription_plans;
CREATE POLICY tenant_isolation_subscription_plans ON subscription_plans
    USING (tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_subscriptions ON subscriptions;
CREATE POLICY tenant_isolation_subscriptions ON subscriptions
    USING (tenant_id = current_setting('app.tenant_id', true));
