-- PAYU-TB-001: Seed TokoBapak escrow + seller wallets for SNAP-BI settlement
-- ACC_TOKOBAPAK_ESCROW holds marketplace escrow balance (100M IDR initial)
-- ACC_SELLER_* are merchant payout accounts (0 initial, credited via WalletSettlementAdapter settle)
-- Balances follow V1 precedent (wallets with balance, no ledger entries) — ledger created on transfer
-- RLS FORCE bypass: set app.tenant_id = SYSTEM

SELECT set_config('app.tenant_id', 'SYSTEM', false);

INSERT INTO wallets (id, account_id, balance, reserved_balance, currency, status, version, tenant_id)
VALUES
    ('a0000000-0000-0000-0000-000000000101', 'ACC_TOKOBAPAK_ESCROW', 100000000.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('a0000000-0000-0000-0000-000000000102', 'ACC_SELLER_001', 0.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('a0000000-0000-0000-0000-000000000103', 'ACC_SELLER_002', 0.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default'),
    ('a0000000-0000-0000-0000-000000000104', 'ACC_SELLER_003', 0.0000, 0.0000, 'IDR', 'ACTIVE', 0, 'default')
ON CONFLICT (account_id) DO NOTHING;

-- Ensure escrow retains minimum balance if already exists but depleted
UPDATE wallets SET balance = GREATEST(balance, 100000000.0000), status='ACTIVE'
WHERE account_id='ACC_TOKOBAPAK_ESCROW' AND balance < 100000000.0000;
