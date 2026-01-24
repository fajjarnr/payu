# Transaction Service - Database Sharding/Partitioning Guide

## Overview

The Transaction Service implements **PostgreSQL declarative partitioning** using hash partitioning on `sender_account_id` to distribute transaction data across multiple partitions.

## Architecture

### Partition Strategy

| Property | Value |
|----------|-------|
| **Partition Type** | HASH |
| **Partition Key** | `sender_account_id` |
| **Number of Partitions** | 8 (configurable: 4, 8, 16, 32) |
| **Table Name** | `transactions_partitioned` |
| **Partition Names** | `transactions_partition_0` to `transactions_partition_7` |

### Benefits

1. **Automatic Data Distribution** - Even distribution across partitions
2. **Partition Pruning** - Queries by sender use single partition
3. **Parallel Query Execution** - PostgreSQL can query partitions in parallel
4. **Easier Maintenance** - Archive/drop individual partitions by time

## Schema Structure

### Partitioned Table

```sql
CREATE TABLE transactions_partitioned (
    id UUID NOT NULL,
    reference_number VARCHAR(50) NOT NULL,
    sender_account_id UUID NOT NULL,      -- PARTITION KEY
    recipient_account_id UUID,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    ...
) PARTITION BY HASH (sender_account_id);
```

### Partitions

```sql
CREATE TABLE transactions_partition_0 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 0);

CREATE TABLE transactions_partition_1 PARTITION OF transactions_partitioned
    FOR VALUES WITH (MODULUS 8, REMAINDER 1);

-- ... continues through partition_7
```

## Configuration

### Application Properties

```yaml
sharding:
  enabled: false                    # Enable after migration
  partition-count: 8                # Must be power of 2
  auto-migrate: true                # Auto-migrate existing data
  migration-batch-size: 1000        # Batch size for migration
  enable-cross-partition-queries: true  # Allow recipient lookups
  max-query-parallelism: 4          # Parallel query limit
  partition-key: senderAccountId    # Field name
```

### ShardingConfig Java Class

Located at: `src/main/java/id/payu/transaction/config/ShardingConfig.java`

```java
@Configuration
@ConfigurationProperties(prefix = "sharding")
public class ShardingConfig {
    private int partitionCount = 8;
    private boolean enabled = false;
    private boolean autoMigrate = true;
    // ... getters, setters, validation
}
```

## Query Patterns

### Single Partition Queries (Fast)

Uses partition pruning - PostgreSQL routes to single partition:

```java
// Finds transactions where user is sender
transactionRepository.findBySenderAccountId(accountId, pageable);
```

### Cross-Partition Queries (Slower)

Scans all partitions - use pagination and indexes:

```java
// Finds transactions where user is recipient
transactionRepository.findByRecipientAccountId(accountId, pageable);

// Finds all transactions for user (sender + recipient)
transactionRepository.findByAccountId(accountId, pageable);
```

### Global Index Queries (Fast)

Uses global index - efficient across all partitions:

```java
// Finds by unique reference number
transactionRepository.findByReferenceNumber(ref);
```

## Migration Guide

### Phase 1: Setup (Downtime: None)

1. **Deploy with Migration**
   ```bash
   # Flyway migration V5__sharding_init.sql creates partitioned tables
   mvn clean package
   oc rollout restart deployment/transaction-service
   ```

2. **Verify Schema**
   ```sql
   SELECT tablename FROM pg_tables
   WHERE tablename LIKE 'transactions_partition%';
   ```

### Phase 2: Migrate Data (Downtime: None)

1. **Migrate Existing Data**
   ```sql
   -- Run this in psql or via migration tool
   SELECT migrate_to_partitions();
   ```

2. **Monitor Progress**
   ```sql
   SELECT * FROM get_migration_status();
   ```

3. **Verify Data Integrity**
   ```sql
   -- Counts should match
   SELECT COUNT(*) FROM transactions;
   SELECT COUNT(*) FROM transactions_partitioned;
   ```

### Phase 3: Enable Sharding (Downtime: None)

1. **Update Configuration**
   ```yaml
   sharding:
     enabled: true
   ```

2. **Deploy Application**
   ```bash
   oc rollout restart deployment/transaction-service
   ```

3. **Monitor Logs**
   ```bash
   oc logs -f deployment/transaction-service | grep partition
   ```

