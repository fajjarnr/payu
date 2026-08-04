ALTER TABLE bill_payments
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128),
    ADD COLUMN IF NOT EXISTS wallet_reservation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS event_published BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_bill_payments_idempotency_key
    ON bill_payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bill_payments_reconciliation
    ON bill_payments (status, event_published);
