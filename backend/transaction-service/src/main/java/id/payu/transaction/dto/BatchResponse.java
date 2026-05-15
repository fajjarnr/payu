package id.payu.transaction.dto;

import id.payu.transaction.adapter.persistence.entity.BatchDisbursementEntity;
import id.payu.transaction.domain.model.BatchDisbursementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for batch disbursement operations.
 */
public class BatchResponse {
    public BatchResponse() {
    }

    public BatchResponse(UUID id, String idempotencyKey, UUID sourceAccountId, String name, String description, BatchDisbursementStatus status, BigDecimal totalAmount, String currency, int itemCount, int progressPercentage, Instant createdAt, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.itemCount = itemCount;
        this.progressPercentage = progressPercentage;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static BatchResponseBuilder builder() {
        return new BatchResponseBuilder();
    }

    public static class BatchResponseBuilder {
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

        public BatchResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public BatchResponseBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }
        public BatchResponseBuilder sourceAccountId(UUID sourceAccountId) {
            this.sourceAccountId = sourceAccountId;
            return this;
        }
        public BatchResponseBuilder name(String name) {
            this.name = name;
            return this;
        }
        public BatchResponseBuilder description(String description) {
            this.description = description;
            return this;
        }
        public BatchResponseBuilder status(BatchDisbursementStatus status) {
            this.status = status;
            return this;
        }
        public BatchResponseBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }
        public BatchResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public BatchResponseBuilder itemCount(int itemCount) {
            this.itemCount = itemCount;
            return this;
        }
        public BatchResponseBuilder progressPercentage(int progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }
        public BatchResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public BatchResponseBuilder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }
        public BatchResponseBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public BatchResponse build() {
            return new BatchResponse(id, idempotencyKey, sourceAccountId, name, description, status, totalAmount, currency, itemCount, progressPercentage, createdAt, startedAt, completedAt);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BatchDisbursementStatus getStatus() {
        return status;
    }

    public void setStatus(BatchDisbursementStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }



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
    public static BatchResponse fromEntity(BatchDisbursementEntity batch) {
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
