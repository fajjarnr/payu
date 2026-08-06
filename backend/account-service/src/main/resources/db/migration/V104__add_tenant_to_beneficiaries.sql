ALTER TABLE beneficiaries
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

CREATE INDEX IF NOT EXISTS idx_beneficiaries_tenant_id
    ON beneficiaries(tenant_id);
