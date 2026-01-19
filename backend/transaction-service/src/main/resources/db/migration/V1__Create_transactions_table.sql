-- Transactions table
CREATE TABLE transactions (
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT positive_amount CHECK (amount > 0),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_transactions_sender ON transactions(sender_account_id);
CREATE INDEX idx_transactions_recipient ON transactions(recipient_account_id);
CREATE INDEX idx_transactions_reference ON transactions(reference_number);
CREATE INDEX idx_transactions_created ON transactions(created_at DESC);
CREATE INDEX idx_transactions_status ON transactions(status);
