-- V7__force_row_level_security.sql
-- B3.3 RLS FORCE rollout: upgrade V6 ENABLE-only to FORCE per ADR-0033
ALTER TABLE dispute_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE dispute_evidence FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_dispute_evidence ON dispute_evidence;
CREATE POLICY tenant_isolation_dispute_evidence ON dispute_evidence
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE disputes ENABLE ROW LEVEL SECURITY;
ALTER TABLE disputes FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_disputes ON disputes;
CREATE POLICY tenant_isolation_disputes ON disputes
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));

ALTER TABLE refunds ENABLE ROW LEVEL SECURITY;
ALTER TABLE refunds FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_refunds ON refunds;
CREATE POLICY tenant_isolation_refunds ON refunds
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
