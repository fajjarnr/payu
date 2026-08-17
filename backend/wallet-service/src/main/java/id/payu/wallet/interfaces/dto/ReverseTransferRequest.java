package id.payu.wallet.interfaces.dto;

import id.payu.security.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class ReverseTransferRequest {

    @NotBlank
    private String senderAccountId;

    @NotBlank
    private String recipientAccountId;

    @NotNull
    @Positive
    @Sensitive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private UUID refundId;

    private String description;

    public String getSenderAccountId() { return senderAccountId; }
    public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }
    public String getRecipientAccountId() { return recipientAccountId; }
    public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public UUID getRefundId() { return refundId; }
    public void setRefundId(UUID refundId) { this.refundId = refundId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
