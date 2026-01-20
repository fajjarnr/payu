package id.payu.wallet.dto;

import jakarta.validation.constraints.*;

public class GetLedgerEntriesByTransactionRequest {
    
    @NotNull(message = "Transaction ID is required")
    private String transactionId;

    public GetLedgerEntriesByTransactionRequest() {
    }

    public GetLedgerEntriesByTransactionRequest(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
}
