package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single recipient in a split payment rule.
 * Defines how much of the total amount goes to this recipient.
 */
public class SplitRecipient {

    private UUID id;
    private UUID splitRuleId;
    private String recipientAccountId;
    private String recipientLabel;
    private RecipientType type;
    private BigDecimal percentage;
    private BigDecimal fixedAmount;
    private int priority;
    private LocalDateTime createdAt;

    public SplitRecipient() {
    }

    public static SplitRecipientBuilder builder() {
        return new SplitRecipientBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSplitRuleId() { return splitRuleId; }
    public void setSplitRuleId(UUID splitRuleId) { this.splitRuleId = splitRuleId; }
    public String getRecipientAccountId() { return recipientAccountId; }
    public void setRecipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; }
    public String getRecipientLabel() { return recipientLabel; }
    public void setRecipientLabel(String recipientLabel) { this.recipientLabel = recipientLabel; }
    public RecipientType getType() { return type; }
    public void setType(RecipientType type) { this.type = type; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class SplitRecipientBuilder {
        private UUID id;
        private UUID splitRuleId;
        private String recipientAccountId;
        private String recipientLabel;
        private RecipientType type;
        private BigDecimal percentage;
        private BigDecimal fixedAmount;
        private int priority;
        private LocalDateTime createdAt;

        SplitRecipientBuilder() {}

        public SplitRecipientBuilder id(UUID id) { this.id = id; return this; }
        public SplitRecipientBuilder splitRuleId(UUID splitRuleId) { this.splitRuleId = splitRuleId; return this; }
        public SplitRecipientBuilder recipientAccountId(String recipientAccountId) { this.recipientAccountId = recipientAccountId; return this; }
        public SplitRecipientBuilder recipientLabel(String recipientLabel) { this.recipientLabel = recipientLabel; return this; }
        public SplitRecipientBuilder type(RecipientType type) { this.type = type; return this; }
        public SplitRecipientBuilder percentage(BigDecimal percentage) { this.percentage = percentage; return this; }
        public SplitRecipientBuilder fixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; return this; }
        public SplitRecipientBuilder priority(int priority) { this.priority = priority; return this; }
        public SplitRecipientBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public SplitRecipient build() {
            SplitRecipient r = new SplitRecipient();
            r.id = this.id;
            r.splitRuleId = this.splitRuleId;
            r.recipientAccountId = this.recipientAccountId;
            r.recipientLabel = this.recipientLabel;
            r.type = this.type;
            r.percentage = this.percentage;
            r.fixedAmount = this.fixedAmount;
            r.priority = this.priority;
            r.createdAt = this.createdAt;
            return r;
        }
    }
}
