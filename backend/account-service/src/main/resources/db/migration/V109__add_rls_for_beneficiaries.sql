-- ARCH-GLOBAL-007 RLS 4/4: beneficiaries
ALTER TABLE beneficiaries ENABLE ROW LEVEL SECURITY;
ALTER TABLE beneficiaries FORCE ROW LEVEL SECURITY;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policename = 'tenant_isolation_beneficiaries') THEN
    CREATE POLICY tenant_isolation_beneficiaries ON beneficiaries
      USING (tenant_id = current_setting('app.tenant_id', true))
      WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
  END IF;
END $$;
