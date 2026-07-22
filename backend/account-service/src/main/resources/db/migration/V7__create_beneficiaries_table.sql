-- Create beneficiaries table (IMP-035)
CREATE TABLE beneficiaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    bank_code VARCHAR(10) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_beneficiary_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT valid_beneficiary_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    CONSTRAINT unique_user_account UNIQUE (user_id, bank_code, account_number)
);

-- Create indexes
CREATE INDEX idx_beneficiaries_user_id ON beneficiaries(user_id);
CREATE INDEX idx_beneficiaries_status ON beneficiaries(status);
CREATE INDEX idx_beneficiaries_user_status ON beneficiaries(user_id, status);

-- Add comments
COMMENT ON TABLE beneficiaries IS 'User saved beneficiaries for quick transfers';
COMMENT ON COLUMN beneficiaries.bank_code IS 'Bank code (e.g., BCA, MANDIRI, BNI)';
COMMENT ON COLUMN beneficiaries.account_number IS 'Beneficiary account number';
COMMENT ON COLUMN beneficiaries.account_name IS 'Beneficiary account name (from BI-FAST inquiry)';
COMMENT ON COLUMN beneficiaries.nickname IS 'User-defined nickname for this beneficiary';
COMMENT ON COLUMN beneficiaries.verified_at IS 'Timestamp when account was verified via BI-FAST inquiry';
