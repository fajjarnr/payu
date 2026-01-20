package id.payu.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
@AllArgsConstructor
public class GetLedgerEntriesByTransactionRequest {
    
    @NotNull(message = "Transaction ID is required")
    private String transactionId;
}
