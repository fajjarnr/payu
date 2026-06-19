-- V2__add_version_to_support_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to support-service entities.

ALTER TABLE agent_training                      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE training_modules                    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE support_agents                      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE agent_training                      SET version = 0 WHERE version IS NULL;
UPDATE training_modules                    SET version = 0 WHERE version IS NULL;
UPDATE support_agents                      SET version = 0 WHERE version IS NULL;
