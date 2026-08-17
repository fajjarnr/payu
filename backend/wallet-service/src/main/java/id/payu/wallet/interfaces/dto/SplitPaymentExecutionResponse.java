package id.payu.wallet.interfaces.dto;

import id.payu.wallet.domain.model.SplitPaymentExecution;
import id.payu.wallet.domain.model.SplitPaymentLeg;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SplitPaymentExecutionResponse {

    private UUID id;
    private UUID splitRuleId;
    private String payerAccountId;
    private BigDecimal totalAmount;
    private String currency;
    private String idempotencyKey;
    private String status;
    private String failureReason;
    private List<LegResponse> legs;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static SplitPaymentExecutionResponse from(SplitPaymentExecution exec) {
        SplitPaymentExecutionResponse r = new SplitPaymentExecutionResponse();
        r.id = exec.getId();
        r.splitRuleId = exec.getSplitRuleId();
        r.payerAccountId = exec.getPayerAccountId();
        r.totalAmount = exec.getTotalAmount();
        r.currency = exec.getCurrency();
        r.idempotencyKey = exec.getIdempotencyKey();
        r.status = exec.getStatus().name();
        r.failureReason = exec.getFailureReason();
        r.legs = exec.getLegs() != null
                ? exec.getLegs().stream().map(LegResponse::from).collect(Collectors.toList())
                : List.of();
        r.createdAt = exec.getCreatedAt();
        r.completedAt = exec.getCompletedAt();
        return r;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getSplitRuleId() { return splitRuleId; }
    public String getPayerAccountId() { return payerAccountId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public List<LegResponse> getLegs() { return legs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public static class LegResponse {
        private UUID id;
        private String recipientAccountId;
        private String recipientLabel;
        private BigDecimal amount;
        private String status;
        private UUID journalEntryId;
        private LocalDateTime settledAt;

        public static LegResponse from(SplitPaymentLeg leg) {
            LegResponse r = new LegResponse();
            r.id = leg.getId();
            r.recipientAccountId = leg.getRecipientAccountId();
            r.recipientLabel = leg.getRecipientLabel();
            r.amount = leg.getAmount();
            r.status = leg.getStatus().name();
            r.journalEntryId = leg.getJournalEntryId();
            r.settledAt = leg.getSettledAt();
            return r;
        }

        public UUID getId() { return id; }
        public String getRecipientAccountId() { return recipientAccountId; }
        public String getRecipientLabel() { return recipientLabel; }
        public BigDecimal getAmount() { return amount; }
        public String getStatus() { return status; }
        public UUID getJournalEntryId() { return journalEntryId; }
        public LocalDateTime getSettledAt() { return settledAt; }
    }
}
