-- Transaction archives table for data archival strategy
-- Simplified version without partitioning for compatibility
CREATE TABLE transaction_archives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number VARCHAR(50) UNIQUE NOT NULL,
    sender_account_id UUID NOT NULL,
    recipient_account_id UUID,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    archival_reason VARCHAR(100) NOT NULL,
    archived_batch_id BIGINT NOT NULL,

    CONSTRAINT positive_amount CHECK (amount > 0),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT valid_archival_reason CHECK (archival_reason IN ('RETENTION_EXPIRED', 'ACCOUNT_CLOSED', 'MANUAL_ARCHIVAL'))
);

CREATE INDEX idx_transaction_archives_sender ON transaction_archives(sender_account_id);
CREATE INDEX idx_transaction_archives_recipient ON transaction_archives(recipient_account_id);
CREATE INDEX idx_transaction_archives_reference ON transaction_archives(reference_number);
CREATE INDEX idx_transaction_archives_created ON transaction_archives(created_at DESC);
CREATE INDEX idx_transaction_archives_status ON transaction_archives(status);
CREATE INDEX idx_transaction_archives_archived_at ON transaction_archives(archived_at DESC);
CREATE INDEX idx_transaction_archives_batch ON transaction_archives(archived_batch_id);
-- Index for date-based queries (using generated column pattern)
CREATE INDEX idx_transaction_archives_created_date ON transaction_archives(created_at DESC);

-- Create a sequence for batch IDs
CREATE SEQUENCE IF NOT EXISTS archival_batch_id_seq START WITH 1;
