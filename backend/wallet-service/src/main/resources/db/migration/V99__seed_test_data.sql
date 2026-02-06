-- PayU Wallet Service - Seed Test Data
-- This migration creates test wallets and ledger entries for development/testing
-- Run this manually after all V* migrations are complete

-- Insert wallets for test users
INSERT INTO wallets (id, account_id, currency, balance, reserved_balance, status, created_at, updated_at) VALUES
    -- Customer 1 wallets
    ('850e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001', 'IDR', 10000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('850e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440002', 'IDR', 5000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('850e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440003', 'IDR', 3000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Customer 2 wallet
    ('850e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440004', 'IDR', 5000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Admin wallet
    ('850e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440005', 'IDR', 0.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (account_id) DO NOTHING;

-- Insert initial ledger entries for wallet creation (credit)
INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after, reference_type, reference_id, created_at) VALUES
    -- Customer 1 main wallet
    ('950e8400-e29b-41d4-a716-446655440001', 'a50e8400-e29b-41d4-a716-446655440001', '850e8400-e29b-41d4-a716-446655440001', 'CREDIT', 10000000.00, 10000000.00, 'WALLET_CREATION', 'INIT-001', CURRENT_TIMESTAMP),
    -- Customer 1 savings pocket
    ('950e8400-e29b-41d4-a716-446655440002', 'a50e8400-e29b-41d4-a716-446655440002', '850e8400-e29b-41d4-a716-446655440002', 'CREDIT', 5000000.00, 5000000.00, 'WALLET_CREATION', 'INIT-002', CURRENT_TIMESTAMP),
    -- Customer 1 emergency pocket
    ('950e8400-e29b-41d4-a716-446655440003', 'a50e8400-e29b-41d4-a716-446655440003', '850e8400-e29b-41d4-a716-446655440003', 'CREDIT', 3000000.00, 3000000.00, 'WALLET_CREATION', 'INIT-003', CURRENT_TIMESTAMP),
    -- Customer 2 main wallet
    ('950e8400-e29b-41d4-a716-446655440004', 'a50e8400-e29b-41d4-a716-446655440004', '850e8400-e29b-41d4-a716-446655440004', 'CREDIT', 5000000.00, 5000000.00, 'WALLET_CREATION', 'INIT-004', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Display seed data summary
DO $$
DECLARE
    wallet_count INT;
BEGIN
    SELECT COUNT(*) INTO wallet_count FROM wallets;

    RAISE NOTICE '========================================';
    RAISE NOTICE 'PayU Wallet Service - Seed Data Created';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Wallets: %', wallet_count;
    RAISE NOTICE 'Ledger Entries: 4';
    RAISE NOTICE '';
    RAISE NOTICE 'Test Wallet Balances:';
    RAISE NOTICE '  customer1 main:     Rp 10,000,000';
    RAISE NOTICE '  customer1 savings:  Rp 5,000,000';
    RAISE NOTICE '  customer1 emergency: Rp 3,000,000';
    RAISE NOTICE '  customer2 main:     Rp 5,000,000';
    RAISE NOTICE '========================================';
END $$;
