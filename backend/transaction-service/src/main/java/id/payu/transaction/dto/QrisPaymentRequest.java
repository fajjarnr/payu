package id.payu.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public class QrisPaymentRequest {
    @NotBlank(message = "QRIS code is required")
    private String qrisCode;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotBlank(message = "Customer reference is required")
    private String customerReference;

    public QrisPaymentRequest() {
    }

    public QrisPaymentRequest(String qrisCode, BigDecimal amount, String currency, String merchantName, String customerReference) {
        this.qrisCode = qrisCode;
        this.amount = amount;
        this.currency = currency;
        this.merchantName = merchantName;
        this.customerReference = customerReference;
    }

    public static QrisPaymentRequestBuilder builder() {
        return new QrisPaymentRequestBuilder();
    }

    public String getQrisCode() {
        return qrisCode;
    }

    public void setQrisCode(String qrisCode) {
        this.qrisCode = qrisCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public void setCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    public static class QrisPaymentRequestBuilder {
        private String qrisCode;
        private BigDecimal amount;
        private String currency;
        private String merchantName;
        private String customerReference;

        public QrisPaymentRequestBuilder qrisCode(String qrisCode) {
            this.qrisCode = qrisCode;
            return this;
        }

        public QrisPaymentRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public QrisPaymentRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public QrisPaymentRequestBuilder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public QrisPaymentRequestBuilder customerReference(String customerReference) {
            this.customerReference = customerReference;
            return this;
        }

        public QrisPaymentRequest build() {
            return new QrisPaymentRequest(qrisCode, amount, currency, merchantName, customerReference);
        }
    }
}
