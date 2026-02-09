-- Saga instances table for PayU Saga Starter
-- Tracks distributed transaction state, steps, and compensation audit trail
CREATE TABLE IF NOT EXISTS saga_instances (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_id             VARCHAR(255) NOT NULL UNIQUE,
    saga_type           VARCHAR(255) NOT NULL,
    current_state       VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    previous_state      VARCHAR(50),
    payload             JSONB,
    step_context        JSONB DEFAULT '{}',
    completed_steps     JSONB DEFAULT '[]',
    current_step        VARCHAR(255),
    error_step          VARCHAR(255),
    error_message       TEXT,
    retry_count         INT NOT NULL DEFAULT 0,
    max_retries         INT NOT NULL DEFAULT 3,
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMP WITH TIME ZONE,
    version             BIGINT NOT NULL DEFAULT 0
);

-- Indexes for saga recovery and monitoring queries
CREATE INDEX IF NOT EXISTS idx_saga_type ON saga_instances(saga_type);
CREATE INDEX IF NOT EXISTS idx_saga_state ON saga_instances(current_state);
CREATE INDEX IF NOT EXISTS idx_saga_started_at ON saga_instances(started_at);
CREATE INDEX IF NOT EXISTS idx_saga_completed_at ON saga_instances(completed_at);
CREATE INDEX IF NOT EXISTS idx_saga_type_state ON saga_instances(saga_type, current_state);

-- GIN index for JSONB payload queries (correlation ID lookups, etc.)
CREATE INDEX IF NOT EXISTS idx_saga_payload_gin ON saga_instances USING GIN (payload);

COMMENT ON TABLE saga_instances IS 'Persists distributed saga state for the BiFast transfer orchestrator';
