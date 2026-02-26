-- V5: Installment Checkout table (GAP-012)
-- Bridges PayLater credit accounts with gateway-facing installment payments

CREATE TABLE IF NOT EXISTS installment_checkouts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID           NOT NULL,
    paylater_id         UUID           NOT NULL REFERENCES paylater_accounts(id),
    loan_id             UUID           REFERENCES loans(id),
    partner_id          VARCHAR(50)    NOT NULL,
    external_order_id   VARCHAR(100)   UNIQUE,
    purchase_amount     NUMERIC(19, 2) NOT NULL,
    currency            VARCHAR(3)     NOT NULL DEFAULT 'IDR',
    tenor               INT            NOT NULL,
    monthly_payment     NUMERIC(19, 2) NOT NULL,
    interest_rate       NUMERIC(5, 4),
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    failure_reason      TEXT,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_installment_checkouts_user_id ON installment_checkouts(user_id);
CREATE INDEX IF NOT EXISTS idx_installment_checkouts_paylater_id ON installment_checkouts(paylater_id);
CREATE INDEX IF NOT EXISTS idx_installment_checkouts_loan_id ON installment_checkouts(loan_id);
CREATE INDEX IF NOT EXISTS idx_installment_checkouts_external_order ON installment_checkouts(external_order_id);
CREATE INDEX IF NOT EXISTS idx_installment_checkouts_status ON installment_checkouts(status);
