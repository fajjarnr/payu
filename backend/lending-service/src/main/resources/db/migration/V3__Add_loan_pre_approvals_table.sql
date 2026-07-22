CREATE TABLE loan_pre_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    requested_amount DECIMAL(19,4) NOT NULL,
    max_approved_amount DECIMAL(19,4) NOT NULL,
    min_interest_rate DECIMAL(5,4) NOT NULL,
    max_tenure_months INTEGER,
    estimated_monthly_payment DECIMAL(19,4),
    status VARCHAR(50) NOT NULL,
    credit_score DECIMAL(5,2) NOT NULL,
    risk_category VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    valid_until DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_loan_pre_approval_user_id ON loan_pre_approvals(user_id);
CREATE INDEX idx_loan_pre_approval_valid_until ON loan_pre_approvals(valid_until);
CREATE INDEX idx_loan_pre_approval_status ON loan_pre_approvals(status);
