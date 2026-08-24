package id.payu.dispute.interfaces.dto;

import id.payu.dispute.domain.model.Chargeback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargebackResponse {
    private UUID id;
    private UUID transactionId;
    private UUID customerId;
    private UUID merchantId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String status;
    private String schemeCaseId;
    private Instant createdAt;

    public static ChargebackResponse from(Chargeback cb) {
        return ChargebackResponse.builder()
                .id(cb.getId())
                .transactionId(cb.getTransactionId())
                .customerId(cb.getCustomerId())
                .merchantId(cb.getMerchantId())
                .amount(cb.getAmount())
                .currency(cb.getCurrency())
                .reason(cb.getReason())
                .status(cb.getStatus().name())
                .schemeCaseId(cb.getSchemeCaseId())
                .createdAt(cb.getCreatedAt())
                .build();
    }
}
