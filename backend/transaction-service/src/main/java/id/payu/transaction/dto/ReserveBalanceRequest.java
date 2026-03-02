package id.payu.transaction.dto;

import java.math.BigDecimal;

public class ReserveBalanceRequest {
    private BigDecimal amount;
    private String referenceId;

    public ReserveBalanceRequest() {
    }

    public ReserveBalanceRequest(BigDecimal amount, String referenceId) {
        this.amount = amount;
        this.referenceId = referenceId;
    }

    public static ReserveBalanceRequestBuilder builder() {
        return new ReserveBalanceRequestBuilder();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public static class ReserveBalanceRequestBuilder {
        private BigDecimal amount;
        private String referenceId;

        public ReserveBalanceRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public ReserveBalanceRequestBuilder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public ReserveBalanceRequest build() {
            return new ReserveBalanceRequest(amount, referenceId);
        }
    }
}
