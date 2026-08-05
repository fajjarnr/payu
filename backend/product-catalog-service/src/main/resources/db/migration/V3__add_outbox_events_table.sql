-- Outbox Events Table for Transactional Outbox Pattern
-- Enables reliable event publishing within database transactions
-- Source: backend/shared/flyway/migrations/V1.0.0__create_outbox_events_table.sql

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB,
    destination_topic VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    sequence_num BIGSERIAL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),

    CONSTRAINT chk_aggregate_type_not_empty CHECK (LENGTH(TRIM(aggregate_type)) > 0),
    CONSTRAINT chk_aggregate_id_not_empty CHECK (LENGTH(TRIM(aggregate_id)) > 0),
    CONSTRAINT chk_event_type_not_empty CHECK (LENGTH(TRIM(event_type)) > 0),
    CONSTRAINT chk_retry_count_non_negative CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON outbox_events(aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_outbox_published
    ON outbox_events(published_at)
    WHERE published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_created
    ON outbox_events(created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_event_type
    ON outbox_events(event_type);

CREATE INDEX IF NOT EXISTS idx_outbox_sequence
    ON outbox_events(sequence_num);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished_retry
    ON outbox_events(created_at, retry_count)
    WHERE published_at IS NULL;

COMMENT ON TABLE outbox_events IS 'Stores events to be published to message broker in an atomic way with transaction';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of the aggregate root (e.g., ORDER, PAYMENT)';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'Identifier of the aggregate instance';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of the domain event';
COMMENT ON COLUMN outbox_events.payload IS 'Event payload in JSON format';
COMMENT ON COLUMN outbox_events.headers IS 'Additional headers for the message';
COMMENT ON COLUMN outbox_events.destination_topic IS 'Target Kafka/AMQ topic for publishing';
COMMENT ON COLUMN outbox_events.published_at IS 'Timestamp when event was successfully published (NULL = pending)';
COMMENT ON COLUMN outbox_events.sequence_num IS 'Global sequence number for ordering';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of publish attempts';
COMMENT ON COLUMN outbox_events.last_error IS 'Last error message if publish failed';
