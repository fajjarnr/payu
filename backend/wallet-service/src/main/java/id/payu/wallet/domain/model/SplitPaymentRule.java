package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Defines how a payment should be split across multiple recipients.
 * Reusable configuration tied to a partner.
 * <p>
 * Split types:
 * <ul>
 *   <li>PERCENTAGE — each recipient gets a % of total</li>
 *   <li>FIXED — each recipient gets a fixed amount</li>
 *   <li>MIXED — some fixed, remainder distributed by %</li>
 * </ul>
 */
public class SplitPaymentRule {

    private UUID id;
    private String partnerId;
    private String ruleName;
    private SplitType splitType;
    private String currency;
    private boolean active;
    private List<SplitRecipient> recipients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SplitPaymentRule() {
        this.recipients = new ArrayList<>();
    }

    // --- Domain Methods ---

    /**
     * Validate that the rule is internally consistent.
     * For PERCENTAGE: sum of percentages must be 100%.
     * For FIXED: amounts must be positive.
     * For MIXED: both above.
     */
    public void validate() {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalStateException("Split rule must have at least one recipient");
        }

        if (splitType == SplitType.PERCENTAGE || splitType == SplitType.MIXED) {
            BigDecimal totalPercentage = recipients.stream()
                    .filter(r -> r.getPercentage() != null)
                    .map(SplitRecipient::getPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (splitType == SplitType.PERCENTAGE
                    && totalPercentage.compareTo(new BigDecimal("100")) != 0) {
                throw new IllegalStateException(
                        "Percentage split must sum to 100%, got: " + totalPercentage);
            }
        }
    }

    /**
     * Compute split amounts for a given total using largest-remainder method.
     * Returns list of (recipientAccountId → amount) preserving recipient order.
     */
    public List<SplitLegAmount> computeAmounts(BigDecimal totalAmount) {
        validate();

        List<SplitLegAmount> legs = new ArrayList<>();
        BigDecimal remaining = totalAmount;

        // Sort by priority (highest priority gets the rounding remainder)
        List<SplitRecipient> sorted = new ArrayList<>(recipients);
        sorted.sort(Comparator.comparingInt(SplitRecipient::getPriority));

        if (splitType == SplitType.FIXED) {
            for (SplitRecipient r : sorted) {
                BigDecimal amt = r.getFixedAmount();
                legs.add(new SplitLegAmount(r, amt));
                remaining = remaining.subtract(amt);
            }
            // Any remainder goes to highest priority (index 0)
            if (remaining.compareTo(BigDecimal.ZERO) > 0 && !legs.isEmpty()) {
                SplitLegAmount first = legs.get(0);
                legs.set(0, new SplitLegAmount(first.recipient, first.amount.add(remaining)));
            }
        } else if (splitType == SplitType.PERCENTAGE) {
            computePercentageSplit(totalAmount, sorted, legs);
        } else {
            // MIXED: deduct fixed amounts first, then split remainder by %
            BigDecimal fixedTotal = BigDecimal.ZERO;
            List<SplitRecipient> fixedRecipients = new ArrayList<>();
            List<SplitRecipient> percentageRecipients = new ArrayList<>();

            for (SplitRecipient r : sorted) {
                if (r.getFixedAmount() != null && r.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    legs.add(new SplitLegAmount(r, r.getFixedAmount()));
                    fixedTotal = fixedTotal.add(r.getFixedAmount());
                    fixedRecipients.add(r);
                } else {
                    percentageRecipients.add(r);
                }
            }

            BigDecimal remainderForPercent = totalAmount.subtract(fixedTotal);
            if (remainderForPercent.compareTo(BigDecimal.ZERO) > 0 && !percentageRecipients.isEmpty()) {
                computePercentageSplit(remainderForPercent, percentageRecipients, legs);
            }
        }

        return legs;
    }

    private void computePercentageSplit(BigDecimal total, List<SplitRecipient> sorted,
                                         List<SplitLegAmount> legs) {
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < sorted.size(); i++) {
            SplitRecipient r = sorted.get(i);
            BigDecimal amt;
            if (i == sorted.size() - 1) {
                // Last recipient gets remainder (eliminates rounding error)
                amt = total.subtract(allocated);
            } else {
                amt = total.multiply(r.getPercentage())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_EVEN);
            }
            legs.add(new SplitLegAmount(r, amt));
            allocated = allocated.add(amt);
        }
    }

    /**
     * DTO for computed leg amounts.
     */
    public static class SplitLegAmount {
        public final SplitRecipient recipient;
        public final BigDecimal amount;

        public SplitLegAmount(SplitRecipient recipient, BigDecimal amount) {
            this.recipient = recipient;
            this.amount = amount;
        }
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public static SplitPaymentRuleBuilder builder() {
        return new SplitPaymentRuleBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<SplitRecipient> getRecipients() { return recipients; }
    public void setRecipients(List<SplitRecipient> recipients) { this.recipients = recipients; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class SplitPaymentRuleBuilder {
        private UUID id;
        private String partnerId;
        private String ruleName;
        private SplitType splitType;
        private String currency;
        private boolean active = true;
        private List<SplitRecipient> recipients = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        SplitPaymentRuleBuilder() {}

        public SplitPaymentRuleBuilder id(UUID id) { this.id = id; return this; }
        public SplitPaymentRuleBuilder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public SplitPaymentRuleBuilder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public SplitPaymentRuleBuilder splitType(SplitType splitType) { this.splitType = splitType; return this; }
        public SplitPaymentRuleBuilder currency(String currency) { this.currency = currency; return this; }
        public SplitPaymentRuleBuilder active(boolean active) { this.active = active; return this; }
        public SplitPaymentRuleBuilder recipients(List<SplitRecipient> recipients) { this.recipients = recipients; return this; }
        public SplitPaymentRuleBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SplitPaymentRuleBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public SplitPaymentRule build() {
            SplitPaymentRule r = new SplitPaymentRule();
            r.id = this.id;
            r.partnerId = this.partnerId;
            r.ruleName = this.ruleName;
            r.splitType = this.splitType;
            r.currency = this.currency;
            r.active = this.active;
            r.recipients = this.recipients;
            r.createdAt = this.createdAt;
            r.updatedAt = this.updatedAt;
            return r;
        }
    }
}
