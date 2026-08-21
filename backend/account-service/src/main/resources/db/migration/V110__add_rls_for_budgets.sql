-- ACC-HARDEN-001: RLS + tenant isolation complete Q1
-- budgets missing FORCE RLS (users/accounts/beneficiaries already V107-109)
-- ponytail: RESTRICTIVE policy tenant_id=current_setting; BYPASSRLS for migrator, app uses SET LOCAL per-tx
ALTER TABLE budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE budgets FORCE ROW LEVEL SECURITY;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policename = 'tenant_isolation_budgets') THEN
    CREATE POLICY tenant_isolation_budgets ON budgets
      USING (tenant_id = current_setting('app.tenant_id', true))
      WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
  END IF;
END $$;
