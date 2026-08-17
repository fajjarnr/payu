package id.payu.transaction.interfaces.dto;

import java.time.Instant;

public class QrisPaymentResponse {
    private String transactionId;
    private String status;
    private String message;
    private String merchantName;
    private Instant transactionTime;

    public QrisPaymentResponse() {
    }

    public QrisPaymentResponse(String transactionId, String status, String message, String merchantName, Instant transactionTime) {
        this.transactionId = transactionId;
        this.status = status;
        this.message = message;
        this.merchantName = merchantName;
        this.transactionTime = transactionTime;
    }

    public static QrisPaymentResponseBuilder builder() {
        return new QrisPaymentResponseBuilder();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public Instant getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Instant transactionTime) {
        this.transactionTime = transactionTime;
    }

    public static class QrisPaymentResponseBuilder {
        private String transactionId;
        private String status;
        private String message;
        private String merchantName;
        private Instant transactionTime;

        public QrisPaymentResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public QrisPaymentResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public QrisPaymentResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public QrisPaymentResponseBuilder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public QrisPaymentResponseBuilder transactionTime(Instant transactionTime) {
            this.transactionTime = transactionTime;
            return this;
        }

        public QrisPaymentResponse build() {
            return new QrisPaymentResponse(transactionId, status, message, merchantName, transactionTime);
        }
    }
}
