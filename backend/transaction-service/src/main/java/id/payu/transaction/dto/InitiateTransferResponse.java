package id.payu.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class InitiateTransferResponse {
    private UUID transactionId;
    private String referenceNumber;
    private String status;
    private BigDecimal fee;
    private String estimatedCompletionTime;
}
