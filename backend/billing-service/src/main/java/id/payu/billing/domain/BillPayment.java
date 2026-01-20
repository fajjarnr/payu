package id.payu.billing.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bill Payment entity using Panache.
 */
@Entity
@Table(name = "bill_payments", indexes = {
    @Index(name = "idx_payment_account", columnList = "accountId"),
    @Index(name = "idx_payment_reference", columnList = "referenceNumber")
})
public class BillPayment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "reference_number", nullable = false, unique = true)
    public String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "biller_type", nullable = false)
    public BillerType billerType;

    @Column(name = "customer_id", nullable = false)
    public String customerId; // PLN meter number, phone number, etc.

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal amount;

    @Column(name = "admin_fee", precision = 19, scale = 4)
    public BigDecimal adminFee;

    @Column(name = "total_amount", precision = 19, scale = 4)
    public BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PaymentStatus status;

    @Column(name = "failure_reason")
    public String failureReason;

    @Column(name = "biller_transaction_id")
    public String billerTransactionId;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (referenceNumber == null) {
            referenceNumber = "BILL" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
        if (adminFee == null) {
            adminFee = BigDecimal.ZERO;
        }
        totalAmount = amount.add(adminFee);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
