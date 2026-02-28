package id.payu.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RevenueSplit aggregate root for revenue sharing / royalty configuration (GAP-013).
 * Defines how settlement amounts are split among multiple stakeholders.
 */
public class RevenueSplit {

    private UUID id;
    private String partnerId;
    private String name;
    private String description;
    private SplitType splitType;
    private List<Stakeholder> stakeholders;
    private boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String tenantId;

    public RevenueSplit() {
        this.stakeholders = new ArrayList<>();
    }

    public RevenueSplit(UUID id, String partnerId, String name, String description,
                        SplitType splitType, List<Stakeholder> stakeholders, boolean active,
                        LocalDateTime effectiveFrom, LocalDateTime effectiveUntil,
                        LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String tenantId) {
        this.id = id;
        this.partnerId = partnerId;
        this.name = name;
        this.description = description;
        this.splitType = splitType;
        this.stakeholders = stakeholders != null ? stakeholders : new ArrayList<>();
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.tenantId = tenantId;
    }

    /**
     * Create a new revenue split configuration.
     */
    public static RevenueSplit create(String partnerId, String name, String description,
                                       SplitType splitType, String createdBy) {
        RevenueSplit split = new RevenueSplit();
        split.id = UUID.randomUUID();
        split.partnerId = partnerId;
        split.name = name;
        split.description = description;
        split.splitType = splitType;
        split.stakeholders = new ArrayList<>();
        split.active = true;
        split.effectiveFrom = LocalDateTime.now();
        split.createdAt = LocalDateTime.now();
        split.updatedAt = LocalDateTime.now();
        split.createdBy = createdBy;
        return split;
    }

    /**
     * Add a stakeholder to this revenue split.
     */
    public void addStakeholder(String accountId, String name, BigDecimal percentage,
                                BigDecimal fixedAmount, int priority) {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId(UUID.randomUUID());
        stakeholder.setRevenueSplitId(this.id);
        stakeholder.setAccountId(accountId);
        stakeholder.setName(name);
        stakeholder.setPercentage(percentage);
        stakeholder.setFixedAmount(fixedAmount);
        stakeholder.setPriority(priority);
        stakeholder.setCreatedAt(LocalDateTime.now());
        stakeholders.add(stakeholder);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate split amounts for a given total amount.
     * Returns list of calculated splits ready for execution.
     */
    public List<CalculatedSplit> calculateSplits(BigDecimal totalAmount) {
        List<CalculatedSplit> results = new ArrayList<>();
        BigDecimal remainingAmount = totalAmount;

        // Sort by priority (higher priority first)
        stakeholders.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        for (Stakeholder stakeholder : stakeholders) {
            BigDecimal amount = BigDecimal.ZERO;

            if (stakeholder.getFixedAmount() != null && stakeholder.getFixedAmount().compareTo(BigDecimal.ZERO) > 0) {
                amount = stakeholder.getFixedAmount().min(remainingAmount);
            }

            if (stakeholder.getPercentage() != null && stakeholder.getPercentage().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentageAmount = totalAmount
                        .multiply(stakeholder.getPercentage())
                        .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                amount = amount.add(percentageAmount);
            }

            // Ensure we don't exceed remaining amount
            amount = amount.min(remainingAmount);
            remainingAmount = remainingAmount.subtract(amount);

            CalculatedSplit split = new CalculatedSplit();
            split.setStakeholderId(stakeholder.getId());
            split.setAccountId(stakeholder.getAccountId());
            split.setName(stakeholder.getName());
            split.setAmount(amount);
            results.add(split);
        }

        return results;
    }

    /**
     * Validate that percentages sum to 100% (for percentage-based splits).
     */
    public boolean isValid() {
        if (splitType == SplitType.PERCENTAGE || splitType == SplitType.MIXED) {
            BigDecimal totalPercentage = stakeholders.stream()
                    .map(s -> s.getPercentage() != null ? s.getPercentage() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return totalPercentage.compareTo(new BigDecimal("100")) <= 0;
        }
        return !stakeholders.isEmpty();
    }

    public void deactivate() {
        this.active = false;
        this.effectiveUntil = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEffectiveAt(LocalDateTime timestamp) {
        if (effectiveFrom != null && timestamp.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveUntil != null && timestamp.isAfter(effectiveUntil)) {
            return false;
        }
        return active;
    }

    public enum SplitType {
        PERCENTAGE,
        FIXED,
        MIXED
    }

    public static RevenueSplitBuilder builder() {
        return new RevenueSplitBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public List<Stakeholder> getStakeholders() { return stakeholders; }
    public void setStakeholders(List<Stakeholder> stakeholders) { this.stakeholders = stakeholders; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static class RevenueSplitBuilder {
        private UUID id;
        private String partnerId;
        private String name;
        private String description;
        private SplitType splitType;
        private List<Stakeholder> stakeholders;
        private boolean active;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveUntil;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String tenantId;

        RevenueSplitBuilder() {}

        public RevenueSplitBuilder id(UUID id) { this.id = id; return this; }
        public RevenueSplitBuilder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public RevenueSplitBuilder name(String name) { this.name = name; return this; }
        public RevenueSplitBuilder description(String description) { this.description = description; return this; }
        public RevenueSplitBuilder splitType(SplitType splitType) { this.splitType = splitType; return this; }
        public RevenueSplitBuilder stakeholders(List<Stakeholder> stakeholders) { this.stakeholders = stakeholders; return this; }
        public RevenueSplitBuilder active(boolean active) { this.active = active; return this; }
        public RevenueSplitBuilder effectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; return this; }
        public RevenueSplitBuilder effectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; return this; }
        public RevenueSplitBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RevenueSplitBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public RevenueSplitBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public RevenueSplitBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }

        public RevenueSplit build() {
            return new RevenueSplit(id, partnerId, name, description, splitType, stakeholders,
                    active, effectiveFrom, effectiveUntil, createdAt, updatedAt, createdBy, tenantId);
        }
    }
}
