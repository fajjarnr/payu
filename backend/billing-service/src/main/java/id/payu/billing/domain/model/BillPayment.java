package id.payu.billing.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bill Payment entity.
 */
@Entity
@Table(name = "bill_payments", indexes = {
    @Index(name = "idx_payment_account", columnList = "account_id"),
    @Index(name = "idx_payment_reference", columnList = "reference_number")
})
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "reference_number", nullable = false, unique = true, length = 100)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "biller_type", nullable = false, length = 50)
    private BillerType billerType;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId; // PLN meter number, phone number, etc.

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "admin_fee", precision = 19, scale = 4)
    private BigDecimal adminFee;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "biller_transaction_id", length = 100)
    private String billerTransactionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Constructors
    public BillPayment() {
    }

    public BillPayment(String accountId, BillerType billerType, String customerId, BigDecimal amount) {
        this.accountId = accountId;
        this.billerType = billerType;
        this.customerId = customerId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (referenceNumber == null) {
            referenceNumber = "BILL" + System.currentTimeMillis() + (int) (Math.random() * 1000);
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

    public BillerType getBillerType() {
        return billerType;
    }

    public void setBillerType(BillerType billerType) {
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
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

    /**
     * Payment status enum.
     */
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
