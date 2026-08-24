-- V112__force_rls_profiles.sql
-- B3.3 RLS FORCE rollout: profiles has tenant_id since V2 but never got RLS. Add FORCE per ADR-0033.
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE profiles FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_profiles ON profiles;
CREATE POLICY tenant_isolation_profiles ON profiles
    USING (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true))
    WITH CHECK (current_setting('app.tenant_id', true) = 'SYSTEM' OR tenant_id = current_setting('app.tenant_id', true));
