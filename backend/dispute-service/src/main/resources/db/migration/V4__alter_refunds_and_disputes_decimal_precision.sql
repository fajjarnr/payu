-- Alter refunds and disputes tables to increase decimal precision to (19,4) per Rule #1
ALTER TABLE refunds ALTER COLUMN amount TYPE DECIMAL(19,4) USING amount::DECIMAL(19,4);
ALTER TABLE disputes ALTER COLUMN disputed_amount TYPE DECIMAL(19,4) USING disputed_amount::DECIMAL(19,4);
