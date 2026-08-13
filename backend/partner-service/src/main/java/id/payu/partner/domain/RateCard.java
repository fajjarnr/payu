package id.payu.partner.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RateCard aggregate root for partner pricing configuration (GAP-004).
 * Defines fee structures: flat, percentage, or tiered pricing.
 */
public class RateCard {

    private UUID id;
    private String name;
    private String description;
    private FeeType feeType;
    private BigDecimal flatFee;
    private BigDecimal percentageFee;
    private List<FeeTier> tiers;
    private String currency;
    private BigDecimal minimumFee;
    private BigDecimal maximumFee;
    private boolean active;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String tenantId;

    public RateCard() {
        this.tiers = new ArrayList<>();
    }

    public RateCard(UUID id, String name, String description, FeeType feeType,
                    BigDecimal flatFee, BigDecimal percentageFee, List<FeeTier> tiers,
                    String currency, BigDecimal minimumFee, BigDecimal maximumFee,
                    boolean active, LocalDateTime effectiveFrom, LocalDateTime effectiveUntil,
                    LocalDateTime createdAt, LocalDateTime updatedAt, String createdBy, String tenantId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.feeType = feeType;
        this.flatFee = flatFee;
        this.percentageFee = percentageFee;
        this.tiers = tiers != null ? tiers : new ArrayList<>();
        this.currency = currency;
        this.minimumFee = minimumFee;
        this.maximumFee = maximumFee;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.tenantId = tenantId;
    }

    /**
     * Create a new flat fee rate card.
     */
    public static RateCard createFlatFee(String name, String description,
                                          BigDecimal flatFee, String currency, String createdBy) {
        RateCard card = new RateCard();
        card.id = UUID.randomUUID();
        card.name = name;
        card.description = description;
        card.feeType = FeeType.FLAT;
        card.flatFee = flatFee;
        card.currency = currency != null ? currency : "IDR";
        card.active = true;
        card.effectiveFrom = LocalDateTime.now();
        card.createdAt = LocalDateTime.now();
        card.updatedAt = LocalDateTime.now();
        card.createdBy = createdBy;
        return card;
    }

    /**
     * Create a new percentage fee rate card.
     */
    public static RateCard createPercentageFee(String name, String description,
                                                BigDecimal percentageFee, String currency,
                                                BigDecimal minimumFee, BigDecimal maximumFee,
                                                String createdBy) {
        RateCard card = new RateCard();
        card.id = UUID.randomUUID();
        card.name = name;
        card.description = description;
        card.feeType = FeeType.PERCENTAGE;
        card.percentageFee = percentageFee;
        card.currency = currency != null ? currency : "IDR";
        card.minimumFee = minimumFee;
        card.maximumFee = maximumFee;
        card.active = true;
        card.effectiveFrom = LocalDateTime.now();
        card.createdAt = LocalDateTime.now();
        card.updatedAt = LocalDateTime.now();
        card.createdBy = createdBy;
        return card;
    }

    /**
     * Create a new tiered fee rate card.
     */
    public static RateCard createTieredFee(String name, String description,
                                            String currency, String createdBy) {
        RateCard card = new RateCard();
        card.id = UUID.randomUUID();
        card.name = name;
        card.description = description;
        card.feeType = FeeType.TIERED;
        card.currency = currency != null ? currency : "IDR";
        card.tiers = new ArrayList<>();
        card.active = true;
        card.effectiveFrom = LocalDateTime.now();
        card.createdAt = LocalDateTime.now();
        card.updatedAt = LocalDateTime.now();
        card.createdBy = createdBy;
        return card;
    }

