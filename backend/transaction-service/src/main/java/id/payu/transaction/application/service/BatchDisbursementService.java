package id.payu.transaction.application.service;

import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;
import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.model.Money;
import id.payu.transaction.domain.port.in.BatchDisbursementUseCase;
import id.payu.transaction.domain.port.out.BatchDisbursementRepositoryPort;
import id.payu.transaction.domain.port.out.DisbursementRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing batch disbursements (bulk payouts).
 *
 * <p>This service orchestrates the batch disbursement lifecycle including:
 * <ul>
 *   <li>Batch creation with idempotency protection</li>
 *   <li>Item management (adding disbursements to batch)</li>
 *   <li>Sequential batch processing with error handling</li>
 *   <li>Progress tracking and aggregate status calculation</li>
 * </ul>
 *
 * <p>The service processes batch items sequentially with continue-on-error semantics,
 * ensuring partial failures don't stop the entire batch.
 *
 * @see BatchDisbursementEntity
 * @see BatchDisbursementUseCase
 */
@Service
@Transactional(readOnly = true)
public class BatchDisbursementService implements BatchDisbursementUseCase {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchDisbursementService.class);



    private final BatchDisbursementRepositoryPort batchRepository;
    private final DisbursementRepositoryPort disbursementRepository;
    private final DisbursementService disbursementService;

    public BatchDisbursementService(BatchDisbursementRepositoryPort batchRepository,
                                     DisbursementRepositoryPort disbursementRepository,
                                     DisbursementService disbursementService) {
        this.batchRepository = batchRepository;
        this.disbursementRepository = disbursementRepository;
        this.disbursementService = disbursementService;
    }

    @Override
    @Transactional
    public BatchDisbursementEntity createBatch(
            UUID sourceAccountId,
            String name,
            String description,
            String idempotencyKey) {

        log.info("Creating batch disbursement for account: {}, name: {}",
                sourceAccountId, name);

        // Check idempotency
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<BatchDisbursementEntity> existing = batchRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Returning existing batch for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        // Create batch
        BatchDisbursementEntity batch = BatchDisbursementEntity.createWithIdempotencyKey(
                sourceAccountId,
                name,
                idempotencyKey != null && !idempotencyKey.isBlank()
                        ? idempotencyKey
                        : generateIdempotencyKey()
        );

        if (description != null && !description.isBlank()) {
            batch.setDescription(description);
        }

        BatchDisbursementEntity saved = batchRepository.save(batch);
        log.info("Created batch disbursement: {}", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public DisbursementEntity addBatchItem(
            UUID batchId,
            Money amount,
            String bankCode,
            String accountNumber,
            String accountName,
            String description) {

        log.info("Adding item to batch: {}, amount: {}, bank: {}",
                batchId, amount, bankCode);

        BatchDisbursementEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        // Create disbursement item
        DisbursementEntity item = DisbursementEntity.createWithIdempotencyKey(
                batch.getSourceAccountId(),
                amount,
                bankCode,
                accountNumber,
                accountName,
                generateIdempotencyKey()
        );

        if (description != null && !description.isBlank()) {
            item.setDescription(description);
        }

        // Add to batch
        batch.addItem(item);

        // Save both
        disbursementRepository.save(item);
        batchRepository.save(batch);

        log.info("Added item: {} to batch: {}", item.getId(), batchId);
        return item;
    }

    @Override
    public Optional<BatchDisbursementEntity> getBatch(UUID id) {
        return batchRepository.findById(id);
    }

    @Override
    public Optional<BatchDisbursementEntity> findBatchByIdempotencyKey(String idempotencyKey) {
        return batchRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<BatchDisbursementEntity> listBatchesByAccount(UUID sourceAccountId, int limit, int offset) {
        return batchRepository.findBySourceAccountId(sourceAccountId, limit, offset);
    }

    @Override
    @Transactional
    public BatchDisbursementEntity processBatch(UUID id) {
        log.info("Processing batch: {}", id);

        BatchDisbursementEntity batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));

        // Transition to PROCESSING
        batch.process();
        batchRepository.save(batch);

        // Publish batch processing event to Kafka
        // The actual item processing happens asynchronously via Kafka listener
        log.info("Batch {} queued for processing with {} items", id, batch.getItemCount());

        return batch;
    }

    /**
     * Kafka listener for processing batch items.
     * Processes items sequentially with continue-on-error semantics.
     *
     * @param batchId the batch ID to process
     */
    @KafkaListener(topics = "disbursement-batch", groupId = "transaction-service")
    @Transactional
    public void processBatchItems(String batchId) {
        log.info("Processing batch items for batch: {}", batchId);

        BatchDisbursementEntity batch = batchRepository.findById(UUID.fromString(batchId))
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        List<DisbursementEntity> items = batch.getItems();
        int successCount = 0;
        int failCount = 0;

        for (DisbursementEntity item : items) {
            try {
                if (item.isPending()) {
                    // Process the disbursement
                    disbursementService.processDisbursement(item.getId());
                    successCount++;
                    log.debug("Processed batch item: {}", item.getId());
                }
            } catch (Exception e) {
                failCount++;
                log.error("Failed to process batch item: {}", item.getId(), e);
                // Continue processing other items - don't stop the batch
            }
        }

        log.info("Batch {} processing complete. Success: {}, Failed: {}",
                batchId, successCount, failCount);
    }

    @Override
    public List<DisbursementEntity> getBatchItems(UUID batchId) {
        BatchDisbursementEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        return batch.getItems();
    }

    @Override
    public int getBatchProgress(UUID batchId) {
        BatchDisbursementEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        return batch.getProgressPercentage();
    }

    @Override
    @Transactional
    public BatchDisbursementEntity completeBatch(UUID id) {
        log.info("Completing batch: {}", id);

        BatchDisbursementEntity batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));

        batch.complete();
        BatchDisbursementEntity saved = batchRepository.save(batch);

        log.info("Batch {} completed with status: {}", id, saved.getStatus());
        return saved;
    }

    private String generateIdempotencyKey() {
        return "batch-" + UUID.randomUUID().toString().replace("-", "");
    }
}
