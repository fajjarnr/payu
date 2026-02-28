package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stakeholder entity representing a revenue split recipient.
 */
public class Stakeholder {

    private UUID id;
    private UUID revenueSplitId;
    private String accountId;
    private String name;
    private BigDecimal percentage;
    private BigDecimal fixedAmount;
    private int priority;
    private LocalDateTime createdAt;

    public Stakeholder() {
    }

    public Stakeholder(UUID id, UUID revenueSplitId, String accountId, String name,
                       BigDecimal percentage, BigDecimal fixedAmount, int priority, LocalDateTime createdAt) {
        this.id = id;
        this.revenueSplitId = revenueSplitId;
        this.accountId = accountId;
        this.name = name;
        this.percentage = percentage;
        this.fixedAmount = fixedAmount;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public static StakeholderBuilder builder() {
        return new StakeholderBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRevenueSplitId() { return revenueSplitId; }
    public void setRevenueSplitId(UUID revenueSplitId) { this.revenueSplitId = revenueSplitId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class StakeholderBuilder {
        private UUID id;
        private UUID revenueSplitId;
        private String accountId;
        private String name;
        private BigDecimal percentage;
        private BigDecimal fixedAmount;
        private int priority;
        private LocalDateTime createdAt;

        StakeholderBuilder() {}

        public StakeholderBuilder id(UUID id) { this.id = id; return this; }
        public StakeholderBuilder revenueSplitId(UUID revenueSplitId) { this.revenueSplitId = revenueSplitId; return this; }
        public StakeholderBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public StakeholderBuilder name(String name) { this.name = name; return this; }
        public StakeholderBuilder percentage(BigDecimal percentage) { this.percentage = percentage; return this; }
        public StakeholderBuilder fixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; return this; }
        public StakeholderBuilder priority(int priority) { this.priority = priority; return this; }
        public StakeholderBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Stakeholder build() {
            return new Stakeholder(id, revenueSplitId, accountId, name, percentage, fixedAmount, priority, createdAt);
        }
    }
}
