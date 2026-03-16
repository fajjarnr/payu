-- V4: Add multi-tenancy support (tenant_id) to all billing tables
-- Default value 'default' ensures backward compatibility with existing rows.

-- bill_payments
ALTER TABLE bill_payments ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_bill_payments_tenant_id ON bill_payments(tenant_id);

-- subscriptions
ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant_id ON subscriptions(tenant_id);

-- subscription_charges
ALTER TABLE subscription_charges ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_subscription_charges_tenant_id ON subscription_charges(tenant_id);

-- subscription_plans
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';
CREATE INDEX IF NOT EXISTS idx_subscription_plans_tenant_id ON subscription_plans(tenant_id);
