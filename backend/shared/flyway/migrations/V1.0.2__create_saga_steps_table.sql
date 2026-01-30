-- Saga Steps Table for Tracking Individual Saga Steps
-- Provides detailed tracking of each step execution within a saga

CREATE TABLE IF NOT EXISTS saga_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_id UUID NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_order INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    input_payload JSONB,
    output_payload JSONB,
    error_message VARCHAR(1000),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_saga_steps_saga
        FOREIGN KEY (saga_id)
        REFERENCES saga_instances(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_step_name_not_empty CHECK (LENGTH(TRIM(step_name)) > 0),
    CONSTRAINT chk_step_order_non_negative CHECK (step_order >= 0),
    CONSTRAINT chk_status_not_empty CHECK (LENGTH(TRIM(status)) > 0),
    CONSTRAINT chk_status_valid CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATING', 'COMPENSATED'))
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_saga_steps_saga_id
    ON saga_steps(saga_id);

CREATE INDEX IF NOT EXISTS idx_saga_steps_status
    ON saga_steps(status);

CREATE INDEX IF NOT EXISTS idx_saga_steps_saga_order
    ON saga_steps(saga_id, step_order);

CREATE INDEX IF NOT EXISTS idx_saga_steps_status_saga
    ON saga_steps(saga_id, status);

-- Partial index for incomplete steps
CREATE INDEX IF NOT EXISTS idx_saga_steps_incomplete
    ON saga_steps(saga_id, step_order)
    WHERE status IN ('PENDING', 'IN_PROGRESS', 'COMPENSATING');

-- Partial index for failed steps
CREATE INDEX IF NOT EXISTS idx_saga_steps_failed
    ON saga_steps(saga_id, step_order)
    WHERE status = 'FAILED';

COMMENT ON TABLE saga_steps IS 'Stores individual step execution details for saga instances';
COMMENT ON COLUMN saga_steps.saga_id IS 'Reference to the parent saga instance';
COMMENT ON COLUMN saga_steps.step_name IS 'Name of the step (e.g., RESERVE_FUNDS, NOTIFY_PARTNER)';
COMMENT ON COLUMN saga_steps.step_order IS 'Execution order within the saga';
COMMENT ON COLUMN saga_steps.status IS 'Current status: PENDING, IN_PROGRESS, COMPLETED, FAILED, COMPENSATING, COMPENSATED';
COMMENT ON COLUMN saga_steps.input_payload IS 'Input data for the step execution';
COMMENT ON COLUMN saga_steps.output_payload IS 'Output/result data from the step';
COMMENT ON COLUMN saga_steps.error_message IS 'Error details if step failed';
COMMENT ON COLUMN saga_steps.started_at IS 'When step execution started';
COMMENT ON COLUMN saga_steps.completed_at IS 'When step execution completed';
