CREATE TABLE IF NOT EXISTS loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    principal_amount DECIMAL(19,2) NOT NULL,
    interest_rate DECIMAL(5,4),
    tenure_months INTEGER,
    monthly_installment DECIMAL(19,2),
    outstanding_balance DECIMAL(19,2),
    status VARCHAR(50) NOT NULL,
    purpose VARCHAR(255),
    disbursement_date DATE,
    maturity_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loans_user_id ON loans(user_id);
CREATE INDEX IF NOT EXISTS idx_loans_external_id ON loans(external_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);
