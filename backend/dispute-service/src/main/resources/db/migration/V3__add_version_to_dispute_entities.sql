-- V3__add_version_to_dispute_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to dispute-service entities.

ALTER TABLE disputes                            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE dispute_evidence                    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE refunds                             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE disputes                            SET version = 0 WHERE version IS NULL;
UPDATE dispute_evidence                    SET version = 0 WHERE version IS NULL;
UPDATE refunds                             SET version = 0 WHERE version IS NULL;
