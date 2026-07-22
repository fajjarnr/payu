-- V10: Virtual Account table + Payment Expiry support
-- Part of E-15 IMP-042: Virtual Account Payment Collection
-- Part of E-15 IMP-044: Payment Expiry & Auto-Cancel

CREATE TABLE virtual_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    va_number           VARCHAR(30) NOT NULL UNIQUE,
    bank_code           VARCHAR(10) NOT NULL,
    bank_name           VARCHAR(50),
    partner_id          UUID NOT NULL,
    external_id         VARCHAR(200),
    amount              NUMERIC(19,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'IDR',
    description         VARCHAR(500),
    customer_name       VARCHAR(200),
    customer_email      VARCHAR(200),
    customer_phone      VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    callback_url        VARCHAR(500),
    paid_amount         NUMERIC(19,2),
    paid_at             TIMESTAMP WITH TIME ZONE,
    payment_reference   VARCHAR(100),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE,
    idempotency_key     VARCHAR(64)
);

-- va_number already has UNIQUE constraint which creates an implicit index
CREATE INDEX idx_va_partner_id ON virtual_accounts(partner_id);
CREATE INDEX idx_va_status ON virtual_accounts(status);
CREATE INDEX idx_va_expires_at ON virtual_accounts(expires_at);
CREATE INDEX idx_va_external_id ON virtual_accounts(partner_id, external_id);
CREATE INDEX idx_va_idempotency ON virtual_accounts(idempotency_key);

-- IMP-044: Add expires_at to transactions for payment expiry tracking
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX IF NOT EXISTS idx_txn_expires_at ON transactions(expires_at) WHERE expires_at IS NOT NULL;
