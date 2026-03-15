-- PayU Account Service - Seed Test Data
-- This migration creates test users and accounts for development/testing
-- Run this manually after all V* migrations are complete

-- Clean up existing test data to avoid conflicts
DELETE FROM profiles WHERE id IN (SELECT id FROM users WHERE username IN ('customer1', 'customer2', 'admin'));
DELETE FROM accounts WHERE user_id IN (SELECT id FROM users WHERE username IN ('customer1', 'customer2', 'admin'));
DELETE FROM users WHERE username IN ('customer1', 'customer2', 'admin');

-- Insert test users
INSERT INTO users (id, external_id, username, email, phone_number, status, kyc_status, created_at, updated_at) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'EXT-CUST-001', 'customer1', 'customer1@payu.fajjjar.my.id', '+6281234567890', 'ACTIVE', 'VERIFIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440002', 'EXT-CUST-002', 'customer2', 'customer2@payu.fajjjar.my.id', '+6281234567891', 'ACTIVE', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440003', 'EXT-ADMIN-001', 'admin', 'admin@payu.fajjjar.my.id', '+628111111111', 'ACTIVE', 'VERIFIED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert profiles for test users
INSERT INTO profiles (id, full_name, nik, birth_date, address) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'Customer One', '3201234567890001', '1990-01-15', 'Jl. Sudirman No. 123, Jakarta'),
    ('550e8400-e29b-41d4-a716-446655440002', 'Customer Two', '3201234567890002', '1992-05-20', 'Jl. Braga No. 456, Bandung'),
    ('550e8400-e29b-41d4-a716-446655440003', 'System Administrator', '3201234567890003', '1985-03-25', 'Jakarta, Indonesia');

-- Insert accounts for test users (Main account + Pocket accounts)
INSERT INTO accounts (id, user_id, account_number, type, status, currency, balance, created_at, updated_at) VALUES
    -- Customer 1 accounts
    ('750e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', '1001001001', 'MAIN', 'ACTIVE', 'IDR', 10000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('750e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440001', '1001001002', 'POCKET_SAVINGS', 'ACTIVE', 'IDR', 5000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('750e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', '1001001003', 'POCKET_EMERGENCY', 'ACTIVE', 'IDR', 3000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Customer 2 accounts
    ('750e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440002', '1001002001', 'MAIN', 'ACTIVE', 'IDR', 5000000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Admin accounts
    ('750e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440003', '1009999001', 'MAIN', 'ACTIVE', 'IDR', 0.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Display seed data summary
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'PayU Account Service - Seed Data Created';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Users: 3 (customer1, customer2, admin)';
    RAISE NOTICE 'Profiles: 3';
    RAISE NOTICE 'Accounts: 6';
    RAISE NOTICE '';
    RAISE NOTICE 'Test Credentials:';
    RAISE NOTICE '  Username: customer1 | Password: P@ssw0rd123';
    RAISE NOTICE '  Username: customer2 | Password: P@ssw0rd123';
    RAISE NOTICE '  Username: admin     | Password: P@ssw0rd123';
    RAISE NOTICE '========================================';
END $$;
