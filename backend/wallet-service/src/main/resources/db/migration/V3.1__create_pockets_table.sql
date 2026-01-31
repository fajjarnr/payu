-- Add multi-currency pockets support
CREATE TABLE IF NOT EXISTS pockets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_pockets_account_id ON pockets(account_id);
CREATE INDEX IF NOT EXISTS idx_pockets_account_currency ON pockets(account_id, currency);
CREATE INDEX IF NOT EXISTS idx_pockets_status ON pockets(status);
