package id.payu.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for Virtual Account details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualAccountResponse {

    private UUID id;
    private String vaNumber;
    private String bankCode;
    private String bankName;
    private UUID partnerId;
    private String externalId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String customerName;
    private String status;
    private BigDecimal paidAmount;
    private Instant paidAt;
    private String paymentReference;
    private Instant expiresAt;
    private Instant createdAt;
}
