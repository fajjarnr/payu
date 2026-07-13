-- Reconcile columns and indexes originally created by Hibernate ddl-auto=update.
-- Existing Flyway-managed environments are safe — every statement is idempotent.

-- Columns missing from V1 that Hibernate auto-generated via ddl-auto=update
ALTER TABLE partners ADD COLUMN IF NOT EXISTS partner_code VARCHAR(64);
ALTER TABLE partners ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION';
ALTER TABLE partners ADD COLUMN IF NOT EXISTS webhook_url VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_partners_client_id
    ON partners(client_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_partners_partner_code
    ON partners(partner_code);
CREATE INDEX IF NOT EXISTS idx_partner_certificates_partner_id
    ON partner_certificates(partner_id);
CREATE INDEX IF NOT EXISTS idx_webhook_sub_partner
    ON webhook_subscriptions(partner_id);
CREATE INDEX IF NOT EXISTS idx_webhook_sub_active
    ON webhook_subscriptions(active);
CREATE INDEX IF NOT EXISTS idx_webhook_sub_tenant_id
    ON webhook_subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_delivery_retry_eligible
    ON webhook_deliveries(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_api_key_tenant
    ON api_keys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_key_active
    ON api_keys(key_hash, status);
CREATE INDEX IF NOT EXISTS idx_api_key_sandbox
    ON api_keys(sandbox);
CREATE INDEX IF NOT EXISTS idx_api_key_sandbox_active
    ON api_keys(sandbox, status);
CREATE INDEX IF NOT EXISTS idx_payment_link_external
    ON payment_links(partner_id, external_id);
