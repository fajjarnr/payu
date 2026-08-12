package id.payu.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank
    private String senderAccountId;

    @NotBlank
    private String recipientAccountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String referenceId;

    private String description;

    public String getSenderAccountId() { return senderAccountId; }
    public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }
    public String getRecipientAccountId() { return recipientAccountId; }
    public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
