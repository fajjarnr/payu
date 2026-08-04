ALTER TABLE disbursements
    ADD COLUMN IF NOT EXISTS reservation_id VARCHAR(64);
