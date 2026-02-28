package id.payu.dispute.domain.port.in;

import id.payu.dispute.domain.model.Refund;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for refund use cases.
 *
 * <p>Defines the operations that can be performed on refunds.
 * This interface is implemented by application services.</p>
 */
public interface RefundUseCase {

    /**
     * Creates a full refund for a transaction.
     *
     * @param transactionId the transaction to refund
     * @param reason        the reason for refund
     * @return the created refund
     */
    Refund createFullRefund(UUID transactionId, String reason);

    /**
     * Creates a partial refund for a transaction.
     *
     * @param transactionId the transaction to refund
     * @param amount        the amount to refund
     * @param currency      the currency code
     * @param reason        the reason for refund
     * @return the created refund
     */
    Refund createPartialRefund(UUID transactionId, BigDecimal amount, String currency, String reason);

    /**
     * Processes a refund.
     *
     * @param refundId the refund ID
     * @return the updated refund
     */
    Refund processRefund(UUID refundId);

    /**
     * Completes a refund.
     *
     * @param refundId the refund ID
     * @return the updated refund
     */
    Refund completeRefund(UUID refundId);

    /**
     * Fails a refund.
     *
     * @param refundId      the refund ID
     * @param failureReason the reason for failure
     * @return the updated refund
     */
    Refund failRefund(UUID refundId, String failureReason);

    /**
     * Cancels a refund.
     *
     * @param refundId           the refund ID
     * @param cancellationReason the reason for cancellation
     * @return the updated refund
     */
    Refund cancelRefund(UUID refundId, String cancellationReason);

    /**
     * Gets a refund by ID.
     *
     * @param refundId the refund ID
     * @return optional containing the refund if found
     */
    Optional<Refund> getRefund(UUID refundId);

    /**
     * Gets all refunds for a transaction.
     *
     * @param transactionId the transaction ID
     * @return list of refunds
     */
    List<Refund> getRefundsByTransaction(UUID transactionId);

    /**
     * Gets refunds by status.
     *
     * @param status the refund status
     * @return list of refunds
     */
    List<Refund> getRefundsByStatus(String status);
}
