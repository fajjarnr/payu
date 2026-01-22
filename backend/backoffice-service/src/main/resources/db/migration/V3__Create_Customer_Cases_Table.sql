CREATE TABLE IF NOT EXISTS customer_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR NOT NULL,
    account_number VARCHAR(100),
    case_number VARCHAR(50),
    case_type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    subject VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    assigned_to VARCHAR(100),
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_user ON customer_cases(user_id);
CREATE INDEX IF NOT EXISTS idx_case_status ON customer_cases(status);
CREATE INDEX IF NOT EXISTS idx_case_type ON customer_cases(case_type);
CREATE INDEX IF NOT EXISTS idx_case_priority ON customer_cases(priority);
CREATE INDEX IF NOT EXISTS idx_case_number ON customer_cases(case_number);
