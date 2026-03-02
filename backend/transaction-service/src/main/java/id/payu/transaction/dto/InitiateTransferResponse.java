package id.payu.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class InitiateTransferResponse {
    public InitiateTransferResponse() {
    }

    public InitiateTransferResponse(UUID transactionId, String referenceNumber, String status, BigDecimal fee, String estimatedCompletionTime) {
        this.transactionId = transactionId;
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.fee = fee;
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    public static InitiateTransferResponseBuilder builder() {
        return new InitiateTransferResponseBuilder();
    }

    public static class InitiateTransferResponseBuilder {
        private UUID transactionId;
        private String referenceNumber;
        private String status;
        private BigDecimal fee;
        private String estimatedCompletionTime;

        public InitiateTransferResponseBuilder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        public InitiateTransferResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public InitiateTransferResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public InitiateTransferResponseBuilder fee(BigDecimal fee) {
            this.fee = fee;
            return this;
        }
        public InitiateTransferResponseBuilder estimatedCompletionTime(String estimatedCompletionTime) {
            this.estimatedCompletionTime = estimatedCompletionTime;
            return this;
        }

        public InitiateTransferResponse build() {
            return new InitiateTransferResponse(transactionId, referenceNumber, status, fee, estimatedCompletionTime);
        }
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public void setEstimatedCompletionTime(String estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }


    private UUID transactionId;
    private String referenceNumber;
    private String status;
    private BigDecimal fee;
    private String estimatedCompletionTime;
}
