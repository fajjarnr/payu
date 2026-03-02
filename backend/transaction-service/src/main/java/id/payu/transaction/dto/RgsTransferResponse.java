package id.payu.transaction.dto;

import java.time.Instant;

public class RgsTransferResponse {
    public RgsTransferResponse() {
    }

    public RgsTransferResponse(String referenceNumber, String status, String transactionId, String message, Instant processingTime, Instant settlementTime) {
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
        this.processingTime = processingTime;
        this.settlementTime = settlementTime;
    }

    public static RgsTransferResponseBuilder builder() {
        return new RgsTransferResponseBuilder();
    }

    public static class RgsTransferResponseBuilder {
        private String referenceNumber;
        private String status;
        private String transactionId;
        private String message;
        private Instant processingTime;
        private Instant settlementTime;

        public RgsTransferResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public RgsTransferResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public RgsTransferResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        public RgsTransferResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        public RgsTransferResponseBuilder processingTime(Instant processingTime) {
            this.processingTime = processingTime;
            return this;
        }
        public RgsTransferResponseBuilder settlementTime(Instant settlementTime) {
            this.settlementTime = settlementTime;
            return this;
        }

        public RgsTransferResponse build() {
            return new RgsTransferResponse(referenceNumber, status, transactionId, message, processingTime, settlementTime);
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

    public Instant getSettlementTime() {
        return settlementTime;
    }

    public void setSettlementTime(Instant settlementTime) {
        this.settlementTime = settlementTime;
    }


    private String referenceNumber;
    private String status;
    private String transactionId;
    private String message;
    private Instant processingTime;
    private Instant settlementTime;
}
