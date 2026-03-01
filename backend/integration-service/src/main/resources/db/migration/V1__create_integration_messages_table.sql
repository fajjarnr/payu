-- Migration: Create integration_messages table
-- Purpose: Store integration messages for SWIFT, OJK, and SOAP processing

CREATE TABLE IF NOT EXISTS integration_messages (
    message_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    source_system VARCHAR(100),
    target_system VARCHAR(100),
    correlation_id VARCHAR(36),
    business_reference VARCHAR(100),
    raw_payload TEXT,
    transformed_payload TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    last_retry_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_intmsg_status ON integration_messages(status);
CREATE INDEX IF NOT EXISTS idx_intmsg_type ON integration_messages(type);
CREATE INDEX IF NOT EXISTS idx_intmsg_created ON integration_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_intmsg_correlation ON integration_messages(correlation_id);
CREATE INDEX IF NOT EXISTS idx_intmsg_business_ref ON integration_messages(business_reference);
CREATE INDEX IF NOT EXISTS idx_intmsg_source_target ON integration_messages(source_system, target_system);

-- Index for retry queries
CREATE INDEX IF NOT EXISTS idx_intmsg_retry ON integration_messages(status, retry_count, max_retries)
    WHERE status = 'FAILED';

-- Comments
COMMENT ON TABLE integration_messages IS 'Stores integration messages for SWIFT, OJK, and SOAP processing';
COMMENT ON COLUMN integration_messages.message_id IS 'Unique identifier for the message';
COMMENT ON COLUMN integration_messages.type IS 'Message type: SWIFT_MT103, SWIFT_MT202, OJK_CSV, OJK_XML, SOAP, HTTP_JSON';
COMMENT ON COLUMN integration_messages.direction IS 'Message direction: INBOUND or OUTBOUND';
COMMENT ON COLUMN integration_messages.status IS 'Processing status: RECEIVED, VALIDATING, TRANSFORMING, TRANSFORMED, SENDING, SENT, FAILED, RETRYING, CANCELLED';
