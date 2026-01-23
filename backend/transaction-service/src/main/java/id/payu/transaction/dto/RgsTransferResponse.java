package id.payu.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RgsTransferResponse {
    private String referenceNumber;
    private String status;
    private String transactionId;
    private String message;
    private Instant processingTime;
    private Instant settlementTime;
}
