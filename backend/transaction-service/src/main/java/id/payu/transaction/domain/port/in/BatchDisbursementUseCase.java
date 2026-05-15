package id.payu.transaction.domain.port.in;

import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;
import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.model.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port defining the batch disbursement (bulk payout) use cases.
 *
 * <p>This interface defines operations for managing batch disbursements including
 * batch creation, item management, and bulk processing. It supports payroll,
 * supplier payments, and other mass payout scenarios.</p>
 *
 * <p>Key Operations:</p>
 * <ul>
 *   <li>Create batch with multiple disbursement items</li>
 *   <li>Add items to pending batch</li>
 *   <li>Process batch items sequentially</li>
 *   <li>Track batch progress and aggregate status</li>
 * </ul>
 *
 * @see BatchDisbursementEntity
 * @see DisbursementEntity
 */
public interface BatchDisbursementUseCase {

    /**
     * Creates a new batch disbursement.
     * The batch is created in PENDING status and items can be added.
     *
     * @param sourceAccountId the source wallet/account ID
     * @param name the batch name/description
     * @param description optional detailed description
     * @param idempotencyKey optional idempotency key for duplicate protection
     * @return the created batch disbursement
     */
    BatchDisbursementEntity createBatch(
            UUID sourceAccountId,
            String name,
            String description,
            String idempotencyKey
    );

    /**
     * Adds a disbursement item to a pending batch.
     *
     * @param batchId the batch ID
     * @param amount the amount to disburse
     * @param bankCode the destination bank code
     * @param accountNumber the destination account number
     * @param accountName the destination account name
     * @param description optional description for this item
     * @return the created disbursement item
     * @throws IllegalStateException if batch is not in PENDING status
     */
    DisbursementEntity addBatchItem(
            UUID batchId,
            Money amount,
            String bankCode,
            String accountNumber,
            String accountName,
            String description
    );

    /**
     * Gets a batch disbursement by ID.
     *
     * @param id the batch ID
     * @return optional containing the batch if found
     */
    Optional<BatchDisbursementEntity> getBatch(UUID id);

    /**
     * Finds a batch by idempotency key.
     *
     * @param idempotencyKey the idempotency key
     * @return optional containing the batch if found
     */
    Optional<BatchDisbursementEntity> findBatchByIdempotencyKey(String idempotencyKey);

    /**
     * Lists batch disbursements for a source account.
     *
     * @param sourceAccountId the source account ID
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of batch disbursements
     */
    List<BatchDisbursementEntity> listBatchesByAccount(UUID sourceAccountId, int limit, int offset);

    /**
     * Starts processing a pending batch.
     * Transitions status to PROCESSING and begins item processing.
     *
     * @param id the batch ID
     * @return the updated batch
     * @throws IllegalStateException if batch is not in PENDING status
     */
    BatchDisbursementEntity processBatch(UUID id);

    /**
     * Gets the items in a batch.
     *
     * @param batchId the batch ID
     * @return list of disbursement items
     */
    List<DisbursementEntity> getBatchItems(UUID batchId);

    /**
     * Gets the progress of a batch as percentage.
     *
     * @param batchId the batch ID
     * @return progress percentage (0-100)
     */
    int getBatchProgress(UUID batchId);

    /**
     * Completes a processing batch and calculates final aggregate status.
     *
     * @param id the batch ID
     * @return the updated batch with final status
     */
    BatchDisbursementEntity completeBatch(UUID id);
}
