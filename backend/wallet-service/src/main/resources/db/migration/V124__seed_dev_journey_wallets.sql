-- RELAY-010: permanent dev-journey wallet seeds (replaces manual wallet-test-data.sql runs)
-- Mirrors V122 pattern: SYSTEM tenant bypass + ON CONFLICT DO NOTHING, no ledger
-- entries (V122 precedent — ledger rows are created by transfers, never hand-seeded).
-- Balances match db/seed/wallet-test-data.sql; money-safe scale DECIMAL(19,4).

SELECT set_config('app.tenant_id', 'SYSTEM', false);

INSERT INTO wallets (id, account_id, balance, reserved_balance, currency, status, version, tenant_id)
VALUES
    ('850e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001', 10000000.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('850e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440002', 5000000.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('850e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440003', 3000000.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('850e8400-e29b-41d4-a716-446655440004', '750e8400-e29b-41d4-a716-446655440004', 5000000.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('850e8400-e29b-41d4-a716-446655440005', '750e8400-e29b-41d4-a716-446655440005', 0.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default')
ON CONFLICT (account_id) DO NOTHING;
