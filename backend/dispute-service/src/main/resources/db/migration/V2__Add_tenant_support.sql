-- Add multi-tenancy support to all dispute-service tables

-- disputes
ALTER TABLE disputes ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_disputes_tenant_id ON disputes(tenant_id);

-- refunds
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_refunds_tenant_id ON refunds(tenant_id);

-- dispute_evidence
ALTER TABLE dispute_evidence ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_dispute_evidence_tenant_id ON dispute_evidence(tenant_id);
