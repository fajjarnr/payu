-- V1__create_bill_payments_table.sql
-- Description: Initial schema for billing service - bill payments table
-- Rollback: DROP TABLE IF EXISTS bill_payments;

CREATE TABLE IF NOT EXISTS bill_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    reference_number VARCHAR(255) NOT NULL UNIQUE,
    biller_type VARCHAR(50) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    admin_fee DECIMAL(19, 4) DEFAULT 0,
    total_amount DECIMAL(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason TEXT,
    biller_transaction_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    CONSTRAINT chk_positive_amount CHECK (amount > 0),
    CONSTRAINT chk_valid_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_payment_account ON bill_payments (account_id);
CREATE INDEX IF NOT EXISTS idx_payment_reference ON bill_payments (reference_number);
CREATE INDEX IF NOT EXISTS idx_payment_status ON bill_payments (status);
CREATE INDEX IF NOT EXISTS idx_payment_created_at ON bill_payments (created_at DESC);

-- Composite index for user's recent payments
CREATE INDEX IF NOT EXISTS idx_payment_account_date ON bill_payments (account_id, created_at DESC);

COMMENT ON TABLE bill_payments IS 'Bill payment transactions for PLN, PDAM, Pulsa, etc.';
COMMENT ON COLUMN bill_payments.reference_number IS 'Unique reference number for tracking payment';
COMMENT ON COLUMN bill_payments.customer_id IS 'Customer identifier at biller (meter number, phone, etc.)';
