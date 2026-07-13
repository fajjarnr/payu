-- Reconcile indexes missing from the schema originally created by Hibernate.
-- Existing Flyway-managed environments remain safe because every statement is idempotent.
CREATE INDEX IF NOT EXISTS idx_partners_client_id
    ON partners(client_id);
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
