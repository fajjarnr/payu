package id.payu.wallet.adapter.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FxRateResponse {

    private UUID id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private BigDecimal inverseRate;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getInverseRate() {
        return inverseRate;
    }

    public void setInverseRate(BigDecimal inverseRate) {
        this.inverseRate = inverseRate;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }
}
