package id.payu.wallet.interfaces.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for creating an escrow / payment holding.
 */
public class CreateEscrowRequest {

    @NotBlank(message = "Buyer account ID is required")
    private String buyerAccountId;

    @NotBlank(message = "Seller account ID is required")
    private String sellerAccountId;

    private String partnerId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @PositiveOrZero(message = "Fee amount must be zero or positive")
    private BigDecimal feeAmount;

    private String currency;

    private String externalReferenceId;

    @Size(max = 512, message = "Description must be 512 characters or less")
    private String description;

    @Min(value = 1, message = "Expiry must be at least 1 hour")
    @Max(value = 720, message = "Expiry must not exceed 720 hours (30 days)")
    private int expiresInHours = 72;

    public CreateEscrowRequest() {
    }

    public CreateEscrowRequest(String buyerAccountId, String sellerAccountId, String partnerId,
                               BigDecimal amount, BigDecimal feeAmount, String currency,
                               String externalReferenceId, String description, int expiresInHours) {
        this.buyerAccountId = buyerAccountId;
        this.sellerAccountId = sellerAccountId;
        this.partnerId = partnerId;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.currency = currency;
        this.externalReferenceId = externalReferenceId;
        this.description = description;
        this.expiresInHours = expiresInHours;
    }

    // Getters and Setters
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
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(int expiresInHours) { this.expiresInHours = expiresInHours; }
}
