CREATE TABLE investment_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL UNIQUE,
    total_balance DECIMAL(19, 4) DEFAULT 0,
    available_balance DECIMAL(19, 4) DEFAULT 0,
    locked_balance DECIMAL(19, 4) DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE deposits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    tenure INTEGER NOT NULL,
    interest_rate DECIMAL(5, 4),
    maturity_amount DECIMAL(19, 4),
    start_date TIMESTAMP,
    maturity_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    currency VARCHAR(3) DEFAULT 'IDR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mutual_funds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    nav_per_unit DECIMAL(19, 4),
    minimum_investment DECIMAL(19, 4),
    management_fee DECIMAL(5, 4),
    redemption_fee DECIMAL(5, 4),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gold_holdings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(19, 4),
    average_buy_price DECIMAL(19, 4),
    current_price DECIMAL(19, 4),
    current_value DECIMAL(19, 4),
    unrealized_profit_loss DECIMAL(19, 4),
    last_price_update TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE investment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    investment_type VARCHAR(50) NOT NULL,
    investment_id VARCHAR(255),
    amount DECIMAL(19, 4),
    price DECIMAL(19, 4),
    units DECIMAL(19, 4),
    fee DECIMAL(19, 4),
    currency VARCHAR(3) DEFAULT 'IDR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reference_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deposits_account_id ON deposits(account_id);
CREATE INDEX idx_mutual_funds_code ON mutual_funds(code);
CREATE INDEX idx_gold_holdings_user_id ON gold_holdings(user_id);
CREATE INDEX idx_investment_transactions_account_id ON investment_transactions(account_id);
CREATE INDEX idx_investment_transactions_reference_number ON investment_transactions(reference_number);

INSERT INTO mutual_funds (code, name, type, nav_per_unit, minimum_investment, management_fee, redemption_fee) VALUES
('MMF001', 'PayU Money Market Fund', 'MONEY_MARKET', 1500.0000, 10000.0000, 0.0050, 0.0020),
('RD001', 'PayU Fixed Income Fund', 'FIXED_INCOME', 2000.0000, 50000.0000, 0.0100, 0.0050),
('STK001', 'PayU Equity Index Fund', 'EQUITY', 3500.0000, 100000.0000, 0.0150, 0.0100);
