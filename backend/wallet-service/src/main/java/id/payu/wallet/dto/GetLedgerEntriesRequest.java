package id.payu.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
@AllArgsConstructor
public class GetLedgerEntriesRequest {
    
    @NotNull(message = "Account ID is required")
    private String accountId;
    
    private Integer page = 0;
    
    @Min(value = 0, message = "Page cannot be negative")
    private Integer size = 20;
}
