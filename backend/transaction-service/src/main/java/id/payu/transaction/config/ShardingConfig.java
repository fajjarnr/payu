package id.payu.transaction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

/**
 * Configuration for database sharding/partitioning.
 *
 * <p>Uses PostgreSQL declarative partitioning by hash on sender_account_id.
 * This provides automatic distribution of data across partitions while
 * maintaining query performance through partition pruning.</p>
 *
 * @see <a href="https://www.postgresql.org/docs/current/ddl-partitioning.html">PostgreSQL Partitioning</a>
 */
@Configuration
@ConfigurationProperties(prefix = "sharding")
public class ShardingConfig {

    /**
     * Number of partitions to create. Must be a power of 2 for hash partitioning.
     * Recommended values: 4, 8, 16, or 32 depending on data volume.
     */
    private int partitionCount = 8;

    /**
     * Enable/disable sharding feature.
     * When disabled, queries go to the legacy transactions table.
     */
    private boolean enabled = false;

    /**
     * Enable automatic migration of data from legacy table to partitions.
     */
    private boolean autoMigrate = true;

    /**
     * Batch size for data migration.
     */
    private int migrationBatchSize = 1000;

    /**
     * Whether to use cross-partition queries for recipient lookups.
     * When true, queries by recipient account ID will scan all partitions.
     * When false, only sender account ID queries are supported.
     */
    private boolean enableCrossPartitionQueries = true;

    /**
     * Maximum parallelism for cross-partition queries.
     * Limits the number of partitions queried concurrently.
     */
    private int maxQueryParallelism = 4;

    /**
     * Partition key field name in the Transaction entity.
     */
    private String partitionKey = "senderAccountId";

    /**
     * List of partition names to use. Generated automatically based on partitionCount.
     */
    private List<String> partitionNames;

    public int getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(int partitionCount) {
        this.partitionCount = partitionCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoMigrate() {
        return autoMigrate;
    }

    public void setAutoMigrate(boolean autoMigrate) {
        this.autoMigrate = autoMigrate;
    }

    public int getMigrationBatchSize() {
        return migrationBatchSize;
    }

    public void setMigrationBatchSize(int migrationBatchSize) {
        this.migrationBatchSize = migrationBatchSize;
    }

    public boolean isEnableCrossPartitionQueries() {
        return enableCrossPartitionQueries;
    }

    public void setEnableCrossPartitionQueries(boolean enableCrossPartitionQueries) {
        this.enableCrossPartitionQueries = enableCrossPartitionQueries;
    }

    public int getMaxQueryParallelism() {
        return maxQueryParallelism;
    }

    public void setMaxQueryParallelism(int maxQueryParallelism) {
        this.maxQueryParallelism = maxQueryParallelism;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    public List<String> getPartitionNames() {
        return partitionNames;
    }

    public void setPartitionNames(List<String> partitionNames) {
        this.partitionNames = partitionNames;
    }

    /**
     * Calculate the partition number for a given account ID using hash partitioning.
     *
     * @param accountId the account ID to partition
     * @return partition number (0 to partitionCount-1)
     */
    public int calculatePartition(UUID accountId) {
        if (accountId == null) {
            return 0;
        }
        // Hash the UUID and take modulo to get partition number
        // Using the least significant bits for better distribution
        long hash = accountId.getLeastSignificantBits() ^ accountId.getMostSignificantBits();
        return Math.abs((int) (hash % partitionCount));
    }

    /**
     * Get the partition table name for a given account ID.
     *
     * @param accountId the account ID
     * @return partition table name (e.g., transactions_partition_0)
     */
    public String getPartitionTableName(UUID accountId) {
        return getPartitionTableName(calculatePartition(accountId));
    }

    /**
     * Get the partition table name for a given partition number.
     *
     * @param partitionNumber the partition number
     * @return partition table name (e.g., transactions_partition_0)
     */
    public String getPartitionTableName(int partitionNumber) {
        return "transactions_partition_" + partitionNumber;
    }

    /**
     * Validate the partition count is a power of 2 (required for hash partitioning).
     *
     * @throws IllegalArgumentException if partition count is invalid
     */
    public void validate() {
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("Partition count must be positive");
        }
        if ((partitionCount & (partitionCount - 1)) != 0) {
            throw new IllegalArgumentException("Partition count must be a power of 2 for hash partitioning");
        }
        if (partitionCount > 64) {
            throw new IllegalArgumentException("Partition count cannot exceed 64");
        }
    }
}
