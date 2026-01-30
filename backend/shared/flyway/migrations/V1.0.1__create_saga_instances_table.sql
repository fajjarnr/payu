-- Saga Instances Table for Orchestrating Long-Running Transactions
-- Supports Saga pattern for distributed transactions

CREATE TABLE IF NOT EXISTS saga_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_type VARCHAR(100) NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    payload JSONB,
    steps JSONB,
    current_step_index INTEGER DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    retry_count INTEGER DEFAULT 0,
    last_error VARCHAR(1000),
    correlation_id VARCHAR(100),

    CONSTRAINT chk_saga_type_not_empty CHECK (LENGTH(TRIM(saga_type)) > 0),
    CONSTRAINT chk_current_state_not_empty CHECK (LENGTH(TRIM(current_state)) > 0),
    CONSTRAINT chk_current_step_index_non_negative CHECK (current_step_index >= 0),
    CONSTRAINT chk_version_non_negative CHECK (version >= 0),
    CONSTRAINT chk_retry_count_non_negative CHECK (retry_count >= 0)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_saga_type
    ON saga_instances(saga_type);

CREATE INDEX IF NOT EXISTS idx_saga_state
    ON saga_instances(current_state);

CREATE INDEX IF NOT EXISTS idx_saga_correlation
    ON saga_instances(correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_saga_started_at
    ON saga_instances(started_at);

CREATE INDEX IF NOT EXISTS idx_saga_type_state
    ON saga_instances(saga_type, current_state);

CREATE INDEX IF NOT EXISTS idx_saga_active
    ON saga_instances(last_updated_at)
    WHERE completed_at IS NULL;

-- Partial index for sagas needing attention (failed/stuck)
CREATE INDEX IF NOT EXISTS idx_saga_needs_attention
    ON saga_instances(last_updated_at, retry_count)
    WHERE completed_at IS NULL AND retry_count > 0;

COMMENT ON TABLE saga_instances IS 'Stores saga orchestration instances for distributed transaction management';
COMMENT ON COLUMN saga_instances.saga_type IS 'Type of saga (e.g., MONEY_TRANSFER, ORDER_PROCESSING)';
COMMENT ON COLUMN saga_instances.current_state IS 'Current state in the saga state machine';
COMMENT ON COLUMN saga_instances.payload IS 'Saga input data and context';
COMMENT ON COLUMN saga_instances.steps IS 'Serialized saga step definitions';
COMMENT ON COLUMN saga_instances.current_step_index IS 'Index of the currently executing step';
COMMENT ON COLUMN saga_instances.completed_at IS 'Timestamp when saga completed (success or failure)';
COMMENT ON COLUMN saga_instances.version IS 'Optimistic locking version';
COMMENT ON COLUMN saga_instances.retry_count IS 'Number of retry attempts';
COMMENT ON COLUMN saga_instances.last_error IS 'Last error message if step failed';
COMMENT ON COLUMN saga_instances.correlation_id IS 'External correlation identifier for tracing';
