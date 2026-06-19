-- V3__add_version_to_cms_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to cms-service entities.

ALTER TABLE cms_contents                        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE cms_contents                        SET version = 0 WHERE version IS NULL;
