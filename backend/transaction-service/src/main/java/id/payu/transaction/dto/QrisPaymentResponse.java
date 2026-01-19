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
public class QrisPaymentResponse {
    private String transactionId;
    private String status;
    private String message;
    private String merchantName;
    private Instant transactionTime;
}
