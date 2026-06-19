-- V2__add_version_to_compliance_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to compliance-service entities.

ALTER TABLE audit_reports                       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE data_access_audits                  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE audit_reports                       SET version = 0 WHERE version IS NULL;
UPDATE data_access_audits                  SET version = 0 WHERE version IS NULL;
