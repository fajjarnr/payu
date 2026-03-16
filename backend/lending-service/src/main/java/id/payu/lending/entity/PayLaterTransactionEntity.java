package id.payu.lending.entity;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paylater_transactions")
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class PayLaterTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "paylater_account_id", nullable = false)
    private UUID paylaterAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private id.payu.lending.domain.model.PayLaterTransaction.TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private id.payu.lending.domain.model.PayLaterTransaction.TransactionStatus status;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PayLaterTransactionEntity() {}

    public PayLaterTransactionEntity(UUID id, String externalId, UUID paylaterAccountId, 
                                     id.payu.lending.domain.model.PayLaterTransaction.TransactionType type, 
                                     BigDecimal amount, String merchantName, String description, 
                                     id.payu.lending.domain.model.PayLaterTransaction.TransactionStatus status, 
                                     LocalDateTime transactionDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.externalId = externalId;
        this.paylaterAccountId = paylaterAccountId;
        this.type = type;
        this.amount = amount;
        this.merchantName = merchantName;
        this.description = description;
        this.status = status;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public UUID getPaylaterAccountId() { return paylaterAccountId; }
    public void setPaylaterAccountId(UUID paylaterAccountId) { this.paylaterAccountId = paylaterAccountId; }
    public id.payu.lending.domain.model.PayLaterTransaction.TransactionType getType() { return type; }
    public void setType(id.payu.lending.domain.model.PayLaterTransaction.TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public id.payu.lending.domain.model.PayLaterTransaction.TransactionStatus getStatus() { return status; }
    public void setStatus(id.payu.lending.domain.model.PayLaterTransaction.TransactionStatus status) { this.status = status; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String externalId;
        private UUID paylaterAccountId;
        private id.payu.lending.domain.model.PayLaterTransaction.TransactionType type;
        private BigDecimal amount;
        private String merchantName;
        private String description;
        private id.payu.lending.domain.model.PayLaterTransaction.TransactionStatus status;
        private LocalDateTime transactionDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder paylaterAccountId(UUID paylaterAccountId) { this.paylaterAccountId = paylaterAccountId; return this; }
        public Builder type(id.payu.lending.domain.model.PayLaterTransaction.TransactionType type) { this.type = type; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder merchantName(String merchantName) { this.merchantName = merchantName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(id.payu.lending.domain.model.PayLaterTransaction.TransactionStatus status) { this.status = status; return this; }
        public Builder transactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public PayLaterTransactionEntity build() {
            return new PayLaterTransactionEntity(id, externalId, paylaterAccountId, type, amount, merchantName, description, status, transactionDate, createdAt, updatedAt);
        }
    }
}
