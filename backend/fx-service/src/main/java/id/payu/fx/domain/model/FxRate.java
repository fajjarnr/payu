package id.payu.fx.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FxRate {

    private UUID id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private BigDecimal inverseRate;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Long version;
    private LocalDateTime createdAt;

    public FxRate() {
    }

    public FxRate(UUID id, String fromCurrency, String toCurrency, BigDecimal rate, 
                   BigDecimal inverseRate, LocalDateTime validFrom, LocalDateTime validUntil, 
                   Long version, LocalDateTime createdAt) {
        this.id = id;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.inverseRate = inverseRate;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.version = version;
        this.createdAt = createdAt;
    }

    public boolean isValidAt(LocalDateTime timestamp) {
        return (timestamp.isEqual(validFrom) || timestamp.isAfter(validFrom)) && 
               (timestamp.isBefore(validUntil) || timestamp.isEqual(validUntil));
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validUntil);
    }

    public BigDecimal convert(BigDecimal amount) {
        return amount.multiply(rate);
    }

    public static FxRateBuilder builder() {
        return new FxRateBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getInverseRate() { return inverseRate; }
    public void setInverseRate(BigDecimal inverseRate) { this.inverseRate = inverseRate; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class FxRateBuilder {
        private UUID id;
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
        private BigDecimal inverseRate;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private Long version;
        private LocalDateTime createdAt;

        FxRateBuilder() {}

        public FxRateBuilder id(UUID id) { this.id = id; return this; }
        public FxRateBuilder fromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; return this; }
        public FxRateBuilder toCurrency(String toCurrency) { this.toCurrency = toCurrency; return this; }
        public FxRateBuilder rate(BigDecimal rate) { this.rate = rate; return this; }
        public FxRateBuilder inverseRate(BigDecimal inverseRate) { this.inverseRate = inverseRate; return this; }
        public FxRateBuilder validFrom(LocalDateTime validFrom) { this.validFrom = validFrom; return this; }
        public FxRateBuilder validUntil(LocalDateTime validUntil) { this.validUntil = validUntil; return this; }
        public FxRateBuilder version(Long version) { this.version = version; return this; }
        public FxRateBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public FxRate build() {
            return new FxRate(id, fromCurrency, toCurrency, rate, inverseRate, validFrom, validUntil, version, createdAt);
        }
    }
}
