-- AUDIT-042 / GAP-25: Upgrade monetary columns from NUMERIC(19,2) to DECIMAL(19,4).
-- AGENTS.md Rule #1: BigDecimal HALF_EVEN, DB DECIMAL(19,4). Money columns MUST use 4 fractional
-- digits to match the core ledger precision in wallet-service.
--
-- Affected columns (billing-service):
--   - subscription_plans.price          (V3__create_subscription_tables.sql:10)
--   - subscriptions.current_price       (V3__create_subscription_tables.sql:28)
--   - subscription_charges.amount       (V3__create_subscription_tables.sql:53)
--
-- USAGE: NUMERIC and DECIMAL are aliases in PostgreSQL. Using DECIMAL(19,4) for consistency.

BEGIN;

ALTER TABLE subscription_plans
    ALTER COLUMN price TYPE DECIMAL(19,4) USING price::DECIMAL(19,4);

ALTER TABLE subscriptions
    ALTER COLUMN current_price TYPE DECIMAL(19,4) USING current_price::DECIMAL(19,4);

ALTER TABLE subscription_charges
    ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);

COMMIT;

-- Verification: existing test suite (Testcontainers + Flyway) loads this migration at startup.
