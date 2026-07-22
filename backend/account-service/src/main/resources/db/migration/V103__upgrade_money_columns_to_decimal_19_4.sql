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

DROP MATERIALIZED VIEW IF EXISTS mv_account_balance_summary CASCADE;

ALTER TABLE accounts
    ALTER COLUMN balance TYPE DECIMAL(19,4) USING balance::DECIMAL(19,4);

ALTER TABLE budgets
    ALTER COLUMN limit_amount TYPE DECIMAL(19,4) USING limit_amount::DECIMAL(19,4),
    ALTER COLUMN current_spent TYPE DECIMAL(19,4) USING current_spent::DECIMAL(19,4);

CREATE MATERIALIZED VIEW mv_account_balance_summary AS
SELECT
    a.id as account_id,
    date_trunc('day', a.updated_at) as date,
    COALESCE(a.balance, 0) as total_balance,
    0 as transaction_count,
    COALESCE(a.balance, 0) as avg_balance,
    COALESCE(a.balance, 0) as min_balance,
    COALESCE(a.balance, 0) as max_balance
FROM accounts a
WHERE a.updated_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY a.id, date_trunc('day', a.updated_at);

CREATE INDEX IF NOT EXISTS idx_mv_balance_summary_account_date ON mv_account_balance_summary(account_id, date);

COMMENT ON MATERIALIZED VIEW mv_account_balance_summary IS 'Daily balance summaries per account';

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
-- Schema assertion tests in src/test/java/.../account/persistence/ will fail if precision is wrong.
