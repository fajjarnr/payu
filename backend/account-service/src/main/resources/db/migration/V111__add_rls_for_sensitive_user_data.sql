-- ACC-HARDEN-001: sensitive_user_data RLS
-- Table lacked tenant_id + RLS; add tenant_id (default 'default' for backfill) + FORCE RLS
-- ponytail: user_id→tenant via users join in app; ceiling: add FK+trigger if strict tenant mapping needed
ALTER TABLE sensitive_user_data ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_sensitive_user_data_tenant_id ON sensitive_user_data(tenant_id);
ALTER TABLE sensitive_user_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE sensitive_user_data FORCE ROW LEVEL SECURITY;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policename = 'tenant_isolation_sensitive_user_data') THEN
    CREATE POLICY tenant_isolation_sensitive_user_data ON sensitive_user_data
      USING (tenant_id = current_setting('app.tenant_id', true))
      WITH CHECK (tenant_id = current_setting('app.tenant_id', true));
  END IF;
END $$;
