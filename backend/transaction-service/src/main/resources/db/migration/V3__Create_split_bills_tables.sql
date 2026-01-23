CREATE TABLE split_bills (
    id UUID PRIMARY KEY,
    reference_number VARCHAR(50) UNIQUE NOT NULL,
    creator_account_id UUID NOT NULL,
    total_amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    split_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    due_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_split_bills_creator ON split_bills(creator_account_id);
CREATE INDEX idx_split_bills_status ON split_bills(status);
CREATE INDEX idx_split_bills_due_date ON split_bills(due_date);

CREATE TABLE split_bill_participants (
    id UUID PRIMARY KEY,
    split_bill_id UUID NOT NULL,
    account_id UUID NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    amount_owed DECIMAL(19, 4) NOT NULL,
    amount_paid DECIMAL(19, 4) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_split_bill_participants_split_bill
        FOREIGN KEY (split_bill_id)
        REFERENCES split_bills(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_split_bill_participants_split_bill ON split_bill_participants(split_bill_id);
CREATE INDEX idx_split_bill_participants_account ON split_bill_participants(account_id);
CREATE INDEX idx_split_bill_participants_status ON split_bill_participants(status);

COMMENT ON TABLE split_bills IS 'Stores split bill information for multi-user payment sharing';
COMMENT ON TABLE split_bill_participants IS 'Stores participants for split bills';
