-- V10__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: upgrade V9 ENABLE-only to FORCE per ADR-0033
-- ponytail: RESTRICTIVE + fail-closed (NULL tenant => 0 rows); SYSTEM bypass for reconciler/outbox
-- Each table already has tenant_id; this adds FORCE and upgrades policy to RESTRICTIVE/WITH CHECK

-- bill_payments
ALTER TABLE bill_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE bill_payments FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_bill_payments ON bill_payments;
CREATE POLICY tenant_isolation_bill_payments ON bill_payments
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

-- subscription_charges
ALTER TABLE subscription_charges ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_charges FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_subscription_charges ON subscription_charges;
CREATE POLICY tenant_isolation_subscription_charges ON subscription_charges
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

-- subscription_plans
ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_plans FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_subscription_plans ON subscription_plans;
CREATE POLICY tenant_isolation_subscription_plans ON subscription_plans
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

-- subscriptions
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_subscriptions ON subscriptions;
CREATE POLICY tenant_isolation_subscriptions ON subscriptions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
