-- Align the durable split execution entity with the original partner-aware model.
ALTER TABLE split_payment_executions
    ADD COLUMN IF NOT EXISTS partner_id VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_split_exec_partner
    ON split_payment_executions (partner_id);
