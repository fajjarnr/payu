-- Virtual ledger accounts use a typed prefix plus a UUID.
ALTER TABLE ledger_entries
    ALTER COLUMN account_id TYPE VARCHAR(128)
    USING account_id::VARCHAR(128);
