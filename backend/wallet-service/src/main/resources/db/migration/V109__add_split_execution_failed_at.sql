-- Align the durable split execution entity with the existing table.
ALTER TABLE split_payment_executions
    ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP;
