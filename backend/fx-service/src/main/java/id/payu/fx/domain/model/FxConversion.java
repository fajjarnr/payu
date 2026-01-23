package id.payu.fx.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FxConversion {

    private UUID id;
    private String accountId;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal exchangeRate;
    private BigDecimal fee;
    private LocalDateTime conversionDate;
    private ConversionStatus status;

    public enum ConversionStatus {
        PENDING, COMPLETED, FAILED, REVERSED
    }

    public FxConversion() {
    }

    public FxConversion(UUID id, String accountId, String fromCurrency, String toCurrency, 
                       BigDecimal fromAmount, BigDecimal toAmount, BigDecimal exchangeRate, 
                       BigDecimal fee, LocalDateTime conversionDate, ConversionStatus status) {
        this.id = id;
        this.accountId = accountId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.exchangeRate = exchangeRate;
        this.fee = fee;
        this.conversionDate = conversionDate;
        this.status = status;
    }

    public void markCompleted() {
        this.status = ConversionStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ConversionStatus.FAILED;
    }

    public void markReversed() {
        this.status = ConversionStatus.REVERSED;
    }

    public BigDecimal getEffectiveAmount() {
        if (fee != null) {
            return toAmount.subtract(fee);
        }
        return toAmount;
    }

    public static FxConversionBuilder builder() {
        return new FxConversionBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }
    public BigDecimal getFromAmount() { return fromAmount; }
    public void setFromAmount(BigDecimal fromAmount) { this.fromAmount = fromAmount; }
    public BigDecimal getToAmount() { return toAmount; }
    public void setToAmount(BigDecimal toAmount) { this.toAmount = toAmount; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public LocalDateTime getConversionDate() { return conversionDate; }
    public void setConversionDate(LocalDateTime conversionDate) { this.conversionDate = conversionDate; }
    public ConversionStatus getStatus() { return status; }
    public void setStatus(ConversionStatus status) { this.status = status; }

    public static class FxConversionBuilder {
        private UUID id;
        private String accountId;
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal fromAmount;
        private BigDecimal toAmount;
        private BigDecimal exchangeRate;
        private BigDecimal fee;
        private LocalDateTime conversionDate;
        private ConversionStatus status;

        FxConversionBuilder() {}

        public FxConversionBuilder id(UUID id) { this.id = id; return this; }
        public FxConversionBuilder accountId(String accountId) { this.accountId = accountId; return this; }
        public FxConversionBuilder fromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; return this; }
        public FxConversionBuilder toCurrency(String toCurrency) { this.toCurrency = toCurrency; return this; }
        public FxConversionBuilder fromAmount(BigDecimal fromAmount) { this.fromAmount = fromAmount; return this; }
        public FxConversionBuilder toAmount(BigDecimal toAmount) { this.toAmount = toAmount; return this; }
        public FxConversionBuilder exchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; return this; }
        public FxConversionBuilder fee(BigDecimal fee) { this.fee = fee; return this; }
        public FxConversionBuilder conversionDate(LocalDateTime conversionDate) { this.conversionDate = conversionDate; return this; }
        public FxConversionBuilder status(ConversionStatus status) { this.status = status; return this; }

        public FxConversion build() {
            return new FxConversion(id, accountId, fromCurrency, toCurrency, fromAmount, toAmount, exchangeRate, fee, conversionDate, status);
        }
    }
}
