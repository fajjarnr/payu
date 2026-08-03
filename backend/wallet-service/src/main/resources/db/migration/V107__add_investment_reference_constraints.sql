CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_investment_reference
    ON wallet_transactions(reference_id)
    WHERE reference_id LIKE 'INVESTMENT%';

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_investment_reservation_reference
    ON ledger_entries(reference_type, reference_id)
    WHERE reference_type = 'RESERVATION'
      AND reference_id LIKE 'INVESTMENT%';
