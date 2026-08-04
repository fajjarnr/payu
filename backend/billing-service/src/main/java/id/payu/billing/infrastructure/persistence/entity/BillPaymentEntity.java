package id.payu.billing.infrastructure.persistence.entity;

import id.payu.security.annotation.Sensitive;
import id.payu.security.multitenancy.TenantAware;
import id.payu.security.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import id.payu.security.annotation.SensitivityLevel;

/**
 * Bill Payment entity.
 */
@Entity
@Table(name = "bill_payments", indexes = {
    @Index(name = "idx_payment_account", columnList = "account_id"),
    @Index(name = "idx_payment_reference", columnList = "reference_number")
})
@TenantAware
@EntityListeners(TenantEntityListener.class)
public class BillPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Sensitive(value = SensitivityLevel.HIGH)
    @Column(name = "reference_number", nullable = false, unique = true, length = 100)
    private String referenceNumber;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "biller_type", nullable = false, length = 50)
    private String billerType;

    @Sensitive
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId; // PLN meter number, phone number, etc.

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "admin_fee", precision = 19, scale = 4)
    private BigDecimal adminFee;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "biller_transaction_id", length = 100)
    private String billerTransactionId;

    @Column(name = "wallet_reservation_id", length = 100)
    private String walletReservationId;

    @Column(name = "event_published", nullable = false)
    private boolean eventPublished;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Version
    private Long version;


    // Constructors
    public BillPaymentEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (referenceNumber == null) {
            referenceNumber = "BILL-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }
        if (adminFee == null) {
            adminFee = BigDecimal.ZERO;
        }
        if (totalAmount == null) {
            totalAmount = amount.add(adminFee);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getBillerType() {
        return billerType;
    }

    public void setBillerType(String billerType) {
        this.billerType = billerType;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAdminFee() {
        return adminFee;
    }

    public void setAdminFee(BigDecimal adminFee) {
        this.adminFee = adminFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getBillerTransactionId() {
        return billerTransactionId;
    }

    public void setBillerTransactionId(String billerTransactionId) {
        this.billerTransactionId = billerTransactionId;
    }

    public String getWalletReservationId() {
        return walletReservationId;
    }

    public void setWalletReservationId(String walletReservationId) {
        this.walletReservationId = walletReservationId;
    }

    public boolean isEventPublished() {
        return eventPublished;
    }

    public void setEventPublished(boolean eventPublished) {
        this.eventPublished = eventPublished;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    /**
     * Payment status enum.
     */
}
