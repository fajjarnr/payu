package id.payu.transaction.dto;

import id.payu.transaction.adapter.persistence.entity.DisbursementEntity;
import id.payu.transaction.domain.model.DisbursementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for disbursement operations.
 */
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

    public DisbursementResponse() {
    }

    public DisbursementResponse(UUID id, String idempotencyKey, UUID sourceAccountId, BigDecimal amount, String currency,
                                String bankCode, String accountNumber, String accountName, String description,
                                DisbursementStatus status, String bankReference, String failureReason,
                                Instant createdAt, Instant processedAt, Instant completedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccountId = sourceAccountId;
        this.amount = amount;
        this.currency = currency;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.description = description;
        this.status = status;
        this.bankReference = bankReference;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.completedAt = completedAt;
    }

    public static DisbursementResponseBuilder builder() {
        return new DisbursementResponseBuilder();
    }

    /**
     * Creates a response DTO from a domain entity.
     *
     * @param disbursement the domain entity
     * @return the response DTO
     */
    public static DisbursementResponse fromEntity(DisbursementEntity disbursement) {
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

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DisbursementStatus getStatus() {
        return status;
    }

    public void setStatus(DisbursementStatus status) {
        this.status = status;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public static class DisbursementResponseBuilder {
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

        public DisbursementResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public DisbursementResponseBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public DisbursementResponseBuilder sourceAccountId(UUID sourceAccountId) {
            this.sourceAccountId = sourceAccountId;
            return this;
        }

        public DisbursementResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public DisbursementResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public DisbursementResponseBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }

        public DisbursementResponseBuilder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public DisbursementResponseBuilder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public DisbursementResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DisbursementResponseBuilder status(DisbursementStatus status) {
            this.status = status;
            return this;
        }

        public DisbursementResponseBuilder bankReference(String bankReference) {
            this.bankReference = bankReference;
            return this;
        }

        public DisbursementResponseBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public DisbursementResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DisbursementResponseBuilder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public DisbursementResponseBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public DisbursementResponse build() {
            return new DisbursementResponse(id, idempotencyKey, sourceAccountId, amount, currency, bankCode,
                    accountNumber, accountName, description, status, bankReference, failureReason,
                    createdAt, processedAt, completedAt);
        }
    }
}
