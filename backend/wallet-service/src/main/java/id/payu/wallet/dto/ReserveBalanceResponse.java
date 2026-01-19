package id.payu.wallet.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveBalanceResponse {
    private String reservationId;
    private String accountId;
    private String referenceId;
    private String status;
}
