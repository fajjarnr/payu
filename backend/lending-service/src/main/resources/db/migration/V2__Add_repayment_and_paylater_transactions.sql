CREATE TABLE repayment_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL,
    installment_number INTEGER NOT NULL,
    installment_amount DECIMAL(19,2) NOT NULL,
    principal_amount DECIMAL(19,2) NOT NULL,
    interest_amount DECIMAL(19,2) NOT NULL,
    outstanding_principal DECIMAL(19,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_date DATE,
    paid_amount DECIMAL(19,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_repayment_schedule_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
);

CREATE INDEX idx_repayment_schedule_loan_id ON repayment_schedules(loan_id);
CREATE INDEX idx_repayment_schedule_due_date ON repayment_schedules(due_date);
CREATE INDEX idx_repayment_schedule_status ON repayment_schedules(status);

CREATE TABLE paylater_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(255) UNIQUE,
    paylater_account_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    merchant_name VARCHAR(255),
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_paylater_transaction_account FOREIGN KEY (paylater_account_id) REFERENCES paylater_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_paylater_transactions_account_id ON paylater_transactions(paylater_account_id);
CREATE INDEX idx_paylater_transactions_type ON paylater_transactions(type);
CREATE INDEX idx_paylater_transactions_status ON paylater_transactions(status);
CREATE INDEX idx_paylater_transactions_date ON paylater_transactions(transaction_date);
