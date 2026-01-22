CREATE TABLE IF NOT EXISTS kyc_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR NOT NULL,
    account_number VARCHAR NOT NULL,
    document_type VARCHAR(100),
    document_number VARCHAR(200),
    document_url TEXT,
    full_name VARCHAR(100),
    address TEXT,
    phone_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kyc_user ON kyc_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_kyc_status ON kyc_reviews(status);
CREATE INDEX IF NOT EXISTS idx_kyc_reviewed_by ON kyc_reviews(reviewed_by);
