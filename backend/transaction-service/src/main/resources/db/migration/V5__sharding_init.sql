-- ===================================================================
-- Transaction Service Sharding/Partitioning Migration
-- ===================================================================
-- This migration implements PostgreSQL declarative partitioning by hash
-- on sender_account_id to distribute data across multiple partitions.
--
-- Partition Strategy:
-- - Type: HASH partitioning
-- - Partition Key: sender_account_id
-- - Number of Partitions: 8 (configurable)
-- - Partition Names: transactions_partition_0 to transactions_partition_7
--
-- Benefits:
-- 1. Automatic data distribution across partitions
-- 2. Partition pruning for efficient queries
-- 3. Parallel query execution
-- 4. Easier archival and maintenance
-- ===================================================================

-- Begin transaction for atomic migration
BEGIN;

-- 1. Create the partitioned table (new schema)
-- ===================================================================
CREATE TABLE IF NOT EXISTS transactions_partitioned (
    id UUID NOT NULL,
    reference_number VARCHAR(50) NOT NULL,
    sender_account_id UUID NOT NULL,
    recipient_account_id UUID,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'IDR',
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT positive_amount CHECK (amount > 0),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
) PARTITION BY HASH (sender_account_id);

-- 2. Create default partition for any data that doesn't match partitions
-- ===================================================================
CREATE TABLE IF NOT EXISTS transactions_partition_default PARTITION OF transactions_partitioned DEFAULT;

-- 3. Create individual partitions
-- ===================================================================
-- Using modulo 8 for hash partitioning
CREATE TABLE IF NOT EXISTS transactions_partition_0 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 0);

CREATE TABLE IF NOT EXISTS transactions_partition_1 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 1);

CREATE TABLE IF NOT EXISTS transactions_partition_2 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 2);

CREATE TABLE IF NOT EXISTS transactions_partition_3 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 3);

CREATE TABLE IF NOT EXISTS transactions_partition_4 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 4);

CREATE TABLE IF NOT EXISTS transactions_partition_5 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 5);

CREATE TABLE IF NOT EXISTS transactions_partition_6 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 6);

CREATE TABLE IF NOT EXISTS transactions_partition_7 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 7);

-- 4. Create primary key and indexes on partitioned table
-- ===================================================================
-- Note: Primary keys on partitioned tables must include the partition key
ALTER TABLE transactions_partitioned ADD CONSTRAINT pk_transactions_partitioned
    PRIMARY KEY (id, sender_account_id);

-- Create unique index on reference number (global across all partitions)
CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_partitioned_reference
    ON transactions_partitioned (reference_number);

-- Create indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_transactions_partitioned_sender
    ON transactions_partitioned (sender_account_id);

CREATE INDEX IF NOT EXISTS idx_transactions_partitioned_recipient
    ON transactions_partitioned (recipient_account_id) WHERE recipient_account_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_partitioned_created
    ON transactions_partitioned (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_partitioned_status
    ON transactions_partitioned (status);

CREATE INDEX IF NOT EXISTS idx_transactions_partitioned_type
    ON transactions_partitioned (type);

-- Composite index for account lookups with pagination
CREATE INDEX IF NOT EXISTS idx_transactions_partitioned_account_created
    ON transactions_partitioned (sender_account_id, created_at DESC);

-- 5. Create function to migrate data from legacy table
-- ===================================================================
CREATE OR REPLACE FUNCTION migrate_to_partitions()
RETURNS BIGINT AS $$
DECLARE
    migrated_count BIGINT;
BEGIN
    -- Insert into partitioned table (PostgreSQL routes to correct partition automatically)
    INSERT INTO transactions_partitioned (
        id, reference_number, sender_account_id, recipient_account_id,
        type, amount, currency, description, status, failure_reason,
        metadata, created_at, updated_at, completed_at
    )
    SELECT
        id, reference_number, sender_account_id, recipient_account_id,
        type, amount, currency, description, status, failure_reason,
        metadata, created_at, updated_at, completed_at
    FROM transactions
    ON CONFLICT (id, sender_account_id) DO NOTHING;

    GET DIAGNOSTICS migrated_count = ROW_COUNT;
    RETURN migrated_count;
END;
$$ LANGUAGE plpgsql;

-- 6. Create function to check migration status
-- ===================================================================
CREATE OR REPLACE FUNCTION get_migration_status()
RETURNS TABLE(
    legacy_count BIGINT,
    partitioned_count BIGINT,
    status TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        (SELECT COUNT(*) FROM transactions) AS legacy_count,
        (SELECT COUNT(*) FROM transactions_partitioned) AS partitioned_count,
        CASE
            WHEN (SELECT COUNT(*) FROM transactions) = (SELECT COUNT(*) FROM transactions_partitioned)
            THEN 'MIGRATED'
            WHEN (SELECT COUNT(*) FROM transactions_partitioned) = 0
            THEN 'NOT_STARTED'
            ELSE 'IN_PROGRESS'
        END AS status;
END;
$$ LANGUAGE plpgsql;

-- 7. Create view for transparent access (optional - for gradual migration)
-- ===================================================================
-- This view allows queries to work seamlessly during migration
CREATE OR REPLACE VIEW transactions_view AS
SELECT * FROM transactions_partitioned;

-- 8. Grant permissions (adjust based on your security requirements)
-- ===================================================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partitioned TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_0 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_1 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_2 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_3 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_4 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_5 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_6 TO payu_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON transactions_partition_7 TO payu_app;
-- GRANT USAGE ON SCHEMA public TO payu_app;

-- 9. Add comments for documentation
-- ===================================================================
COMMENT ON TABLE transactions_partitioned IS 'Hash-partitioned transactions table by sender_account_id';
COMMENT ON TABLE transactions_partition_0 IS 'Partition 0 for transactions (hash modulus 8, remainder 0)';
COMMENT ON TABLE transactions_partition_1 IS 'Partition 1 for transactions (hash modulus 8, remainder 1)';
COMMENT ON TABLE transactions_partition_2 IS 'Partition 2 for transactions (hash modulus 8, remainder 2)';
COMMENT ON TABLE transactions_partition_3 IS 'Partition 3 for transactions (hash modulus 8, remainder 3)';
COMMENT ON TABLE transactions_partition_4 IS 'Partition 4 for transactions (hash modulus 8, remainder 4)';
COMMENT ON TABLE transactions_partition_5 IS 'Partition 5 for transactions (hash modulus 8, remainder 5)';
COMMENT ON TABLE transactions_partition_6 IS 'Partition 6 for transactions (hash modulus 8, remainder 6)';
COMMENT ON TABLE transactions_partition_7 IS 'Partition 7 for transactions (hash modulus 8, remainder 7)';
COMMENT ON FUNCTION migrate_to_partitions() IS 'Migrate data from legacy transactions table to partitioned table';
COMMENT ON FUNCTION get_migration_status() IS 'Check migration status between legacy and partitioned tables';

COMMIT;

-- ===================================================================
-- Migration Instructions:
-- ===================================================================
-- 1. Run this migration to create the partitioned table structure
-- 2. Optionally migrate existing data by executing: SELECT migrate_to_partitions();
-- 3. Check migration status: SELECT * FROM get_migration_status();
-- 4. Update application.yml to set sharding.enabled=true
-- 5. After verification, you can drop the legacy table:
--    DROP TABLE transactions CASCADE;
-- 6. Rename the view to match the original table name (optional):
--    CREATE OR REPLACE VIEW transactions AS SELECT * FROM transactions_partitioned;
-- ===================================================================
