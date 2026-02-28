package id.payu.transaction.domain.port.in;

import id.payu.transaction.domain.model.Disbursement;
import id.payu.transaction.domain.model.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port defining the disbursement (payout) use cases.
 *
 * <p>This interface defines the operations available for managing disbursements
 * including creation, processing, and status tracking. It follows the hexagonal
 * architecture pattern where the domain defines the contract and adapters implement it.</p>
 *
 * <p>Key Operations:</p>
 * <ul>
 *   <li>Create disbursement with idempotency protection</li>
 *   <li>Process disbursement through BI-FAST</li>
 *   <li>Handle BI-FAST callbacks (success/failure)</li>
 *   <li>Query disbursement status</li>
 * </ul>
 *
 * @see Disbursement
 */
public interface DisbursementUseCase {

    /**
     * Creates a new disbursement (payout) request.
     * The disbursement is created in PENDING status and funds are reserved.
     *
     * @param sourceAccountId the source wallet/account ID
     * @param amount the amount to disburse
     * @param bankCode the destination bank code
     * @param accountNumber the destination account number
     * @param accountName the destination account name
     * @param description optional description
     * @param idempotencyKey optional idempotency key for duplicate protection
     * @return the created disbursement
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if duplicate idempotency key exists with different parameters
     */
    Disbursement createDisbursement(
            UUID sourceAccountId,
            Money amount,
            String bankCode,
            String accountNumber,
            String accountName,
            String description,
            String idempotencyKey
    );

    /**
     * Gets a disbursement by ID.
     *
     * @param id the disbursement ID
     * @return optional containing the disbursement if found
     */
    Optional<Disbursement> getDisbursement(UUID id);

    /**
     * Finds a disbursement by idempotency key.
     *
     * @param idempotencyKey the idempotency key
     * @return optional containing the disbursement if found
     */
    Optional<Disbursement> findByIdempotencyKey(String idempotencyKey);

    /**
     * Lists disbursements for a source account.
     *
     * @param sourceAccountId the source account ID
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of disbursements
     */
    List<Disbursement> listDisbursementsByAccount(UUID sourceAccountId, int limit, int offset);

    /**
     * Processes a pending disbursement.
     * Transitions status to PROCESSING and initiates BI-FAST transfer.
     *
     * @param id the disbursement ID
     * @return the updated disbursement
     * @throws IllegalStateException if disbursement is not in PENDING status
     */
    Disbursement processDisbursement(UUID id);

    /**
     * Handles successful BI-FAST callback.
     * Transitions status to COMPLETED.
     *
     * @param id the disbursement ID
     * @param bankReference the bank reference number
     * @return the updated disbursement
     */
    Disbursement completeDisbursement(UUID id, String bankReference);

    /**
     * Handles failed BI-FAST callback.
     * Transitions status to FAILED and releases reserved funds.
     *
     * @param id the disbursement ID
     * @param reason the failure reason
     * @return the updated disbursement
     */
    Disbursement failDisbursement(UUID id, String reason);

    /**
     * Lists disbursements by status.
     *
     * @param status the status to filter by
     * @param limit maximum number of results
     * @return list of disbursements
     */
    List<Disbursement> listDisbursementsByStatus(String status, int limit);
}
