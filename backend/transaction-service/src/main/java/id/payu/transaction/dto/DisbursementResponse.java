package id.payu.transaction.dto;

import id.payu.transaction.domain.model.Disbursement;
import id.payu.transaction.domain.model.DisbursementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for disbursement operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementResponse {

    private UUID id;
    private String idempotencyKey;
    private UUID sourceAccountId;
    private BigDecimal amount;
    private String currency;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String description;
    private DisbursementStatus status;
    private String bankReference;
    private String failureReason;
    private Instant createdAt;
    private Instant processedAt;
    private Instant completedAt;

    /**
     * Creates a response DTO from a domain entity.
     *
     * @param disbursement the domain entity
     * @return the response DTO
     */
    public static DisbursementResponse fromEntity(Disbursement disbursement) {
        return DisbursementResponse.builder()
                .id(disbursement.getId())
                .idempotencyKey(disbursement.getIdempotencyKey())
                .sourceAccountId(disbursement.getSourceAccountId())
                .amount(disbursement.getAmount() != null ? disbursement.getAmount().getAmount() : null)
                .currency(disbursement.getAmount() != null ? disbursement.getAmount().getCurrency().getCurrencyCode() : null)
                .bankCode(disbursement.getBankCode())
                .accountNumber(disbursement.getAccountNumber())
                .accountName(disbursement.getAccountName())
                .description(disbursement.getDescription())
                .status(disbursement.getStatus())
                .bankReference(disbursement.getBankReference())
                .failureReason(disbursement.getFailureReason())
                .createdAt(disbursement.getCreatedAt())
                .processedAt(disbursement.getProcessedAt())
                .completedAt(disbursement.getCompletedAt())
                .build();
    }
}
