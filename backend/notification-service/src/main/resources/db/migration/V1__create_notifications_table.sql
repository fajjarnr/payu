-- V1__create_notifications_table.sql
-- Description: Initial schema for notification service - notifications table
-- Rollback: DROP TABLE IF EXISTS notifications;

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(500) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(2000),
    template_id VARCHAR(100),
    data TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    
    CONSTRAINT chk_valid_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    CONSTRAINT chk_valid_status CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'))
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notification_channel ON notifications (channel);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON notifications (created_at DESC);

-- Composite index for user's unread notifications
CREATE INDEX IF NOT EXISTS idx_notification_user_unread ON notifications (user_id, status) 
    WHERE status NOT IN ('READ', 'FAILED');

-- Index for retry mechanism
CREATE INDEX IF NOT EXISTS idx_notification_failed_retry ON notifications (status, retry_count)
    WHERE status = 'FAILED' AND retry_count < 3;

COMMENT ON TABLE notifications IS 'Multi-channel notification records (Email, SMS, Push, In-App)';
COMMENT ON COLUMN notifications.recipient IS 'Channel-specific recipient: email address, phone number, or device token';
COMMENT ON COLUMN notifications.data IS 'JSON data for template rendering';
