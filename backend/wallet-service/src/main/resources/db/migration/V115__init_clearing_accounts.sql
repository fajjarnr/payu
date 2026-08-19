-- ADR-0029 ISO20022 Interbank Clearing & Suspense Ledgering: system clearing accounts
-- ponytail: parent exists as a000...015 (code 1500) from prior seed, children use SELECT for FK
INSERT INTO chart_of_accounts (id, code, name, description, account_type, category, parent_id, level, active, normal_balance, currency)
VALUES ('c0000001-0000-0000-0000-000000000101', '1500', 'Clearing Suspense', 'Interbank clearing suspense', 'ASSET', 'CLEARING_SUSPENSE', 'a0000001-0000-0000-0000-000000000001', 2, true, 'DEBIT', 'IDR')
ON CONFLICT (code) DO NOTHING;
INSERT INTO chart_of_accounts (id, code, name, description, account_type, category, parent_id, level, active, normal_balance, currency)
VALUES
    ('c0000001-0000-0000-0000-000000000102', '1510', 'SYSTEM_BI_FAST_CLEARING', 'BI-FAST interbank clearing', 'ASSET', 'CLEARING_BI_FAST', (SELECT id FROM chart_of_accounts WHERE code = '1500'), 3, true, 'DEBIT', 'IDR'),
    ('c0000001-0000-0000-0000-000000000103', '1520', 'SYSTEM_SKN_CLEARING', 'SKN interbank clearing', 'ASSET', 'CLEARING_SKN', (SELECT id FROM chart_of_accounts WHERE code = '1500'), 3, true, 'DEBIT', 'IDR'),
    ('c0000001-0000-0000-0000-000000000104', '1530', 'SYSTEM_RTGS_CLEARING', 'RTGS clearing', 'ASSET', 'CLEARING_RTGS', (SELECT id FROM chart_of_accounts WHERE code = '1500'), 3, true, 'DEBIT', 'IDR'),
    ('c0000001-0000-0000-0000-000000000105', '1540', 'SYSTEM_QRIS_CLEARING', 'QRIS clearing', 'ASSET', 'CLEARING_QRIS', (SELECT id FROM chart_of_accounts WHERE code = '1500'), 3, true, 'DEBIT', 'IDR'),
    ('c0000001-0000-0000-0000-000000000106', '1550', 'NOSTRO_BI_FAST', 'Nostro BI-FAST account', 'ASSET', 'NOSTRO_BI_FAST', (SELECT id FROM chart_of_accounts WHERE code = '1500'), 3, true, 'DEBIT', 'IDR')
ON CONFLICT (code) DO NOTHING;
