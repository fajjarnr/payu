-- V4__add_version_to_statement_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to statement-service entities.

ALTER TABLE statements                          ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE receipts                            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE statements                          SET version = 0 WHERE version IS NULL;
UPDATE receipts                            SET version = 0 WHERE version IS NULL;