    /**
     * Add a fee tier for tiered pricing.
     */
    public void addTier(BigDecimal minAmount, BigDecimal maxAmount,
                        BigDecimal flatFee, BigDecimal percentageFee) {
        if (feeType != FeeType.TIERED) {
            throw new IllegalStateException("Can only add tiers to TIERED rate cards");
        }
        FeeTier tier = new FeeTier();
        tier.setId(UUID.randomUUID());
        tier.setRateCardId(this.id);
        tier.setMinAmount(minAmount);
        tier.setMaxAmount(maxAmount);
        tier.setFlatFee(flatFee);
        tier.setPercentageFee(percentageFee);
        tier.setCreatedAt(LocalDateTime.now());
        tiers.add(tier);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate fee for a given transaction amount.
     */
    public FeeCalculationResult calculateFee(BigDecimal amount) {
        BigDecimal fee = BigDecimal.ZERO;

        switch (feeType) {
            case FLAT:
                fee = flatFee != null ? flatFee : BigDecimal.ZERO;
                break;

            case PERCENTAGE:
                fee = amount.multiply(percentageFee)
                        .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_EVEN);
                break;

            case TIERED:
                fee = calculateTieredFee(amount);
                break;
        }

        // Apply min/max constraints
        if (minimumFee != null && fee.compareTo(minimumFee) < 0) {
            fee = minimumFee;
        }
        if (maximumFee != null && fee.compareTo(maximumFee) > 0) {
            fee = maximumFee;
        }

        return new FeeCalculationResult(fee, amount.add(fee), feeType);
    }

    private BigDecimal calculateTieredFee(BigDecimal amount) {
        for (FeeTier tier : tiers) {
            boolean aboveMin = tier.getMinAmount() == null ||
                    amount.compareTo(tier.getMinAmount()) >= 0;
            boolean belowMax = tier.getMaxAmount() == null ||
                    amount.compareTo(tier.getMaxAmount()) <= 0;

            if (aboveMin && belowMax) {
                BigDecimal fee = BigDecimal.ZERO;
                if (tier.getFlatFee() != null) {
                    fee = fee.add(tier.getFlatFee());
                }
                if (tier.getPercentageFee() != null) {
                    fee = fee.add(amount.multiply(tier.getPercentageFee())
                            .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_EVEN));
                }
                return fee;
            }
        }
        return BigDecimal.ZERO;
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

    public static RateCardBuilder builder() {
        return new RateCardBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FeeType getFeeType() { return feeType; }
    public void setFeeType(FeeType feeType) { this.feeType = feeType; }
    public BigDecimal getFlatFee() { return flatFee; }
    public void setFlatFee(BigDecimal flatFee) { this.flatFee = flatFee; }
    public BigDecimal getPercentageFee() { return percentageFee; }
    public void setPercentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; }
    public List<FeeTier> getTiers() { return tiers; }
    public void setTiers(List<FeeTier> tiers) { this.tiers = tiers; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getMinimumFee() { return minimumFee; }
    public void setMinimumFee(BigDecimal minimumFee) { this.minimumFee = minimumFee; }
    public BigDecimal getMaximumFee() { return maximumFee; }
    public void setMaximumFee(BigDecimal maximumFee) { this.maximumFee = maximumFee; }
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

    public static class RateCardBuilder {
        private UUID id;
        private String name;
        private String description;
        private FeeType feeType;
        private BigDecimal flatFee;
        private BigDecimal percentageFee;
        private List<FeeTier> tiers;
        private String currency;
        private BigDecimal minimumFee;
        private BigDecimal maximumFee;
        private boolean active;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveUntil;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String tenantId;

        RateCardBuilder() {}

        public RateCardBuilder id(UUID id) { this.id = id; return this; }
        public RateCardBuilder name(String name) { this.name = name; return this; }
        public RateCardBuilder description(String description) { this.description = description; return this; }
        public RateCardBuilder feeType(FeeType feeType) { this.feeType = feeType; return this; }
        public RateCardBuilder flatFee(BigDecimal flatFee) { this.flatFee = flatFee; return this; }
        public RateCardBuilder percentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; return this; }
        public RateCardBuilder tiers(List<FeeTier> tiers) { this.tiers = tiers; return this; }
        public RateCardBuilder currency(String currency) { this.currency = currency; return this; }
        public RateCardBuilder minimumFee(BigDecimal minimumFee) { this.minimumFee = minimumFee; return this; }
        public RateCardBuilder maximumFee(BigDecimal maximumFee) { this.maximumFee = maximumFee; return this; }
        public RateCardBuilder active(boolean active) { this.active = active; return this; }
        public RateCardBuilder effectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; return this; }
        public RateCardBuilder effectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; return this; }
        public RateCardBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public RateCardBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public RateCardBuilder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public RateCardBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }

        public RateCard build() {
            return new RateCard(id, name, description, feeType, flatFee, percentageFee, tiers,
                    currency, minimumFee, maximumFee, active, effectiveFrom, effectiveUntil,
                    createdAt, updatedAt, createdBy, tenantId);
        }
    }
}
