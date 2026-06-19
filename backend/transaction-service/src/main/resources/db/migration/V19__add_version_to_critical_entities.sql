-- V19__add_version_to_critical_entities.sql
-- ITER-51D: Add @Version column (optimistic locking) to the 3 most critical
-- financial entities. Without @Version, concurrent updates (e.g., async disbursement
-- processing + admin status change) can silently overwrite each other — lost update.
--
-- Per JPA spec, @Version field must be: Long type, NOT NULL DEFAULT 0.
-- Hibernate auto-increments on UPDATE. OptimisticLockingException thrown on mismatch.

ALTER TABLE transactions
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE scheduled_transfers
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE batch_disbursements
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Ensure existing rows have version=0
UPDATE transactions SET version = 0 WHERE version IS NULL;
UPDATE scheduled_transfers SET version = 0 WHERE version IS NULL;
UPDATE batch_disbursements SET version = 0 WHERE version IS NULL;
