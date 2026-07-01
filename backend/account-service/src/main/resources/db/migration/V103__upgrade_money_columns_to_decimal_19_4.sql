-- AUDIT-042 / GAP-25: Upgrade monetary columns from DECIMAL/NUMERIC(19,2) to DECIMAL(19,4)
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). All money columns MUST use 4 fractional
-- digits to match the core ledger precision in wallet-service.
--
-- Affected columns (account-service):
--   - accounts.balance          (V1__Create_schema.sql:32)
--   - budgets.limit_amount      (V9__create_budgets_table.sql:7)
--   - budgets.current_spent     (V9__create_budgets_table.sql:9)
--
-- The USING clause is mandatory for type widening when the column contains data.
-- NUMERIC(19,2) -> NUMERIC(19,4) is a widening cast (no data loss; trailing zeros appended).
-- All changes wrapped in a single transaction for atomicity.

BEGIN;

ALTER TABLE accounts
    ALTER COLUMN balance TYPE DECIMAL(19,4) USING balance::DECIMAL(19,4);

ALTER TABLE budgets
    ALTER COLUMN limit_amount TYPE DECIMAL(19,4) USING limit_amount::DECIMAL(19,4),
    ALTER COLUMN current_spent TYPE DECIMAL(19,4) USING current_spent::DECIMAL(19,4);

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
-- Schema assertion tests in src/test/java/.../account/persistence/ will fail if precision is wrong.
