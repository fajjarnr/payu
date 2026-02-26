-- GAP-002: Multi-tenancy / Data Isolation
-- Add tenant_id columns for row-level tenant isolation

-- Partners: tenant_id identifies the organizational tenant scope
ALTER TABLE partners ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'default';
UPDATE partners SET tenant_id = COALESCE(client_id, 'default') WHERE tenant_id = 'default';
CREATE INDEX idx_partner_tenant_id ON partners(tenant_id);

-- Webhook subscriptions: inherit tenant context
ALTER TABLE webhook_subscriptions ADD COLUMN tenant_id VARCHAR(64) DEFAULT 'default';
UPDATE webhook_subscriptions ws SET tenant_id = (
    SELECT COALESCE(p.client_id, 'default') FROM partners p WHERE p.id = ws.partner_id
) WHERE tenant_id = 'default';
CREATE INDEX idx_webhook_sub_tenant_id ON webhook_subscriptions(tenant_id);
