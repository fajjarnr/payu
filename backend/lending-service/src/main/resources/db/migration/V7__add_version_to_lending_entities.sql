-- V7__add_version_to_lending_entities.sql
-- ITER-52C: Add @Version column to 7 lending entities for optimistic locking.

ALTER TABLE paylater_accounts      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE credit_scores          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE loans                  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE repayment_schedules    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE installment_checkouts  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE loan_pre_approvals     ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE paylater_transactions  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE paylater_accounts      SET version = 0 WHERE version IS NULL;
UPDATE credit_scores          SET version = 0 WHERE version IS NULL;
UPDATE loans                  SET version = 0 WHERE version IS NULL;
UPDATE repayment_schedules    SET version = 0 WHERE version IS NULL;
UPDATE installment_checkouts  SET version = 0 WHERE version IS NULL;
UPDATE loan_pre_approvals     SET version = 0 WHERE version IS NULL;
UPDATE paylater_transactions  SET version = 0 WHERE version IS NULL;
