package id.payu.partner.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fee tier entity for tiered pricing within a RateCard.
 */
public class FeeTier {

    private UUID id;
    private UUID rateCardId;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal flatFee;
    private BigDecimal percentageFee;
    private LocalDateTime createdAt;

    public FeeTier() {
    }

    public FeeTier(UUID id, UUID rateCardId, BigDecimal minAmount, BigDecimal maxAmount,
                   BigDecimal flatFee, BigDecimal percentageFee, LocalDateTime createdAt) {
        this.id = id;
        this.rateCardId = rateCardId;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.flatFee = flatFee;
        this.percentageFee = percentageFee;
        this.createdAt = createdAt;
    }

    public static FeeTierBuilder builder() {
        return new FeeTierBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRateCardId() { return rateCardId; }
    public void setRateCardId(UUID rateCardId) { this.rateCardId = rateCardId; }
    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public BigDecimal getFlatFee() { return flatFee; }
    public void setFlatFee(BigDecimal flatFee) { this.flatFee = flatFee; }
    public BigDecimal getPercentageFee() { return percentageFee; }
    public void setPercentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class FeeTierBuilder {
        private UUID id;
        private UUID rateCardId;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private BigDecimal flatFee;
        private BigDecimal percentageFee;
        private LocalDateTime createdAt;

        FeeTierBuilder() {}

        public FeeTierBuilder id(UUID id) { this.id = id; return this; }
        public FeeTierBuilder rateCardId(UUID rateCardId) { this.rateCardId = rateCardId; return this; }
        public FeeTierBuilder minAmount(BigDecimal minAmount) { this.minAmount = minAmount; return this; }
        public FeeTierBuilder maxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; return this; }
        public FeeTierBuilder flatFee(BigDecimal flatFee) { this.flatFee = flatFee; return this; }
        public FeeTierBuilder percentageFee(BigDecimal percentageFee) { this.percentageFee = percentageFee; return this; }
        public FeeTierBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public FeeTier build() {
            return new FeeTier(id, rateCardId, minAmount, maxAmount, flatFee, percentageFee, createdAt);
        }
    }
}
