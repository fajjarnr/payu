package id.payu.transaction.dto;

import java.time.Instant;

public class BifastTransferResponse {
    public BifastTransferResponse() {
    }

    public BifastTransferResponse(String referenceNumber, String status, String transactionId, String message, Instant processingTime) {
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
        this.processingTime = processingTime;
    }

    public static BifastTransferResponseBuilder builder() {
        return new BifastTransferResponseBuilder();
    }

    public static class BifastTransferResponseBuilder {
        private String referenceNumber;
        private String status;
        private String transactionId;
        private String message;
        private Instant processingTime;

        public BifastTransferResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public BifastTransferResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public BifastTransferResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        public BifastTransferResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        public BifastTransferResponseBuilder processingTime(Instant processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public BifastTransferResponse build() {
            return new BifastTransferResponse(referenceNumber, status, transactionId, message, processingTime);
        }
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(Instant processingTime) {
        this.processingTime = processingTime;
    }


    private String referenceNumber;
    private String status;
    private String transactionId;
    private String message;
    private Instant processingTime;
}
