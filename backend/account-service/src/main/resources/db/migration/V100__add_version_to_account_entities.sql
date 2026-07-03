-- V100__add_version_to_account_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to account-service entities.

ALTER TABLE sensitive_user_data                 ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE budgets                             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE sensitive_user_data                 SET version = 0 WHERE version IS NULL;
UPDATE budgets                             SET version = 0 WHERE version IS NULL;
