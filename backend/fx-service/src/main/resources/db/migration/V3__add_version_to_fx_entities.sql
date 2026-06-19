-- V3__add_version_to_fx_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to fx-service entities.

ALTER TABLE fx_rates                            ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE fx_conversions                      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE fx_rates                            SET version = 0 WHERE version IS NULL;
UPDATE fx_conversions                      SET version = 0 WHERE version IS NULL;