### Phase 4: Cleanup (Optional, Downtime: None)

1. **After Verification Period (e.g., 1 week)**
   ```sql
   -- Drop legacy table (keep backup!)
   DROP TABLE transactions CASCADE;

   -- Or rename for backup
   ALTER TABLE transactions RENAME TO transactions_backup_YYYYMMDD;
   ```

## Monitoring

### Check Partition Sizes

```sql
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    (SELECT COUNT(*) FROM transactions_partitioned) AS total_rows
FROM pg_tables
WHERE tablename LIKE 'transactions_partition%'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Check Data Distribution

```sql
SELECT
    'partition_' || (hashtext(sender_account_id::text) % 8) AS partition,
    COUNT(*) AS count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) AS percentage
FROM transactions_partitioned
GROUP BY partition
ORDER BY partition;
```

### Check Query Performance

```sql
-- Should show "Partition Pruning" for sender queries
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM transactions_partitioned
WHERE sender_account_id = 'your-uuid-here';

-- Should show "Append" with all partitions for recipient queries
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM transactions_partitioned
WHERE recipient_account_id = 'your-uuid-here';
```

## Rollback Procedure

### If Issues Occur After Enabling Sharding

1. **Disable Sharding**
   ```yaml
   sharding:
     enabled: false
   ```

2. **Redeploy Application**
   ```bash
   oc rollout restart deployment/transaction-service
   ```

3. **Investigate Issues** using monitoring queries above

4. **Re-enable** once issues are resolved

### If Data Issues Are Found

1. **Drop Partitioned Table**
   ```sql
   DROP TABLE transactions_partitioned CASCADE;
   ```

2. **Re-run Migration**
   ```bash
   oc rollout undo deployment/transaction-service
   ```

## Performance Considerations

### Sender Queries (Primary Use Case)

- **Partition Pruning**: Single partition scan
- **Index**: Local index on `sender_account_id`
- **Performance**: ~1-5ms per query

### Recipient Queries (Secondary Use Case)

- **Full Scan**: All partitions scanned in parallel
- **Index**: Local index on `recipient_account_id` in each partition
- **Performance**: ~10-50ms per query (depends on partition count)

### Reference Number Lookups

- **Global Index**: Single index across all partitions
- **Performance**: ~1-5ms per query

### Scaling Guidelines

| Transactions | Partitions | Storage per Partition | Query Latency |
|--------------|------------|----------------------|---------------|
| < 1M | 4 | ~250K | < 10ms |
| 1M-10M | 8 | ~1.25M | < 20ms |
| 10M-100M | 16 | ~6.25M | < 50ms |
| > 100M | 32 | ~3M+ | < 100ms |

## Troubleshooting

### Issue: Uneven Data Distribution

**Symptom**: Some partitions have significantly more data than others.

**Solution**: Hash partitioning should distribute evenly. If skewed:
1. Check partition key calculation
2. Verify `sender_account_id` is uniformly distributed
3. Consider changing partition count

### Issue: Slow Recipient Queries

**Symptom**: Queries by recipient account are slow.

**Solutions**:
1. Add index: `CREATE INDEX ON transactions_partition(recipient_account_id)`
2. Use pagination to limit result set
3. Consider caching frequent recipient lookups
4. Set `enable-cross-partition-queries=false` to disable

### Issue: Migration Timeout

**Symptom**: Migration function times out.

**Solutions**:
1. Run in batches manually:
   ```sql
   INSERT INTO transactions_partitioned
   SELECT * FROM transactions
   WHERE created_at < '2024-01-01'
   ON CONFLICT DO NOTHING;
   ```
2. Increase migration batch size in configuration
3. Run during off-peak hours

### Issue: Constraint Violations

**Symptom**: Primary key violations during migration.

**Solution**:
```sql
-- Check for duplicates
SELECT id, COUNT(*)
FROM transactions
GROUP BY id
HAVING COUNT(*) > 1;
```

## References

- [PostgreSQL Table Partitioning](https://www.postgresql.org/docs/current/ddl-partitioning.html)
- [PostgreSQL Partition Pruning](https://www.postgresql.org/docs/current/sql-createtable.html#SQL-CREATETABLE-PARTITIONING)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

## Support

- **Backend Team**: backend-team@payu.id
- **Database Team**: dba-team@payu.id
- **Architecture**: architect@payu.id

---

*Last Updated: January 2026*
