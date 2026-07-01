-- AUDIT-042 / GAP-25: Upgrade monetary columns from DECIMAL(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision in wallet-service.
--
-- Affected columns (fx-service):
--   - fx_conversions.from_amount       (V1__create_fx_tables.sql:22)
--   - fx_conversions.to_amount         (V1__create_fx_tables.sql:23)
--   - fx_conversions.fee               (V1__create_fx_tables.sql:25) -- nullable
--
-- Note: fx_conversions.exchange_rate stays DECIMAL(19,8) -- exchange rates require 8 fractional digits.
-- USAGE: DECIMAL(19,2) -> DECIMAL(19,4) is a widening cast (no data loss).

BEGIN;

ALTER TABLE fx_conversions
    ALTER COLUMN from_amount TYPE DECIMAL(19,4) USING from_amount::DECIMAL(19,4),
    ALTER COLUMN to_amount TYPE DECIMAL(19,4) USING to_amount::DECIMAL(19,4),
    ALTER COLUMN fee TYPE DECIMAL(19,4) USING fee::DECIMAL(19,4);

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
