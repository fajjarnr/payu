-- V5: Payment Links table for partner invoice/payment link generation
-- Part of E-15 IMP-040: Payment Link / Invoice Generation

CREATE TABLE IF NOT EXISTS payment_links (
    id              BIGSERIAL PRIMARY KEY,
    slug            VARCHAR(36) NOT NULL UNIQUE,
    partner_id      BIGINT NOT NULL REFERENCES partners(id),
    amount          NUMERIC(19,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'IDR',
    description     VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    customer_name   VARCHAR(200),
    customer_email  VARCHAR(200),
    external_id     VARCHAR(200),
    callback_url    VARCHAR(500),
    redirect_url    VARCHAR(500),
    paid_at         TIMESTAMP,
    payment_method  VARCHAR(50),
    payment_reference VARCHAR(100),
    expires_at      TIMESTAMP NOT NULL,
    tenant_id       VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

-- slug already has UNIQUE constraint which creates an implicit index
CREATE INDEX IF NOT EXISTS idx_payment_link_partner ON payment_links(partner_id);
CREATE INDEX IF NOT EXISTS idx_payment_link_status ON payment_links(status);
CREATE INDEX IF NOT EXISTS idx_payment_link_tenant_id ON payment_links(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_link_expires_at ON payment_links(expires_at);
CREATE INDEX IF NOT EXISTS idx_payment_link_external ON payment_links(partner_id, external_id);
