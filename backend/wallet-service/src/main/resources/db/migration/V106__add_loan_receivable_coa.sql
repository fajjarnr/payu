INSERT INTO chart_of_accounts (
    id, code, name, description, account_type, category, parent_id, level,
    active, normal_balance, currency
)
VALUES (
    'a0000001-0000-0000-0000-000000000015',
    '1500',
    'Loan Receivables',
    'Outstanding loan receivables',
    'ASSET',
    'LOAN_RECEIVABLE',
    'a0000001-0000-0000-0000-000000000001',
    2,
    true,
    'DEBIT',
    'IDR'
)
ON CONFLICT (code) DO NOTHING;
