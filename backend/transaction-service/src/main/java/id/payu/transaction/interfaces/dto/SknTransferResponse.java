package id.payu.transaction.interfaces.dto;

import java.time.Instant;

public class SknTransferResponse {
    public SknTransferResponse() {
    }

    public SknTransferResponse(String referenceNumber, String status, String transactionId, String message, Instant processingTime, String settlementDate) {
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
        this.processingTime = processingTime;
        this.settlementDate = settlementDate;
    }

    public static SknTransferResponseBuilder builder() {
        return new SknTransferResponseBuilder();
    }

    public static class SknTransferResponseBuilder {
        private String referenceNumber;
        private String status;
        private String transactionId;
        private String message;
        private Instant processingTime;
        private String settlementDate;

        public SknTransferResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public SknTransferResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public SknTransferResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        public SknTransferResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        public SknTransferResponseBuilder processingTime(Instant processingTime) {
            this.processingTime = processingTime;
            return this;
        }
        public SknTransferResponseBuilder settlementDate(String settlementDate) {
            this.settlementDate = settlementDate;
            return this;
        }

        public SknTransferResponse build() {
            return new SknTransferResponse(referenceNumber, status, transactionId, message, processingTime, settlementDate);
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

    public String getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
    }


    private String referenceNumber;
    private String status;
    private String transactionId;
    private String message;
    private Instant processingTime;
    private String settlementDate;
}
