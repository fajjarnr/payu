CREATE TABLE IF NOT EXISTS chargebacks (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    scheme_case_id VARCHAR(50),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    under_review_at TIMESTAMP WITH TIME ZONE,
    accepted_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    reversed_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_chargebacks_status ON chargebacks(status);
CREATE INDEX IF NOT EXISTS idx_chargebacks_customer_id ON chargebacks(customer_id);
CREATE INDEX IF NOT EXISTS idx_chargebacks_transaction_id ON chargebacks(transaction_id);
