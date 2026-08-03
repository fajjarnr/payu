CREATE TABLE IF NOT EXISTS loan_repayment_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repayment_schedule_id UUID NOT NULL REFERENCES repayment_schedules(id),
    loan_id UUID NOT NULL REFERENCES loans(id),
    user_id UUID NOT NULL,
    amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PROCESSING', 'COMPLETED', 'RECONCILIATION_REQUIRED')),
    wallet_transaction_id VARCHAR(128),
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tenant_id VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_loan_repayment_payments_schedule
    ON loan_repayment_payments(repayment_schedule_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_loan_repayment_payments_status
    ON loan_repayment_payments(status, updated_at);

CREATE UNIQUE INDEX IF NOT EXISTS ux_repayment_schedule_loan_installment
    ON repayment_schedules(loan_id, installment_number);
