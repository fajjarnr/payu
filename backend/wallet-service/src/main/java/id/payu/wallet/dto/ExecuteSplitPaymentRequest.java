package id.payu.wallet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for executing a split payment.
 */
public class ExecuteSplitPaymentRequest {

    /** Rule ID — required for rule-based execution, null for ad-hoc. */
    private String ruleId;

    @NotBlank(message = "Payer account ID is required")
    private String payerAccountId;

    private String partnerId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    private String currency;

    private String externalReferenceId;

    @Size(max = 512)
    private String description;

    private String idempotencyKey;

    /** Only for ad-hoc — inline recipients. */
    @Valid
    private List<CreateSplitPaymentRuleRequest.RecipientDto> recipients;

    public ExecuteSplitPaymentRequest() {}

    // Getters and Setters
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getPayerAccountId() { return payerAccountId; }
    public void setPayerAccountId(String payerAccountId) { this.payerAccountId = payerAccountId; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getExternalReferenceId() { return externalReferenceId; }
    public void setExternalReferenceId(String externalReferenceId) { this.externalReferenceId = externalReferenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public List<CreateSplitPaymentRuleRequest.RecipientDto> getRecipients() { return recipients; }
    public void setRecipients(List<CreateSplitPaymentRuleRequest.RecipientDto> recipients) { this.recipients = recipients; }
}
