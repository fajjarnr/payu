-- Scheduled Transfers table
CREATE TABLE scheduled_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number VARCHAR(50) UNIQUE NOT NULL,
    sender_account_id UUID NOT NULL,
    recipient_account_number VARCHAR(50) NOT NULL,
    recipient_account_id UUID,
    transfer_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    description VARCHAR(500),
    schedule_type VARCHAR(20) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE,
    next_execution_date TIMESTAMP WITH TIME ZONE NOT NULL,
    frequency_days INTEGER,
    day_of_month INTEGER,
    occurrence_count INTEGER,
    executed_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    last_transaction_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT positive_amount CHECK (amount > 0),
    CONSTRAINT valid_schedule_type CHECK (schedule_type IN ('ONE_TIME', 'RECURRING_DAILY', 'RECURRING_WEEKLY', 'RECURRING_MONTHLY', 'RECURRING_CUSTOM')),
    CONSTRAINT valid_scheduled_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT positive_executed_count CHECK (executed_count >= 0),
    CONSTRAINT valid_occurrence_count CHECK (occurrence_count IS NULL OR occurrence_count > 0)
);

CREATE INDEX idx_scheduled_transfers_sender ON scheduled_transfers(sender_account_id);
CREATE INDEX idx_scheduled_transfers_reference ON scheduled_transfers(reference_number);
CREATE INDEX idx_scheduled_transfers_next_execution ON scheduled_transfers(next_execution_date);
CREATE INDEX idx_scheduled_transfers_status ON scheduled_transfers(status);
CREATE INDEX idx_scheduled_transfers_type ON scheduled_transfers(schedule_type);
