package id.payu.transaction.service;

import id.payu.transaction.config.ShardingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service for routing queries to appropriate database shards/partitions.
 *
 * <p>This router uses PostgreSQL declarative partitioning, where the database
 * automatically handles partition routing. This service provides:</p>
 *
 * <ul>
 *   <li>Partition calculation for monitoring and logging</li>
 *   <li>Cross-partition query coordination for recipient lookups</li>
 *   <li>Partition-aware query hints for optimization</li>
 * </ul>
 *
 * <p>For most operations, PostgreSQL's partition pruning automatically routes
 * queries to the correct partition based on the WHERE clause.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShardRouter {

    private final ShardingConfig shardingConfig;

    /**
     * Executor for parallel cross-partition queries.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    /**
     * Calculate which partition a transaction belongs to based on sender account ID.
     *
     * @param senderAccountId the sender account ID
     * @return partition number (0 to partitionCount-1)
     */
    public int getPartitionForSender(UUID senderAccountId) {
        int partition = shardingConfig.calculatePartition(senderAccountId);
        log.debug("Calculated partition {} for sender account {}", partition, senderAccountId);
        return partition;
    }

    /**
     * Calculate which partitions need to be queried for recipient account lookups.
     *
     * <p>For recipient queries, we need to search all partitions since data is
     * partitioned by sender account ID. Use partition pruning with recipient_account_id
     * index for optimization.</p>
     *
     * @return list of all partition numbers to query
     */
    public List<Integer> getPartitionsForRecipientQuery() {
        return IntStream.range(0, shardingConfig.getPartitionCount())
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Execute a cross-partition query for recipient lookups.
     *
     * <p>This method runs queries in parallel across partitions and merges results.
     * In production, PostgreSQL will handle this automatically with partition pruning.</p>
     *
     * @param queryFunction function to execute per partition
     * @param <T> result type
     * @return merged results from all partitions
     */
    public <T> List<T> executeCrossPartitionQuery(PartitionQueryFunction<T> queryFunction) {
        if (!shardingConfig.isEnableCrossPartitionQueries()) {
            log.warn("Cross-partition queries are disabled. Only sender account lookups are supported.");
            return Collections.emptyList();
        }

        List<Integer> partitions = getPartitionsForRecipientQuery();
        log.debug("Executing cross-partition query across {} partitions", partitions.size());

        @SuppressWarnings("unchecked")
        CompletableFuture<List<T>>[] futures = partitions.stream()
                .map(partition -> CompletableFuture.supplyAsync(
                        () -> queryFunction.query(partition),
                        executorService
                ))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        return Arrays.stream(futures)
                .flatMap(future -> future.join().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get partition table name for a given partition number.
     *
     * @param partitionNumber the partition number
     * @return table name (e.g., transactions_partition_0)
     */
    public String getPartitionTableName(int partitionNumber) {
        return shardingConfig.getPartitionTableName(partitionNumber);
    }

    /**
     * Get all partition table names.
     *
     * @return list of partition table names
     */
    public List<String> getAllPartitionTableNames() {
        return IntStream.range(0, shardingConfig.getPartitionCount())
                .mapToObj(this::getPartitionTableName)
                .collect(Collectors.toList());
    }

    /**
     * Check if sharding is enabled.
     *
     * @return true if sharding is enabled
     */
    public boolean isShardingEnabled() {
        return shardingConfig.isEnabled();
    }

    /**
     * Get the number of partitions.
     *
     * @return partition count
     */
    public int getPartitionCount() {
        return shardingConfig.getPartitionCount();
    }

    /**
     * Build a partition-aware query hint.
     *
     * @param partitionNumber the target partition
     * @return SQL hint for partition routing
     */
    public String buildPartitionHint(int partitionNumber) {
        return String.format("/*+ SET(enable_partition_pruning on) */ %s",
                getPartitionTableName(partitionNumber));
    }

    /**
     * Functional interface for executing queries per partition.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    public interface PartitionQueryFunction<T> {
        List<T> query(int partitionNumber);
    }

    /**
     * Shutdown the executor service on bean destruction.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
