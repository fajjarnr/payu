CREATE TABLE loans (
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

CREATE TABLE paylater_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) UNIQUE,
    user_id UUID NOT NULL UNIQUE,
    credit_limit DECIMAL(19,2) NOT NULL,
    used_credit DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    available_credit DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    billing_cycle_day INTEGER,
    interest_rate DECIMAL(5,4),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE credit_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    score DECIMAL(5,2) NOT NULL,
    risk_category VARCHAR(50) NOT NULL,
    last_calculated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_loans_user_id ON loans(user_id);
CREATE INDEX idx_loans_external_id ON loans(external_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_paylater_user_id ON paylater_accounts(user_id);
CREATE INDEX idx_credit_scores_user_id ON credit_scores(user_id);
