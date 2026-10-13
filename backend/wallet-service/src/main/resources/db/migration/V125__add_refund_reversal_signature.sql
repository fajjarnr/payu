ALTER TABLE refund_reversal_executions
    ADD COLUMN IF NOT EXISTS signature VARCHAR(128);
