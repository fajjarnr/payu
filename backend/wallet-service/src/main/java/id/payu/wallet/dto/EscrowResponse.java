package id.payu.wallet.dto;

import id.payu.wallet.domain.model.EscrowTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for escrow transactions.
 */
public class EscrowResponse {

    private UUID id;
    private String buyerAccountId;
    private String sellerAccountId;
    private String partnerId;
    private BigDecimal amount;
    private BigDecimal feeAmount;
    private BigDecimal netAmount;
    private String currency;
    private String status;
    private String externalReferenceId;
    private String description;
    private LocalDateTime expiresAt;
    private LocalDateTime heldAt;
    private LocalDateTime releasedAt;
    private LocalDateTime settledAt;
    private LocalDateTime refundedAt;
    private String refundReason;
    private LocalDateTime createdAt;

    public EscrowResponse() {
    }

    /**
     * Factory method to create response from domain model.
     */
    public static EscrowResponse from(EscrowTransaction escrow) {
        EscrowResponse response = new EscrowResponse();
        response.id = escrow.getId();
        response.buyerAccountId = escrow.getBuyerAccountId();
        response.sellerAccountId = escrow.getSellerAccountId();
        response.partnerId = escrow.getPartnerId();
        response.amount = escrow.getAmount();
        response.feeAmount = escrow.getFeeAmount();
        response.netAmount = escrow.getNetAmount();
        response.currency = escrow.getCurrency();
        response.status = escrow.getStatus().name();
        response.externalReferenceId = escrow.getExternalReferenceId();
        response.description = escrow.getDescription();
        response.expiresAt = escrow.getExpiresAt();
        response.heldAt = escrow.getHeldAt();
        response.releasedAt = escrow.getReleasedAt();
        response.settledAt = escrow.getSettledAt();
        response.refundedAt = escrow.getRefundedAt();
        response.refundReason = escrow.getRefundReason();
        response.createdAt = escrow.getCreatedAt();
        return response;
    }

    // Getters
    public UUID getId() { return id; }
    public String getBuyerAccountId() { return buyerAccountId; }
    public String getSellerAccountId() { return sellerAccountId; }
    public String getPartnerId() { return partnerId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public String getDescription() { return description; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public LocalDateTime getReleasedAt() { return releasedAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public String getRefundReason() { return refundReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
