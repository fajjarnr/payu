-- V22__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: upgrade V20 ENABLE-only to FORCE + add missing tenant tables per ADR-0033
-- ponytail: RESTRICTIVE + SYSTEM bypass; FAIL-closed (NULL => 0 rows)

-- already-ENABLE tables -> FORCE
ALTER TABLE partners ENABLE ROW LEVEL SECURITY;
ALTER TABLE partners FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_partners ON partners;
CREATE POLICY tenant_isolation_partners ON partners
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE webhook_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE webhook_subscriptions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_webhook_subscriptions ON webhook_subscriptions;
CREATE POLICY tenant_isolation_webhook_subscriptions ON webhook_subscriptions
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

-- tenant_id tables that never had RLS (V4-V6, V19)
ALTER TABLE api_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE api_keys FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_api_keys ON api_keys;
CREATE POLICY tenant_isolation_api_keys ON api_keys
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE payment_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_links FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_payment_links ON payment_links;
CREATE POLICY tenant_isolation_payment_links ON payment_links
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchants FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_merchants ON merchants;
CREATE POLICY tenant_isolation_merchants ON merchants
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE snap_reconciliation_cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE snap_reconciliation_cases FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_snap_reconciliation_cases ON snap_reconciliation_cases;
CREATE POLICY tenant_isolation_snap_reconciliation_cases ON snap_reconciliation_cases
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
