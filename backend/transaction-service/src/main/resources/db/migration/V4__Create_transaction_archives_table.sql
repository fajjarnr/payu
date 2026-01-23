-- Transaction archives table for data archival strategy
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

-- Partition transaction_archives by year for better performance
CREATE TABLE transaction_archives_y2024 PARTITION OF transaction_archives
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE transaction_archives_y2025 PARTITION OF transaction_archives
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE transaction_archives_y2026 PARTITION OF transaction_archives
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- Create a sequence for batch IDs
CREATE SEQUENCE archival_batch_id_seq START WITH 1;

-- Function to create new yearly partitions automatically
CREATE OR REPLACE FUNCTION create_transaction_archive_partition(year INT)
RETURNS VOID AS $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    start_date := TO_DATE(year::TEXT, 'YYYY');
    end_date := start_date + INTERVAL '1 year';
    partition_name := 'transaction_archives_y' || year::TEXT;

    EXECUTE FORMAT(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF transaction_archives
         FOR VALUES FROM (%L) TO (%L)',
        partition_name, start_date, end_date
    );
END;
$$ LANGUAGE plpgsql;
