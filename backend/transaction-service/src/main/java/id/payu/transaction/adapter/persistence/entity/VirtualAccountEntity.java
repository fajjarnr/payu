package id.payu.transaction.adapter.persistence.entity;

import id.payu.transaction.domain.model.*;

import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a Virtual Account (VA) payment collection.
 * Banks: BCA, BNI, Mandiri, Permata VA numbers for payment collection.
 * Lifecycle: PENDING → PAID → EXPIRED
 */
@Entity
@Table(name = "virtual_accounts",
       indexes = {
           @Index(name = "idx_va_number", columnList = "va_number", unique = true),
           @Index(name = "idx_va_partner_id", columnList = "partner_id"),
           @Index(name = "idx_va_status", columnList = "status"),
           @Index(name = "idx_va_expires_at", columnList = "expires_at"),
           @Index(name = "idx_va_external_id", columnList = "partner_id, external_id")
       })
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class VirtualAccountEntity {
    public VirtualAccountEntity() {
    }

    public VirtualAccountEntity(UUID id, String vaNumber, String bankCode, String bankName, UUID partnerId, String externalId, BigDecimal amount, String currency, String description, String customerName, String customerEmail, String customerPhone, VaStatus status, String callbackUrl, BigDecimal paidAmount, Instant paidAt, String paymentReference, Instant expiresAt, Instant createdAt, Instant updatedAt, String idempotencyKey) {
        this.id = id;
        this.vaNumber = vaNumber;
        this.bankCode = bankCode;
        this.bankName = bankName;
        this.partnerId = partnerId;
        this.externalId = externalId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.status = status;
        this.callbackUrl = callbackUrl;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
        this.paymentReference = paymentReference;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.idempotencyKey = idempotencyKey;
    }

    public static VirtualAccountBuilder builder() {
        return new VirtualAccountBuilder();
    }

    public static class VirtualAccountBuilder {
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
        private String customerEmail;
        private String customerPhone;
        private VaStatus status;
        private String callbackUrl;
        private BigDecimal paidAmount;
        private Instant paidAt;
        private String paymentReference;
        private Instant expiresAt;
        private Instant createdAt;
        private Instant updatedAt;
        private String idempotencyKey;

        public VirtualAccountBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        public VirtualAccountBuilder vaNumber(String vaNumber) {
            this.vaNumber = vaNumber;
            return this;
        }
        public VirtualAccountBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }
        public VirtualAccountBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }
        public VirtualAccountBuilder partnerId(UUID partnerId) {
            this.partnerId = partnerId;
            return this;
        }
        public VirtualAccountBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }
        public VirtualAccountBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        public VirtualAccountBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }
        public VirtualAccountBuilder description(String description) {
            this.description = description;
            return this;
        }
        public VirtualAccountBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }
        public VirtualAccountBuilder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }
        public VirtualAccountBuilder customerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
            return this;
        }
        public VirtualAccountBuilder status(VaStatus status) {
            this.status = status;
            return this;
        }
        public VirtualAccountBuilder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }
        public VirtualAccountBuilder paidAmount(BigDecimal paidAmount) {
            this.paidAmount = paidAmount;
            return this;
        }
        public VirtualAccountBuilder paidAt(Instant paidAt) {
            this.paidAt = paidAt;
            return this;
        }
        public VirtualAccountBuilder paymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }
        public VirtualAccountBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        public VirtualAccountBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public VirtualAccountBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public VirtualAccountBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public VirtualAccountEntity build() {
            return new VirtualAccountEntity(id, vaNumber, bankCode, bankName, partnerId, externalId, amount, currency, description, customerName, customerEmail, customerPhone, status, callbackUrl, paidAmount, paidAt, paymentReference, expiresAt, createdAt, updatedAt, idempotencyKey);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVaNumber() {
        return vaNumber;
    }

    public void setVaNumber(String vaNumber) {
        this.vaNumber = vaNumber;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public UUID getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(UUID partnerId) {
        this.partnerId = partnerId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public VaStatus getStatus() {
        return status;
    }

    public void setStatus(VaStatus status) {
        this.status = status;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "va_number", nullable = false, unique = true, length = 30)
    private String vaNumber;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 500)
    private String description;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VaStatus status;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = VaStatus.PENDING;
        }
        if (this.currency == null) {
            this.currency = "IDR";
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Domain methods

    public boolean isPending() {
        return status == VaStatus.PENDING && expiresAt.isAfter(Instant.now());
    }

    public void markPaid(BigDecimal paidAmount, String paymentReference) {
        if (this.status != VaStatus.PENDING) {
            throw new IllegalStateException("VA is not pending: " + this.status);
        }
        this.status = VaStatus.PAID;
        this.paidAmount = paidAmount;
        this.paymentReference = paymentReference;
        this.paidAt = Instant.now();
    }

    public void markExpired() {
        if (this.status == VaStatus.PENDING) {
            this.status = VaStatus.EXPIRED;
        }
    }
}
