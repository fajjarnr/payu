-- AUDIT-042 / GAP-25: Upgrade monetary columns from DECIMAL(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision.
--
-- Affected columns (wallet-service):
--   - pockets.balance                  (V3.1__create_pockets_table.sql:8)
--   - savings_goals.target_amount      (V11__create_savings_goals_table.sql:8)
--   - savings_goals.current_amount     (V11__create_savings_goals_table.sql:9)
--   - cards.daily_limit                (V2__create_cards_table.sql:9) -- nullable
--
-- Note: ledger_entries.amount and balance_after are ALREADY DECIMAL(19,4) (ledger design).
-- USAGE: DECIMAL(19,2) -> DECIMAL(19,4) is a widening cast (no data loss; trailing zeros appended).

BEGIN;

ALTER TABLE pockets
    ALTER COLUMN balance TYPE DECIMAL(19,4) USING balance::DECIMAL(19,4);

ALTER TABLE savings_goals
    ALTER COLUMN target_amount TYPE DECIMAL(19,4) USING target_amount::DECIMAL(19,4),
    ALTER COLUMN current_amount TYPE DECIMAL(19,4) USING current_amount::DECIMAL(19,4);

ALTER TABLE cards
    ALTER COLUMN daily_limit TYPE DECIMAL(19,4) USING daily_limit::DECIMAL(19,4);

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
-- wallet-service LedgerInvariantTest (per CHANGELOG iter-57) asserts BigDecimal precision preservation.
