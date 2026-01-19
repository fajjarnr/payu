package id.payu.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveBalanceResponse {
    private String reservationId;
    private String accountId;
    private String referenceId;
    private String status;
    
    // Helper method for backward compatibility
    public boolean isSuccess() {
        return "RESERVED".equals(status);
    }
}
