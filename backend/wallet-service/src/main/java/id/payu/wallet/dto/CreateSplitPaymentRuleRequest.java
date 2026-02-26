package id.payu.wallet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a split payment rule.
 */
public class CreateSplitPaymentRuleRequest {

    @NotBlank(message = "Partner ID is required")
    private String partnerId;

    @NotBlank(message = "Rule name is required")
    @Size(max = 128)
    private String ruleName;

    @NotNull(message = "Split type is required")
    private String splitType;

    private String currency;

    @NotEmpty(message = "At least one recipient is required")
    @Valid
    private List<RecipientDto> recipients;

    public CreateSplitPaymentRuleRequest() {}

    // Getters and Setters
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getSplitType() { return splitType; }
    public void setSplitType(String splitType) { this.splitType = splitType; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<RecipientDto> getRecipients() { return recipients; }
    public void setRecipients(List<RecipientDto> recipients) { this.recipients = recipients; }

    public static class RecipientDto {
        @NotBlank(message = "Recipient account ID is required")
        private String recipientAccountId;

        @NotBlank(message = "Recipient label is required")
        private String recipientLabel;

        private String type;

        @PositiveOrZero
        private BigDecimal percentage;

        @PositiveOrZero
        private BigDecimal fixedAmount;

        private int priority;

        public RecipientDto() {}

        public String getRecipientAccountId() { return recipientAccountId; }
        public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
        public String getRecipientLabel() { return recipientLabel; }
        public void setRecipientLabel(String recipientLabel) { this.recipientLabel = recipientLabel; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
        public BigDecimal getFixedAmount() { return fixedAmount; }
        public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
    }
}
