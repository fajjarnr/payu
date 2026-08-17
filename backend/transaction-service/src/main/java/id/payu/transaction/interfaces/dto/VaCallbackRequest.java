package id.payu.transaction.interfaces.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request payload for VA bank callback (payment confirmation).
 */
public class VaCallbackRequest {
    public VaCallbackRequest() {
    }

    public VaCallbackRequest(String vaNumber, BigDecimal amount, String paymentReference, String bankReferenceNumber) {
        this.vaNumber = vaNumber;
        this.amount = amount;
        this.paymentReference = paymentReference;
        this.bankReferenceNumber = bankReferenceNumber;
    }

    public static VaCallbackRequestBuilder builder() {
        return new VaCallbackRequestBuilder();
    }

    public static class VaCallbackRequestBuilder {
        private String vaNumber;
        private BigDecimal amount;
        private String paymentReference;
        private String bankReferenceNumber;

        public VaCallbackRequestBuilder vaNumber(String vaNumber) {
            this.vaNumber = vaNumber;
            return this;
        }
        public VaCallbackRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public VaCallbackRequestBuilder paymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }
        public VaCallbackRequestBuilder bankReferenceNumber(String bankReferenceNumber) {
            this.bankReferenceNumber = bankReferenceNumber;
            return this;
        }

        public VaCallbackRequest build() {
            return new VaCallbackRequest(vaNumber, amount, paymentReference, bankReferenceNumber);
        }
    }

    public String getVaNumber() {
        return vaNumber;
    }

    public void setVaNumber(String vaNumber) {
        this.vaNumber = vaNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getBankReferenceNumber() {
        return bankReferenceNumber;
    }

    public void setBankReferenceNumber(String bankReferenceNumber) {
        this.bankReferenceNumber = bankReferenceNumber;
    }



    @NotBlank(message = "VA number is required")
    private String vaNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    private String bankReferenceNumber;
}
