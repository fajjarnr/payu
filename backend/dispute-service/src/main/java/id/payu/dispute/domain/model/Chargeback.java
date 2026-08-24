package id.payu.dispute.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate Root for chargeback via scheme (ADR-0054).
 * Hexagonal: business invariants enforced here, not in service layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chargeback {

    private UUID id;
    private UUID transactionId;
    private UUID customerId;
    private UUID merchantId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private ChargebackStatus status;
    private String schemeCaseId;
    private String rejectionReason;
    private Instant createdAt;
    private Instant submittedAt;
    private Instant underReviewAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant reversedAt;
    private Instant closedAt;
    @Builder.Default
    private Long version = 0L;

    public static Chargeback create(UUID transactionId, UUID customerId, UUID merchantId,
                                    BigDecimal amount, String currency, String reason) {
        if (transactionId == null) throw new IllegalArgumentException("Transaction ID cannot be null");
        if (customerId == null) throw new IllegalArgumentException("Customer ID cannot be null");
        if (merchantId == null) throw new IllegalArgumentException("Merchant ID cannot be null");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (currency == null || currency.trim().isEmpty()) throw new IllegalArgumentException("Currency cannot be null or empty");
        if (reason == null || reason.trim().isEmpty()) throw new IllegalArgumentException("Reason cannot be null or empty");
        return Chargeback.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .customerId(customerId)
                .merchantId(merchantId)
                .amount(amount)
                .currency(currency)
                .reason(reason)
                .status(ChargebackStatus.OPEN)
                .createdAt(Instant.now())
                .build();
    }

    public void submit(String schemeCaseId) {
        if (status != ChargebackStatus.OPEN) throw new IllegalStateException("Cannot submit chargeback in status: " + status);
        if (schemeCaseId == null || schemeCaseId.trim().isEmpty()) throw new IllegalArgumentException("Scheme case ID cannot be null or empty");
        this.status = ChargebackStatus.SUBMITTED;
        this.schemeCaseId = schemeCaseId;
        this.submittedAt = Instant.now();
    }

    public void startReview() {
        if (status != ChargebackStatus.SUBMITTED) throw new IllegalStateException("Cannot start review in status: " + status);
        this.status = ChargebackStatus.UNDER_REVIEW;
        this.underReviewAt = Instant.now();
    }

    public void accept() {
        if (status != ChargebackStatus.UNDER_REVIEW) throw new IllegalStateException("Cannot accept chargeback in status: " + status);
        this.status = ChargebackStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void reject(String rejectionReason) {
        if (status != ChargebackStatus.UNDER_REVIEW && status != ChargebackStatus.SUBMITTED) throw new IllegalStateException("Cannot reject chargeback in status: " + status);
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) throw new IllegalArgumentException("Rejection reason cannot be null or empty");
        this.status = ChargebackStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.rejectedAt = Instant.now();
    }

    public void reverse() {
        if (status != ChargebackStatus.ACCEPTED) throw new IllegalStateException("Cannot reverse chargeback in status: " + status);
        this.status = ChargebackStatus.REVERSED;
        this.reversedAt = Instant.now();
    }

    public void close() {
        if (status != ChargebackStatus.ACCEPTED && status != ChargebackStatus.REJECTED && status != ChargebackStatus.REVERSED) {
            throw new IllegalStateException("Cannot close chargeback in status: " + status);
        }
        this.status = ChargebackStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public boolean isTerminal() {
        return status == ChargebackStatus.CLOSED;
    }
}
