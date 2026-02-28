-- Disbursement (Payout) tables for Epic E-16
-- IMP-047, IMP-048, IMP-049

-- Disbursements table for individual payouts
CREATE TABLE IF NOT EXISTS disbursements (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    source_account_id UUID NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    bank_code VARCHAR(10) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    bank_reference VARCHAR(50),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for disbursements
CREATE INDEX IF NOT EXISTS idx_disbursement_source_account ON disbursements(source_account_id);
CREATE INDEX IF NOT EXISTS idx_disbursement_status ON disbursements(status);
CREATE INDEX IF NOT EXISTS idx_disbursement_created_at ON disbursements(created_at);

-- Batch disbursements table for bulk payouts
CREATE TABLE IF NOT EXISTS batch_disbursements (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    source_account_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for batch disbursements
CREATE INDEX IF NOT EXISTS idx_batch_source_account ON batch_disbursements(source_account_id);
CREATE INDEX IF NOT EXISTS idx_batch_status ON batch_disbursements(status);
CREATE INDEX IF NOT EXISTS idx_batch_created_at ON batch_disbursements(created_at);

-- Add batch_id column to disbursements for batch relationship
ALTER TABLE disbursements
    ADD COLUMN IF NOT EXISTS batch_id UUID REFERENCES batch_disbursements(id);

CREATE INDEX IF NOT EXISTS idx_disbursement_batch ON disbursements(batch_id);

-- Comments
COMMENT ON TABLE disbursements IS 'Individual disbursement/payout transactions';
COMMENT ON COLUMN disbursements.idempotency_key IS 'Unique key for duplicate protection';
COMMENT ON COLUMN disbursements.status IS 'PENDING, PROCESSING, COMPLETED, FAILED';

COMMENT ON TABLE batch_disbursements IS 'Batch disbursement groups for bulk payouts';
COMMENT ON COLUMN batch_disbursements.status IS 'PENDING, PROCESSING, COMPLETED, PARTIAL, FAILED';
