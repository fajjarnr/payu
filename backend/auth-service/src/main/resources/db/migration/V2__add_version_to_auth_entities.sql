-- V2__add_version_to_auth_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to auth-service entities.

ALTER TABLE user_risk_profiles                  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_known_ips                      ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE user_known_devices                  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE user_risk_profiles                  SET version = 0 WHERE version IS NULL;
UPDATE user_known_ips                      SET version = 0 WHERE version IS NULL;
UPDATE user_known_devices                  SET version = 0 WHERE version IS NULL;
