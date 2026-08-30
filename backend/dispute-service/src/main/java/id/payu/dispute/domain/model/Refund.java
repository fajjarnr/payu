package id.payu.dispute.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate Root representing a refund request.
 *
 * <p>This aggregate manages the lifecycle of a refund from creation to completion or failure.
 * It enforces business invariants and state transitions.</p>
 *
 * <p>State Machine:
 * <pre>
 * PENDING -> PROCESSING -> COMPLETED
 *                     \> FAILED
 *         -> CANCELLED (from PENDING only)
 * </pre></p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    private UUID id;
    private UUID transactionId;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private RefundStatus status;
    private String failureReason;
    private Instant createdAt;
    private Instant processedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;

    /**
     * Creates a new refund request.
     *
     * @param transactionId the transaction to refund
     * @param amount        the amount to refund
     * @param currency      the currency code
     * @param reason        the reason for refund
     * @return a new Refund instance in PENDING status
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static Refund create(UUID transactionId, BigDecimal amount, String currency, String reason) {
        validateCreationParameters(transactionId, amount, currency, reason);

        return Refund.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .amount(amount)
                .currency(currency)
                .reason(reason)
                .status(RefundStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates a full refund request.
     *
     * @param transactionId the transaction to refund
     * @param fullAmount    the full transaction amount
     * @param currency      the currency code
     * @param reason        the reason for refund
     * @return a new Refund instance in PENDING status
     */
    public static Refund createFullRefund(UUID transactionId, BigDecimal fullAmount, String currency, String reason) {
        return create(transactionId, fullAmount, currency, reason);
    }

    /**
     * Creates a partial refund request.
     *
     * @param transactionId the transaction to refund
     * @param partialAmount the partial amount to refund
     * @param currency      the currency code
     * @param reason        the reason for refund
     * @return a new Refund instance in PENDING status
     */
    public static Refund createPartialRefund(UUID transactionId, BigDecimal partialAmount, String currency, String reason) {
        return create(transactionId, partialAmount, currency, reason);
    }

    /**
     * Processes the refund, transitioning from PENDING to PROCESSING.
     *
     * @throws IllegalStateException if the refund is not in PENDING status
     */
    public void process() {
        if (status != RefundStatus.PENDING) {
            throw new IllegalStateException("Cannot process refund in status: " + status);
        }
        this.status = RefundStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    /**
     * Completes the refund, transitioning from PROCESSING to COMPLETED.
     *
     * @throws IllegalStateException if the refund is not in PROCESSING status
     */
    public void complete() {
        if (status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Cannot complete refund in status: " + status);
        }
        this.status = RefundStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Fails the refund, transitioning from PROCESSING to FAILED.
     *
     * @param failureReason the reason for failure
     * @throws IllegalStateException    if the refund is not in PROCESSING status
     * @throws IllegalArgumentException if failureReason is null or empty
     */
    public void fail(String failureReason) {
        if (status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Cannot fail refund in status: " + status);
        }
        if (failureReason == null || failureReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure reason cannot be null or empty");
        }
        this.status = RefundStatus.FAILED;
        this.failureReason = failureReason;
        this.failedAt = Instant.now();
    }

    /**
     * Cancels the refund, transitioning from PENDING to CANCELLED.
     *
     * @param cancellationReason the reason for cancellation
     * @throws IllegalStateException    if the refund is not in PENDING status
     * @throws IllegalArgumentException if cancellationReason is null or empty
     */
    public void cancel(String cancellationReason) {
        if (status != RefundStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel refund in status: " + status);
        }
        if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
        this.status = RefundStatus.CANCELLED;
        this.failureReason = cancellationReason;
        this.cancelledAt = Instant.now();
    }

    /**
     * Checks if the refund is in a terminal state.
     *
     * @return true if the refund is COMPLETED, FAILED, or CANCELLED
     */
    public boolean isInTerminalState() {
        return status == RefundStatus.COMPLETED
                || status == RefundStatus.FAILED
                || status == RefundStatus.CANCELLED;
    }

    private static void validateCreationParameters(UUID transactionId, BigDecimal amount, String currency, String reason) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
    }
}
