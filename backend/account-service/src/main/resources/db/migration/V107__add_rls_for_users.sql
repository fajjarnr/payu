-- ARCH-GLOBAL-007 RLS 2/4: enable FORCE RLS for users (ponytail minimal, next tables follow per ADR-0033)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policename = 'tenant_isolation_users') THEN
    CREATE POLICY tenant_isolation_users ON users
      USING (tenant_id = current_setting('app.tenant_id', true))
      WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
  END IF;
END $$;
