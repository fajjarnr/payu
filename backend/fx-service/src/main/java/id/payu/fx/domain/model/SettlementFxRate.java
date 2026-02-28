package id.payu.fx.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FX rate specifically for settlement operations with rate locking (GAP-010).
 * Provides 15-minute rate locking window for settlement certainty.
 */
public class SettlementFxRate {

    private UUID id;
    private String partnerId;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private BigDecimal inverseRate;
    private LocalDateTime lockedAt;
    private LocalDateTime expiresAt;
    private boolean active;
    private String settlementBatchId;
    private LocalDateTime createdAt;

    public SettlementFxRate() {
    }

    public SettlementFxRate(UUID id, String partnerId, String fromCurrency, String toCurrency,
                            BigDecimal rate, BigDecimal inverseRate, LocalDateTime lockedAt,
                            LocalDateTime expiresAt, boolean active, String settlementBatchId,
                            LocalDateTime createdAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.inverseRate = inverseRate;
        this.lockedAt = lockedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.settlementBatchId = settlementBatchId;
        this.createdAt = createdAt;
    }

    /**
     * Lock an FX rate for settlement with 15-minute window.
     */
    public static SettlementFxRate lockRate(String partnerId, String fromCurrency, String toCurrency,
                                            BigDecimal rate, String settlementBatchId) {
        SettlementFxRate fxRate = new SettlementFxRate();
        fxRate.id = UUID.randomUUID();
        fxRate.partnerId = partnerId;
        fxRate.fromCurrency = fromCurrency;
        fxRate.toCurrency = toCurrency;
        fxRate.rate = rate;
        fxRate.inverseRate = BigDecimal.ONE.divide(rate, 8, BigDecimal.ROUND_HALF_UP);
        fxRate.lockedAt = LocalDateTime.now();
        fxRate.expiresAt = fxRate.lockedAt.plusMinutes(15);
        fxRate.active = true;
        fxRate.settlementBatchId = settlementBatchId;
        fxRate.createdAt = LocalDateTime.now();
        return fxRate;
    }

    /**
     * Check if the locked rate is still valid.
     */
    public boolean isValid() {
        return active && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Check if the rate lock has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Convert amount using the locked rate.
     */
    public BigDecimal convert(BigDecimal amount) {
        if (!isValid()) {
            throw new IllegalStateException("FX rate lock has expired");
        }
        return amount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Invalidate the rate lock.
     */
    public void invalidate() {
        this.active = false;
    }

    public static SettlementFxRateBuilder builder() {
        return new SettlementFxRateBuilder();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getInverseRate() { return inverseRate; }
    public void setInverseRate(BigDecimal inverseRate) { this.inverseRate = inverseRate; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSettlementBatchId() { return settlementBatchId; }
    public void setSettlementBatchId(String settlementBatchId) { this.settlementBatchId = settlementBatchId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class SettlementFxRateBuilder {
        private UUID id;
        private String partnerId;
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
        private BigDecimal inverseRate;
        private LocalDateTime lockedAt;
        private LocalDateTime expiresAt;
        private boolean active;
        private String settlementBatchId;
        private LocalDateTime createdAt;

        SettlementFxRateBuilder() {}

        public SettlementFxRateBuilder id(UUID id) { this.id = id; return this; }
        public SettlementFxRateBuilder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public SettlementFxRateBuilder fromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; return this; }
        public SettlementFxRateBuilder toCurrency(String toCurrency) { this.toCurrency = toCurrency; return this; }
        public SettlementFxRateBuilder rate(BigDecimal rate) { this.rate = rate; return this; }
        public SettlementFxRateBuilder inverseRate(BigDecimal inverseRate) { this.inverseRate = inverseRate; return this; }
        public SettlementFxRateBuilder lockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; return this; }
        public SettlementFxRateBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public SettlementFxRateBuilder active(boolean active) { this.active = active; return this; }
        public SettlementFxRateBuilder settlementBatchId(String settlementBatchId) { this.settlementBatchId = settlementBatchId; return this; }
        public SettlementFxRateBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public SettlementFxRate build() {
            return new SettlementFxRate(id, partnerId, fromCurrency, toCurrency, rate, inverseRate,
                    lockedAt, expiresAt, active, settlementBatchId, createdAt);
        }
    }
}
