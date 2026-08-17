package id.payu.wallet.interfaces.dto;

import id.payu.wallet.domain.model.SplitPaymentRule;
import id.payu.wallet.domain.model.SplitRecipient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SplitPaymentRuleResponse {

    private UUID id;
    private String partnerId;
    private String ruleName;
    private String splitType;
    private String currency;
    private boolean active;
    private List<RecipientResponse> recipients;
    private LocalDateTime createdAt;

    public static SplitPaymentRuleResponse from(SplitPaymentRule rule) {
        SplitPaymentRuleResponse r = new SplitPaymentRuleResponse();
        r.id = rule.getId();
        r.partnerId = rule.getPartnerId();
        r.ruleName = rule.getRuleName();
        r.splitType = rule.getSplitType().name();
        r.currency = rule.getCurrency();
        r.active = rule.isActive();
        r.recipients = rule.getRecipients() != null
                ? rule.getRecipients().stream().map(RecipientResponse::from).collect(Collectors.toList())
                : List.of();
        r.createdAt = rule.getCreatedAt();
        return r;
    }

    // Getters
    public UUID getId() { return id; }
    public String getPartnerId() { return partnerId; }
    public String getRuleName() { return ruleName; }
    public String getSplitType() { return splitType; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public List<RecipientResponse> getRecipients() { return recipients; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class RecipientResponse {
        private UUID id;
        private String recipientAccountId;
        private String recipientLabel;
        private String type;
        private BigDecimal percentage;
        private BigDecimal fixedAmount;
        private int priority;

        public static RecipientResponse from(SplitRecipient r) {
            RecipientResponse resp = new RecipientResponse();
            resp.id = r.getId();
            resp.recipientAccountId = r.getRecipientAccountId();
            resp.recipientLabel = r.getRecipientLabel();
            resp.type = r.getType() != null ? r.getType().name() : null;
            resp.percentage = r.getPercentage();
            resp.fixedAmount = r.getFixedAmount();
            resp.priority = r.getPriority();
            return resp;
        }

        public UUID getId() { return id; }
        public String getRecipientAccountId() { return recipientAccountId; }
        public String getRecipientLabel() { return recipientLabel; }
        public String getType() { return type; }
        public BigDecimal getPercentage() { return percentage; }
        public BigDecimal getFixedAmount() { return fixedAmount; }
        public int getPriority() { return priority; }
    }
}
