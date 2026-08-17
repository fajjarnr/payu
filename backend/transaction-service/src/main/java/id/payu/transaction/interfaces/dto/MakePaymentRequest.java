package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MakePaymentRequest {
    public MakePaymentRequest() {
    }

    public MakePaymentRequest(BigDecimal amount, String notes) {
        this.amount = amount;
        this.notes = notes;
    }

    public static MakePaymentRequestBuilder builder() {
        return new MakePaymentRequestBuilder();
    }

    public static class MakePaymentRequestBuilder {
        private BigDecimal amount;
        private String notes;

        public MakePaymentRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public MakePaymentRequestBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public MakePaymentRequest build() {
            return new MakePaymentRequest(amount, notes);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }


    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String notes;
}
