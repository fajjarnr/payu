package id.payu.partner.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for partner refund operations.
 * Implements rich domain behavior for refund lifecycle management.
 */
public class Refund {

    private UUID id;
    private UUID transactionId;
    private UUID partnerId;
    private BigDecimal amount;
    private String reason;
    private String requestedBy;
    private RefundStatus status;
    private Instant requestedAt;
    private Instant processedAt;
    private Instant completedAt;
    private String failureReason;
    private String processorId;
    private String refundTransactionId;

    private Refund() {
    }

    /**
     * Factory method to create a new refund request.
     */
    public static Refund create(UUID transactionId, UUID partnerId, BigDecimal amount,
                                 String reason, String requestedBy) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be blank");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new IllegalArgumentException("RequestedBy cannot be blank");
        }
        Refund refund = new Refund();
        refund.id = UUID.randomUUID();
        refund.transactionId = transactionId;
        refund.partnerId = partnerId;
        refund.amount = amount;
        refund.reason = reason;
        refund.requestedBy = requestedBy;
        refund.status = RefundStatus.PENDING;
        refund.requestedAt = Instant.now();
        return refund;
    }

    /**
     * Transition to PROCESSING state.
     */
    public void process(String processorId) {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException("Cannot process refund in " + status + " status");
        }
        if (processorId == null || processorId.isBlank()) {
            throw new IllegalArgumentException("Processor ID cannot be blank");
        }
        this.processorId = processorId;
        this.status = RefundStatus.PROCESSING;
        this.processedAt = Instant.now();
    }

    /**
     * Mark refund as COMPLETED.
     */
    public void complete(String refundTransactionId) {
        if (this.status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Cannot complete refund in " + status + " status");
        }
        if (refundTransactionId == null || refundTransactionId.isBlank()) {
            throw new IllegalArgumentException("Refund transaction ID cannot be blank");
        }
        this.refundTransactionId = refundTransactionId;
        this.status = RefundStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * Mark refund as FAILED.
     */
    public void fail(String failureReason) {
        if (this.status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Cannot fail refund in " + status + " status");
        }
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Failure reason cannot be blank");
        }
        this.failureReason = failureReason;
        this.status = RefundStatus.FAILED;
        this.completedAt = Instant.now();
    }

    /**
     * Check if refund is in a terminal state (COMPLETED or FAILED).
     */
    public boolean isTerminal() {
        return status == RefundStatus.COMPLETED || status == RefundStatus.FAILED;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getProcessorId() {
        return processorId;
    }

    public String getRefundTransactionId() {
        return refundTransactionId;
    }
}
