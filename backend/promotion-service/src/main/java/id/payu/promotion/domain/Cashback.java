package id.payu.promotion.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cashbacks", indexes = {
    @Index(name = "idx_cashback_account", columnList = "accountId"),
    @Index(name = "idx_cashback_transaction", columnList = "transactionId"),
    @Index(name = "idx_cashback_status", columnList = "status"),
    @Index(name = "idx_cashback_date", columnList = "createdAt DESC")
})
public class Cashback extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "account_id", nullable = false)
    public String accountId;

    @Column(name = "transaction_id", nullable = false)
    public String transactionId;

    @Column(name = "cashback_amount", nullable = false, precision = 19, scale = 4)
    public BigDecimal cashbackAmount;

    @Column(name = "transaction_amount", nullable = false, precision = 19, scale = 4)
    public BigDecimal transactionAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal percentage;

    @Column(name = "merchant_code")
    public String merchantCode;

    @Column(name = "category_code")
    public String categoryCode;

    @Column(name = "cashback_code")
    public String cashbackCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public Status status;

    @Column(name = "credited_at")
    public LocalDateTime creditedAt;

    @Column(name = "expiry_date")
    public LocalDateTime expiryDate;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (percentage == null) {
            percentage = BigDecimal.ZERO;
        }
    }

    public enum Status {
        PENDING,
        CREDITED,
        EXPIRED,
        VOIDED
    }
}
