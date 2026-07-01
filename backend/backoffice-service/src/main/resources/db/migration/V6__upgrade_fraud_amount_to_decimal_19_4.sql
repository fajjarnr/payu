-- AUDIT-042 / GAP-25: Upgrade monetary column from DECIMAL(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision.
--
-- Affected column (backoffice-service):
--   - fraud_cases.amount        (V2__Create_Fraud_Cases_Table.sql:7)
--
-- USAGE: nullable column (no NOT NULL constraint). Existing NULL values preserved.
-- DECIMAL(19,2) -> DECIMAL(19,4) is a widening cast (no data loss; trailing zeros appended).

BEGIN;

ALTER TABLE fraud_cases
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
