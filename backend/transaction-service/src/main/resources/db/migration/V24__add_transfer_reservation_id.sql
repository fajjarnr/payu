ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS reservation_id VARCHAR(64);

ALTER TABLE transactions_partitioned
    ADD COLUMN IF NOT EXISTS reservation_id VARCHAR(64);
