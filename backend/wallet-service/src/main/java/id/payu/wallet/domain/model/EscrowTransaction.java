package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Escrow domain model — payment holding for marketplace/partner transactions.
 * <p>
 * Lifecycle:
 * <pre>
 *   CREATED → HELD → RELEASED → SETTLED    (happy path)
 *                  → REFUNDED               (buyer refund)
 *                  → EXPIRED → REFUNDED     (auto-refund on timeout)
 * </pre>
 * <p>
 * Accounting treatment:
 * <ul>
 *   <li>HOLD:    DR Buyer Wallet (1100) / CR Escrow Holdings (2100)</li>
 *   <li>RELEASE: DR Escrow Holdings (2100) / CR Merchant Payable (2200)</li>
 *   <li>SETTLE:  DR Merchant Payable (2200) / CR Merchant Wallet (1100)</li>
 *   <li>REFUND:  DR Escrow Holdings (2100) / CR Buyer Wallet (1100)</li>
 * </ul>
 */
public class EscrowTransaction {

    private UUID id;
    private String buyerAccountId;
    private String sellerAccountId;
    private String partnerId;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private String currency;
    private EscrowStatus status;
    private String externalReferenceId;
    private String description;
    private String reservationId;

    /** When the escrow hold expires and auto-refunds */
    private LocalDateTime expiresAt;
    private LocalDateTime heldAt;
    private LocalDateTime releasedAt;
    private LocalDateTime settledAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EscrowTransaction() {
    }

    public EscrowTransaction(UUID id, String buyerAccountId, String sellerAccountId,
                             String partnerId, BigDecimal amount, BigDecimal feeAmount,
                             String currency, EscrowStatus status,
                             String externalReferenceId, String description,
                             String reservationId, LocalDateTime expiresAt,
                             LocalDateTime heldAt, LocalDateTime releasedAt,
                             LocalDateTime settledAt, LocalDateTime refundedAt,
                             String refundReason,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.buyerAccountId = buyerAccountId;
        this.sellerAccountId = sellerAccountId;
        this.partnerId = partnerId;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.currency = currency;
        this.status = status;
        this.externalReferenceId = externalReferenceId;
        this.description = description;
        this.reservationId = reservationId;
        this.expiresAt = expiresAt;
        this.heldAt = heldAt;
        this.releasedAt = releasedAt;
        this.settledAt = settledAt;
        this.refundedAt = refundedAt;
        this.refundReason = refundReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public enum EscrowStatus {
        CREATED,    // initial state, funds not yet held
        HELD,       // buyer funds reserved in escrow
        RELEASED,   // released to merchant (pending settlement)
        SETTLED,    // merchant has received funds
        REFUNDED,   // funds returned to buyer
        EXPIRED     // transitional — auto-refund triggered
    }

    // --- Domain Methods ---

    /**
     * Mark escrow as held after buyer funds are reserved.
     */
    public void hold(String reservationId) {
        if (this.status != EscrowStatus.CREATED) {
            throw new IllegalStateException("Can only hold from CREATED status, current: " + status);
        }
        this.status = EscrowStatus.HELD;
        this.reservationId = reservationId;
        this.heldAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Release escrow to merchant (condition met: goods received, event completed, etc.).
     */
    public void release() {
        if (this.status != EscrowStatus.HELD) {
            throw new IllegalStateException("Can only release from HELD status, current: " + status);
        }
        this.status = EscrowStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark escrow as fully settled — merchant has received the funds.
     */
    public void settle() {
        if (this.status != EscrowStatus.RELEASED) {
            throw new IllegalStateException("Can only settle from RELEASED status, current: " + status);
        }
        this.status = EscrowStatus.SETTLED;
        this.settledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Refund escrow back to buyer.
     */
    public void refund(String reason) {
        if (this.status != EscrowStatus.HELD && this.status != EscrowStatus.EXPIRED) {
            throw new IllegalStateException("Can only refund from HELD or EXPIRED status, current: " + status);
        }
        this.status = EscrowStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.refundReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as expired (pre-refund transitional state).
     */
    public void expire() {
        if (this.status != EscrowStatus.HELD) {
            throw new IllegalStateException("Can only expire from HELD status, current: " + status);
        }
        this.status = EscrowStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if escrow has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt)
                && status == EscrowStatus.HELD;
    }

    /**
     * Net amount to merchant (amount minus fee).
     */
    public BigDecimal getNetAmount() {
        BigDecimal fee = feeAmount != null ? feeAmount : BigDecimal.ZERO;
        return amount.subtract(fee);
    }

    public static EscrowTransactionBuilder builder() {
        return new EscrowTransactionBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBuyerAccountId() { return buyerAccountId; }
    public void setBuyerAccountId(String buyerAccountId) { this.buyerAccountId = buyerAccountId; }
    public String getSellerAccountId() { return sellerAccountId; }
    public void setSellerAccountId(String sellerAccountId) { this.sellerAccountId = sellerAccountId; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public EscrowStatus getStatus() { return status; }
    public void setStatus(EscrowStatus status) { this.status = status; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public void setHeldAt(LocalDateTime heldAt) { this.heldAt = heldAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class EscrowTransactionBuilder {
        private UUID id;
        private String buyerAccountId;
        private String sellerAccountId;
        private String partnerId;
        private BigDecimal amount;
        private BigDecimal feeAmount;
        private String currency;
        private EscrowStatus status;
        private String externalReferenceId;
        private String description;
        private String reservationId;
        private LocalDateTime expiresAt;
        private LocalDateTime heldAt;
        private LocalDateTime releasedAt;
        private LocalDateTime settledAt;
        private LocalDateTime refundedAt;
        private String refundReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        EscrowTransactionBuilder() {}

        public EscrowTransactionBuilder id(UUID id) { this.id = id; return this; }
        public EscrowTransactionBuilder buyerAccountId(String buyerAccountId) { this.buyerAccountId = buyerAccountId; return this; }
        public EscrowTransactionBuilder sellerAccountId(String sellerAccountId) { this.sellerAccountId = sellerAccountId; return this; }
        public EscrowTransactionBuilder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public EscrowTransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public EscrowTransactionBuilder feeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; return this; }
        public EscrowTransactionBuilder currency(String currency) { this.currency = currency; return this; }
        public EscrowTransactionBuilder status(EscrowStatus status) { this.status = status; return this; }
        public EscrowTransactionBuilder externalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; return this; }
        public EscrowTransactionBuilder description(String description) { this.description = description; return this; }
        public EscrowTransactionBuilder reservationId(String reservationId) { this.reservationId = reservationId; return this; }
        public EscrowTransactionBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public EscrowTransactionBuilder heldAt(LocalDateTime heldAt) { this.heldAt = heldAt; return this; }
        public EscrowTransactionBuilder releasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; return this; }
        public EscrowTransactionBuilder settledAt(LocalDateTime settledAt) { this.settledAt = settledAt; return this; }
        public EscrowTransactionBuilder refundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; return this; }
        public EscrowTransactionBuilder refundReason(String refundReason) { this.refundReason = refundReason; return this; }
        public EscrowTransactionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public EscrowTransactionBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public EscrowTransaction build() {
            return new EscrowTransaction(id, buyerAccountId, sellerAccountId, partnerId,
                    amount, feeAmount, currency, status, externalReferenceId, description,
                    reservationId, expiresAt, heldAt, releasedAt, settledAt, refundedAt,
                    refundReason, createdAt, updatedAt);
        }
    }
}
