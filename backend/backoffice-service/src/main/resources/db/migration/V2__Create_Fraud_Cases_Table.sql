CREATE TABLE IF NOT EXISTS fraud_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR NOT NULL,
    account_number VARCHAR(100),
    transaction_id UUID,
    transaction_type VARCHAR(50),
    amount DECIMAL(19,2),
    fraud_type VARCHAR(100),
    risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    description TEXT,
    evidence JSONB,
    notes TEXT,
    assigned_to VARCHAR(100),
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_user ON fraud_cases(user_id);
CREATE INDEX IF NOT EXISTS idx_fraud_status ON fraud_cases(status);
CREATE INDEX IF NOT EXISTS idx_fraud_risk ON fraud_cases(risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_transaction ON fraud_cases(transaction_id);
