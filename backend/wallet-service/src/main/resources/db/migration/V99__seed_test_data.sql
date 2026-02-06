-- PayU Wallet Service - Seed Test Data
-- This migration creates test wallets and ledger entries for development/testing
-- Run this manually after all V* migrations are complete

-- Insert wallets for test users
INSERT INTO wallets (id, account_id, user_id, currency, balance, available_balance, reserved_balance, status, created_at, updated_at) VALUES
    -- Customer 1 wallets
    ('850e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'IDR', 10000000.00, 10000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('850e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', 'IDR', 5000000.00, 5000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('850e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'IDR', 3000000.00, 3000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Customer 2 wallet
    ('850e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440002', 'IDR', 5000000.00, 5000000.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Admin wallet
    ('850e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440003', 'IDR', 0.00, 0.00, 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (account_id) DO NOTHING;

-- Insert initial ledger entries for wallet creation (credit)
INSERT INTO ledger_entries (id, wallet_id, entry_type, reference_id, reference_type, amount, balance_after, description, created_at) VALUES
    -- Customer 1 main wallet
    ('950e8400-e29b-41d4-a716-446655440001', '850e8400-e29b-41d4-a716-446655440001', 'CREDIT', 'INIT-001', 'WALLET_CREATION', 10000000.00, 10000000.00, 'Initial wallet creation', CURRENT_TIMESTAMP),
    -- Customer 1 savings pocket
    ('950e8400-e29b-41d4-a716-446655440002', '850e8400-e29b-41d4-a716-446655440002', 'CREDIT', 'INIT-002', 'WALLET_CREATION', 5000000.00, 5000000.00, 'Initial savings pocket', CURRENT_TIMESTAMP),
    -- Customer 1 emergency pocket
    ('950e8400-e29b-41d4-a716-446655440003', '850e8400-e29b-41d4-a716-446655440003', 'CREDIT', 'INIT-003', 'WALLET_CREATION', 3000000.00, 3000000.00, 'Initial emergency pocket', CURRENT_TIMESTAMP),
    -- Customer 2 main wallet
    ('950e8400-e29b-41d4-a716-446655440004', '850e8400-e29b-41d4-a716-446655440004', 'CREDIT', 'INIT-004', 'WALLET_CREATION', 5000000.00, 5000000.00, 'Initial wallet creation', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

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
