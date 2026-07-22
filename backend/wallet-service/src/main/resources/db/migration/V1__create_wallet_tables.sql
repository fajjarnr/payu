-- V1__create_wallet_tables.sql
-- Flyway migration for wallet service

-- Wallets table
CREATE TABLE IF NOT EXISTS wallets (
    id UUID PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL UNIQUE,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reserved_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallet_account_id ON wallets(account_id);

-- Wallet transactions (ledger) table
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    reference_id VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_txn_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_txn_reference_id ON wallet_transactions(reference_id);

-- Insert test wallet for development
INSERT INTO wallets (id, account_id, balance, reserved_balance, currency, status, version)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'ACC-001', 1000000.0000, 0.0000, 'IDR', 'ACTIVE', 0),
    ('22222222-2222-2222-2222-222222222222', 'ACC-002', 500000.0000, 0.0000, 'IDR', 'ACTIVE', 0);
