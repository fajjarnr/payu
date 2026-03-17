-- BUG-BE-182 FIX: Persistent storage for SNAP BI payments and refunds
-- Replaces in-memory ConcurrentHashMap that lost data on service restart

CREATE TABLE IF NOT EXISTS snap_bi_payments (
    id              BIGSERIAL PRIMARY KEY,
    payu_reference_no   VARCHAR(64)  NOT NULL UNIQUE,
    partner_id          VARCHAR(64)  NOT NULL,
    partner_reference_no VARCHAR(64),
    amount              NUMERIC(19,2) NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    beneficiary_account_no VARCHAR(64),
    beneficiary_bank_code  VARCHAR(20),
    source_account_no   VARCHAR(64),
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_snap_payment_partner_ref
    ON snap_bi_payments (partner_id, partner_reference_no);

CREATE TABLE IF NOT EXISTS snap_bi_refunds (
    id                  BIGSERIAL PRIMARY KEY,
    payu_refund_no      VARCHAR(64)  NOT NULL UNIQUE,
    partner_id          VARCHAR(64)  NOT NULL,
    payu_reference_no   VARCHAR(64)  NOT NULL,
    partner_reference_no VARCHAR(64),
    partner_refund_no   VARCHAR(64),
    amount              NUMERIC(19,2) NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_snap_refund_payment_ref
    ON snap_bi_refunds (payu_reference_no);
