package id.payu.transaction.dto;

import id.payu.transaction.domain.model.BatchDisbursement;
import id.payu.transaction.domain.model.BatchDisbursementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for batch disbursement operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {

    private UUID id;
    private String idempotencyKey;
    private UUID sourceAccountId;
    private String name;
    private String description;
    private BatchDisbursementStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private int itemCount;
    private int progressPercentage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    /**
     * Creates a response DTO from a domain entity.
     *
     * @param batch the domain entity
     * @return the response DTO
     */
    public static BatchResponse fromEntity(BatchDisbursement batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .idempotencyKey(batch.getIdempotencyKey())
                .sourceAccountId(batch.getSourceAccountId())
                .name(batch.getName())
                .description(batch.getDescription())
                .status(batch.getStatus())
                .totalAmount(batch.getTotalAmount() != null ? batch.getTotalAmount().getAmount() : BigDecimal.ZERO)
                .currency(batch.getTotalAmount() != null ? batch.getTotalAmount().getCurrency().getCurrencyCode() : "IDR")
                .itemCount(batch.getItemCount())
                .progressPercentage(batch.getProgressPercentage())
                .createdAt(batch.getCreatedAt())
                .startedAt(batch.getStartedAt())
                .completedAt(batch.getCompletedAt())
                .build();
    }
}
