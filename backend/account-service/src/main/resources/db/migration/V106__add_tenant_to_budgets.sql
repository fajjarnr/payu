-- V106: tenant scoping for budgets (ACCOUNT-003)
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_budgets_tenant_id ON budgets(tenant_id);

COMMENT ON COLUMN budgets.tenant_id IS 'Tenant owning this budget; enforced by the Hibernate tenant filter';
