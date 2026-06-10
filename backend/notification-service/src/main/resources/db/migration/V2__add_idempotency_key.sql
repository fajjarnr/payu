-- V2__add_idempotency_key.sql
-- Description: Add idempotency_key column to prevent duplicate notification sends
-- Rollback: ALTER TABLE notifications DROP COLUMN IF EXISTS idempotency_key;

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notification_idempotency
    ON notifications (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON COLUMN notifications.idempotency_key IS 'Client-supplied idempotency key to prevent duplicate sends';
