CREATE TABLE IF NOT EXISTS refund_reversal_executions (
    refund_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    sender_account_id VARCHAR(50),
    recipient_account_id VARCHAR(50),
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refund_reversal_status
    ON refund_reversal_executions(status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_refund_reversal_ledger
    ON ledger_entries(reference_type, reference_id, entry_type)
    WHERE reference_type = 'REFUND_REVERSAL';
