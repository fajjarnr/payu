-- V6: Merchant and QR Payment tables for dynamic QRIS
-- Part of E-15 IMP-045: Dynamic QR for Merchants

CREATE TABLE merchants (
    id                      BIGSERIAL PRIMARY KEY,
    partner_id              BIGINT NOT NULL REFERENCES partners(id),
    merchant_code           VARCHAR(20) NOT NULL UNIQUE,
    business_name           VARCHAR(200) NOT NULL,
    business_type           VARCHAR(100),
    category                VARCHAR(30) NOT NULL,
    address                 VARCHAR(300) NOT NULL,
    city                    VARCHAR(100),
    postal_code             VARCHAR(10),
    pic_name                VARCHAR(200),
    pic_phone               VARCHAR(20),
    pic_email               VARCHAR(200),
    settlement_account_id   VARCHAR(64),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    static_qr_code          VARCHAR(500),
    tenant_id               VARCHAR(64),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP
);

CREATE INDEX idx_merchant_partner_id ON merchants(partner_id);
-- merchant_code already has UNIQUE constraint which creates an implicit index
CREATE INDEX idx_merchant_status ON merchants(status);
CREATE INDEX idx_merchant_tenant_id ON merchants(tenant_id);

CREATE TABLE merchant_qr_payments (
    id                  BIGSERIAL PRIMARY KEY,
    reference_id        VARCHAR(36) NOT NULL UNIQUE,
    merchant_id         BIGINT NOT NULL REFERENCES merchants(id),
    amount              NUMERIC(19,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'IDR',
    description         VARCHAR(500),
    qr_content          VARCHAR(1000) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payer_account_id    VARCHAR(64),
    payment_reference   VARCHAR(100),
    paid_at             TIMESTAMP,
    expires_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qr_payment_merchant_id ON merchant_qr_payments(merchant_id);
-- reference_id already has UNIQUE constraint which creates an implicit index
CREATE INDEX idx_qr_payment_status ON merchant_qr_payments(status);
CREATE INDEX idx_qr_payment_expires_at ON merchant_qr_payments(expires_at);
