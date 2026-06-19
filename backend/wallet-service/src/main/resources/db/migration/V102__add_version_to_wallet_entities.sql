-- V102__add_version_to_wallet_entities.sql
-- ITER-52A: Add @Version column (optimistic locking) to 17 wallet entities.
-- Prevents lost updates on concurrent writes (e.g., balance updates from
-- concurrent transactions, split payment rule modifications, etc).

ALTER TABLE cards                       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chart_of_accounts           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_discrepancies    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE escrow_transactions         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE journal_entries             ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ledger_entries              ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pockets                     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE revenue_split_stakeholders  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE revenue_splits              ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE savings_goals               ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_batches          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_entries          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE split_payment_executions    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE split_payment_legs          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE split_payment_rules         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE split_recipients            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE wallet_transactions         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Backfill safety: ensure no NULL versions (NOT NULL DEFAULT 0 already handles new rows)
UPDATE cards                       SET version = 0 WHERE version IS NULL;
UPDATE chart_of_accounts           SET version = 0 WHERE version IS NULL;
UPDATE settlement_discrepancies    SET version = 0 WHERE version IS NULL;
UPDATE escrow_transactions         SET version = 0 WHERE version IS NULL;
UPDATE journal_entries             SET version = 0 WHERE version IS NULL;
UPDATE ledger_entries              SET version = 0 WHERE version IS NULL;
UPDATE pockets                     SET version = 0 WHERE version IS NULL;
UPDATE revenue_split_stakeholders  SET version = 0 WHERE version IS NULL;
UPDATE revenue_splits              SET version = 0 WHERE version IS NULL;
UPDATE savings_goals               SET version = 0 WHERE version IS NULL;
UPDATE settlement_batches          SET version = 0 WHERE version IS NULL;
UPDATE settlement_entries          SET version = 0 WHERE version IS NULL;
UPDATE split_payment_executions    SET version = 0 WHERE version IS NULL;
UPDATE split_payment_legs          SET version = 0 WHERE version IS NULL;
UPDATE split_payment_rules         SET version = 0 WHERE version IS NULL;
UPDATE split_recipients            SET version = 0 WHERE version IS NULL;
UPDATE wallet_transactions         SET version = 0 WHERE version IS NULL;
