-- AUDIT-042 / GAP-25: Upgrade monetary columns from NUMERIC(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision in wallet-service.
--
-- Affected columns (transaction-service):
--   - virtual_accounts.amount          (V10__create_virtual_accounts_and_payment_expiry.sql:12)
--   - virtual_accounts.paid_amount     (V10__create_virtual_accounts_and_payment_expiry.sql:20) -- nullable
--   - disbursements.amount             (V13__create_disbursement_tables.sql:9)
--
-- Note: TransactionArchiveEntity already uses precision=19, scale=4 for its amount column (audit confirmed).
-- USAGE: NUMERIC(19,2) -> DECIMAL(19,4) is a widening cast (no data loss; trailing zeros appended).

BEGIN;

ALTER TABLE virtual_accounts
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4),
    ALTER COLUMN paid_amount TYPE DECIMAL(19,4) USING paid_amount::DECIMAL(19,4);

ALTER TABLE disbursements
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
