-- V5__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: support_tickets has tenant_id since V4 but never got RLS per ADR-0033
ALTER TABLE support_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE support_tickets FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_support_tickets ON support_tickets;
CREATE POLICY tenant_isolation_support_tickets ON support_tickets
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
