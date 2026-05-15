package id.payu.transaction.dto;

import id.payu.transaction.adapter.persistence.entity.TransactionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * DTO for TransactionEntity API responses.
 * BUG-BE-135 FIX: Prevents exposing internal domain model fields
 * (audit timestamps, deprecated BigDecimal fields, idempotency key, metadata)
 * directly via the API.
 */
public class TransactionResponse {
    public TransactionResponse() {
    }

    public TransactionResponse(UUID id, String referenceNumber, UUID senderAccountId, UUID recipientAccountId, String type, BigDecimal amount, String currency, String description, String status, String failureReason, Instant createdAt, Instant completedAt, String memo, java.util.List<String> tags) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.senderAccountId = senderAccountId;
        this.recipientAccountId = recipientAccountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.memo = memo;
        this.tags = tags;
    }

    public static TransactionResponseBuilder builder() {
        return new TransactionResponseBuilder();
    }

    public static class TransactionResponseBuilder {
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

        public TransactionResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public TransactionResponseBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        public TransactionResponseBuilder senderAccountId(UUID senderAccountId) {
            this.senderAccountId = senderAccountId;
            return this;
        }
        public TransactionResponseBuilder recipientAccountId(UUID recipientAccountId) {
            this.recipientAccountId = recipientAccountId;
            return this;
        }
        public TransactionResponseBuilder type(String type) {
            this.type = type;
            return this;
        }
        public TransactionResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public TransactionResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public TransactionResponseBuilder description(String description) {
            this.description = description;
            return this;
        }
        public TransactionResponseBuilder status(String status) {
            this.status = status;
            return this;
        }
        public TransactionResponseBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        public TransactionResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public TransactionResponseBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }
        public TransactionResponseBuilder memo(String memo) {
            this.memo = memo;
            return this;
        }

        public TransactionResponseBuilder tags(java.util.List<String> tags) {
            this.tags = tags;
            return this;
        }

        public TransactionResponse build() {
            return new TransactionResponse(id, referenceNumber, senderAccountId, recipientAccountId, type, amount, currency, description, status, failureReason, createdAt, completedAt, memo, tags);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public UUID getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(UUID senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public UUID getRecipientAccountId() {
        return recipientAccountId;
    }

    public void setRecipientAccountId(UUID recipientAccountId) {
        this.recipientAccountId = recipientAccountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }


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
     * Map domain TransactionEntity to API response DTO.
     */
    public static TransactionResponse from(TransactionEntity tx) {
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
