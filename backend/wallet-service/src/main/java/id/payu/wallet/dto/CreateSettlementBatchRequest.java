package id.payu.wallet.dto;

import java.time.LocalDate;

/**
 * Request DTO for creating a settlement batch.
 */
public class CreateSettlementBatchRequest {

    private String partnerId;
    private LocalDate settlementDate;
    private String currency;

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
