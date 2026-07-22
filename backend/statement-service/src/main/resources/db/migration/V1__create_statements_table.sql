-- Statement Service Migration
-- V1__create_statements_table.sql

-- Statements table for e-statement metadata
CREATE TABLE IF NOT EXISTS statements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    statement_period DATE NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT,
    opening_balance DECIMAL(19,4) NOT NULL,
    closing_balance DECIMAL(19,4) NOT NULL,
    total_credits DECIMAL(19,4),
    total_debits DECIMAL(19,4),
    transaction_count INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    generated_at TIMESTAMP WITH TIME ZONE,
    access_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_statements_status CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED', 'ARCHIVED')),
    CONSTRAINT chk_statement_period UNIQUE (customer_id, statement_period)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_statements_customer_id ON statements(customer_id);
CREATE INDEX IF NOT EXISTS idx_statements_period ON statements(statement_period DESC);
CREATE INDEX IF NOT EXISTS idx_statements_status ON statements(status);
CREATE INDEX IF NOT EXISTS idx_statements_customer_period ON statements(customer_id, statement_period DESC);

-- Comments for documentation
COMMENT ON TABLE statements IS 'Monthly e-statement metadata for customer accounts';
COMMENT ON COLUMN statements.customer_id IS 'Customer identifier from account-service';
COMMENT ON COLUMN statements.account_number IS 'Customer account number';
COMMENT ON COLUMN statements.statement_period IS 'First day of the statement month';
COMMENT ON COLUMN statements.storage_path IS 'Path to PDF file in storage (S3 or local)';
COMMENT ON COLUMN statements.status IS 'GENERATING, COMPLETED, FAILED, or ARCHIVED';
COMMENT ON COLUMN statements.access_count IS 'Number of times the statement has been downloaded';
