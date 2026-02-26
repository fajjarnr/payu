-- V9: Create escrow_transactions table for marketplace payment holding
-- Supports E-10: Escrow & Marketplace Payments (GAP-007)

CREATE TABLE escrow_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_account_id VARCHAR(128) NOT NULL,
    seller_account_id VARCHAR(128) NOT NULL,
    partner_id       VARCHAR(128),
    amount           DECIMAL(19,4) NOT NULL,
    fee_amount       DECIMAL(19,4) DEFAULT 0,
    currency         VARCHAR(3) NOT NULL DEFAULT 'IDR',
    status           VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    external_reference_id VARCHAR(128),
    description      VARCHAR(512),
    reservation_id   VARCHAR(64),
    expires_at       TIMESTAMP,
    held_at          TIMESTAMP,
    released_at      TIMESTAMP,
    settled_at       TIMESTAMP,
    refunded_at      TIMESTAMP,
    refund_reason    VARCHAR(512),
    tenant_id        VARCHAR(64),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance indexes
CREATE INDEX idx_escrow_buyer ON escrow_transactions (buyer_account_id);
CREATE INDEX idx_escrow_seller ON escrow_transactions (seller_account_id);
CREATE INDEX idx_escrow_partner ON escrow_transactions (partner_id);
CREATE INDEX idx_escrow_status ON escrow_transactions (status);
CREATE INDEX idx_escrow_external_ref ON escrow_transactions (external_reference_id);
CREATE INDEX idx_escrow_expires ON escrow_transactions (expires_at);
CREATE INDEX idx_escrow_tenant ON escrow_transactions (tenant_id);

-- Composite index for expired escrow auto-refund scheduler
CREATE INDEX idx_escrow_held_expires ON escrow_transactions (status, expires_at);
