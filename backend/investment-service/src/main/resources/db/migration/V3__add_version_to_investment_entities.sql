-- V3__add_version_to_investment_entities.sql
-- ITER-52D: Add @Version column to 5 investment entities for optimistic locking.

ALTER TABLE deposits                ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gold_holdings           ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE investment_accounts     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE investment_transactions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mutual_funds            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE deposits                SET version = 0 WHERE version IS NULL;
UPDATE gold_holdings           SET version = 0 WHERE version IS NULL;
UPDATE investment_accounts     SET version = 0 WHERE version IS NULL;
UPDATE investment_transactions SET version = 0 WHERE version IS NULL;
UPDATE mutual_funds            SET version = 0 WHERE version IS NULL;
