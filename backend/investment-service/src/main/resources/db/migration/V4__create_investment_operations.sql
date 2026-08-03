CREATE TABLE investment_operations (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    account_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(40) NOT NULL,
    product_code VARCHAR(255),
    tenure INTEGER,
    amount DECIMAL(19, 4) NOT NULL,
    price DECIMAL(19, 4),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    target_id UUID,
    debit_reference VARCHAR(100) NOT NULL,
    compensation_reference VARCHAR(100) NOT NULL,
    failure_reason VARCHAR(1000),
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_investment_operation_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_investment_operations_reconcile
    ON investment_operations(status, next_attempt_at);
