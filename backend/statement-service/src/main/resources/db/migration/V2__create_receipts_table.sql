-- Migration: Create receipts table for transaction proof feature
-- Epic E-19: Transaction Proof & Receipts (IMP-055)
-- Created: 2026-03-01

CREATE TABLE IF NOT EXISTS receipts (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    customer_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',

    -- Sender information
    sender_name VARCHAR(200) NOT NULL,
    sender_account_number VARCHAR(50) NOT NULL,
    sender_bank_name VARCHAR(100) NOT NULL,
    -- Recipient information
    recipient_name VARCHAR(200) NOT NULL,
    recipient_account_number VARCHAR(50) NOT NULL,
    recipient_bank_name VARCHAR(100) NOT NULL,

    transaction_timestamp TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    reference_number VARCHAR(100) NOT NULL,

    expiry_date TIMESTAMP NOT NULL,
    access_count INTEGER DEFAULT 0,
    last_accessed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_receipts_transaction_id ON receipts(transaction_id);
CREATE INDEX IF NOT EXISTS idx_receipts_customer_id ON receipts(customer_id);
CREATE INDEX IF NOT EXISTS idx_receipts_status ON receipts(status);
CREATE INDEX IF NOT EXISTS idx_receipts_expiry_date ON receipts(expiry_date);

-- Comments for documentation
COMMENT ON TABLE receipts IS 'Transaction receipts (bukti transfer) for PayU platform';
COMMENT ON COLUMN receipts.id IS 'Unique receipt ID (UUID)';
COMMENT ON COLUMN receipts.transaction_id IS 'Reference to the original transaction';
COMMENT ON COLUMN receipts.status IS 'GENERATED or EXPIRED';
COMMENT ON COLUMN receipts.expiry_date IS 'Receipt valid for 90 days from creation';
