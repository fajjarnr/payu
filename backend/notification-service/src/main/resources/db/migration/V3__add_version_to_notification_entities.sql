-- V3__add_version_to_notification_entities.sql
-- ITER-52: Add @Version column (optimistic locking) to notification-service entities.

ALTER TABLE notifications                       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE notifications                       SET version = 0 WHERE version IS NULL;
