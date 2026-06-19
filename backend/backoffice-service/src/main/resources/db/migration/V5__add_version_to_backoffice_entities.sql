-- V5__add_version_to_backoffice_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to backoffice-service entities.

ALTER TABLE customer_cases                      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE backoffice_admins                   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE fraud_cases                         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE kyc_reviews                         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE customer_cases                      SET version = 0 WHERE version IS NULL;
UPDATE backoffice_admins                   SET version = 0 WHERE version IS NULL;
UPDATE fraud_cases                         SET version = 0 WHERE version IS NULL;
UPDATE kyc_reviews                         SET version = 0 WHERE version IS NULL;
