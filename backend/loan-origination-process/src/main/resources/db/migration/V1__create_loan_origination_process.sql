CREATE TABLE loan_origination_process (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    principal_amount DECIMAL(19,4) NOT NULL CHECK (principal_amount > 0),
    tenure_months INTEGER NOT NULL CHECK (tenure_months > 0),
    purpose VARCHAR(64),
    loan_type VARCHAR(64),
    credit_score DECIMAL(10,4) NOT NULL,
    status VARCHAR(32) NOT NULL,
    approved BOOLEAN,
    comment VARCHAR(1000),
    approved_by VARCHAR(255),
    disbursement_reference VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_loan_origination_process_user_id
    ON loan_origination_process (user_id, created_at DESC);
