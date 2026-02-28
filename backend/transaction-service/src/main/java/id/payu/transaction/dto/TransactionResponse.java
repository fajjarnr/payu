package id.payu.transaction.dto;

import id.payu.transaction.domain.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Transaction API responses.
 * BUG-BE-135 FIX: Prevents exposing internal domain model fields
 * (audit timestamps, deprecated BigDecimal fields, idempotency key, metadata)
 * directly via the API.
 */
@Data
@Builder
@AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private String referenceNumber;
    private UUID senderAccountId;
    private UUID recipientAccountId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private String failureReason;
    private Instant createdAt;
    private Instant completedAt;
    private String memo;
    private java.util.List<String> tags;

    /**
     * Map domain Transaction to API response DTO.
     */
    public static TransactionResponse from(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceNumber(tx.getReferenceNumber())
                .senderAccountId(tx.getSenderAccountId())
                .recipientAccountId(tx.getRecipientAccountId())
                .type(tx.getType() != null ? tx.getType().name() : null)
                .amount(tx.getAmount() != null ? tx.getAmount().getAmount() : tx.getAmountValue())
                .currency(tx.getAmount() != null ? tx.getAmount().getCurrency().getCurrencyCode() : tx.getCurrencyCode())
                .description(tx.getDescription())
                .status(tx.getStatus() != null ? tx.getStatus().name() : null)
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .memo(tx.getMemo())
                .tags(parseTags(tx.getTags()))
                .build();
    }

    private static List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
